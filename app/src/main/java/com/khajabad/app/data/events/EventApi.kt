package com.khajabad.app.data.events

import android.util.Log
import com.khajabad.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the website's Notte function.
 *
 * The exact REST shape of a Notte function is not documented and the authors'
 * build environment could not reach `us-prod.notte.cc`, so instead of betting on
 * a single endpoint this client walks a short ladder of plausible ones:
 *
 *   POST /functions/{id}/runs/start   {function_id, variables}
 *   POST /functions/{id}/runs/start   {variables}
 *   POST /functions/{id}/runs         {function_id, variables}
 *   POST /functions/{id}/runs         {variables}
 *   POST /functions/{id}/run          {function_id, variables}
 *   POST /functions/{id}/run          {variables}
 *   GET  /functions/{id}/runs?limit=N
 *   GET  /functions/{id}/events?limit=N
 *
 * If a call answers with a `run_id`, it polls `GET /runs/{id}` (and the
 * function-scoped variant) until events appear or the deadline passes.
 * Every step is recorded in [lastTrace], which the Events screen shows on
 * failure — so a wrong endpoint becomes a readable message instead of a spinner.
 *
 * Auth: `x-notte-api-key` is always sent; `Authorization: Bearer` is added but
 * dropped and retried if the server answers 401/403.
 */
class EventApi(
    private val baseUrl: String = BuildConfig.NOTTE_BASE_URL,
    private val functionId: String = BuildConfig.NOTTE_FUNCTION_ID,
    private val apiKey: String = BuildConfig.NOTTE_API_KEY,
) {

    class ApiError(message: String, val trace: String = "") : Exception(message)

    /** Human-readable log of the last [fetchEvents] attempt. */
    @Volatile
    var lastTrace: String = ""
        private set

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && functionId.isNotBlank()

    private val root: String get() = baseUrl.trimEnd('/')

    suspend fun fetchEvents(limit: Int = 50): Pair<List<Event>, Long> = withContext(Dispatchers.IO) {
        if (!isConfigured) throw ApiError("Events feed is not configured in this build")

        val startedAt = System.currentTimeMillis()
        val trace = mutableListOf<String>()
        val variables = JSONObject().apply {
            put("query", "")
            put("highlighted_only", false)
            put("limit", limit)
        }
        val bodies = listOf(
            "function_id+variables" to JSONObject().apply {
                put("function_id", functionId)
                put("variables", variables)
            }.toString(),
            "variables" to JSONObject().apply { put("variables", variables) }.toString(),
        )
        val endpoints = listOf(
            "/functions/$functionId/runs/start",
            "/functions/$functionId/runs",
            "/functions/$functionId/run",
        )

        var lastBody = ""

        for (endpoint in endpoints) {
            for ((label, body) in bodies) {
                val text = runCatching { post(endpoint, body) }
                    .onFailure { error ->
                        trace += "POST $endpoint [$label] → ${error.message?.take(160) ?: "failed"}"
                    }
                    .getOrNull()
                    ?: continue

                lastBody = text
                trace += "POST $endpoint [$label] → ${text.length} bytes"
                EventParser.parse(text).takeIf { it.isNotEmpty() }?.let { events ->
                    return@withContext done(events, startedAt, endpoint, trace)
                }

                val runId = extractRunId(text) ?: continue
                trace += "run $runId queued → polling"
                val payload = poll(runId, trace)
                if (payload != null) {
                    lastBody = payload
                    EventParser.parse(payload).takeIf { it.isNotEmpty() }?.let { events ->
                        return@withContext done(events, startedAt, "run $runId", trace)
                    }
                }
            }
        }

        for (path in listOf(
            "/functions/$functionId/runs?limit=$limit",
            "/functions/$functionId/events?limit=$limit",
        )) {
            val text = runCatching { get(path) }
                .onFailure { error ->
                    trace += "GET $path → ${error.message?.take(160) ?: "failed"}"
                }
                .getOrNull()
                ?: continue
            lastBody = text
            trace += "GET $path → ${text.length} bytes"
            EventParser.parse(text).takeIf { it.isNotEmpty() }?.let { events ->
                return@withContext done(events, startedAt, path, trace)
            }
        }

        lastTrace = trace.joinToString("\n")
        val snippet = lastBody.replace(Regex("\\s+"), " ").take(220)
        throw ApiError(
            message = "Events API answered, but no events could be read. " +
                if (snippet.isBlank()) "Every endpoint returned an empty body." else "Last body: $snippet",
            trace = lastTrace,
        )
    }

    private fun done(
        events: List<Event>,
        startedAt: Long,
        via: String,
        trace: MutableList<String>,
    ): Pair<List<Event>, Long> {
        lastTrace = trace.joinToString("\n")
        val fetchedAt = System.currentTimeMillis()
        Log.i(TAG, "loaded ${events.size} events via $via in ${fetchedAt - startedAt} ms")
        return events to fetchedAt
    }

    /* --------------------------------------------------------------- polling */

    private suspend fun poll(runId: String, trace: MutableList<String>): String? {
        val deadline = System.currentTimeMillis() + POLL_DEADLINE_MS
        val paths = listOf("/runs/$runId", "/functions/$functionId/runs/$runId")
        while (System.currentTimeMillis() < deadline) {
            delay(1500)
            for (path in paths) {
                val body = runCatching { get(path) }
                    .onFailure { error -> trace += "GET $path → ${error.message?.take(120) ?: "failed"}" }
                    .getOrNull()
                    ?: continue
                if (EventParser.parse(body).isNotEmpty()) return body
                if (isTerminal(body)) {
                    trace += "GET $path → finished, no events"
                    return body
                }
                trace += "GET $path → still working"
            }
        }
        trace += "polling timed out after ${POLL_DEADLINE_MS / 1000}s"
        return null
    }

    /* ------------------------------------------------------------------ http */

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        bearer: Boolean = true,
    ): String {
        val conn = (URL(root + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doOutput = body != null
            setRequestProperty("x-notte-api-key", apiKey)
            if (bearer) setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            // Some gateways reject the default Dalvik agent with a 403 before the
            // request ever reaches the function.
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = 10_000
            readTimeout = 20_000
        }
        return try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            read(conn, method, path, bearer)
        } finally {
            conn.disconnect()
        }
    }

    private fun read(conn: HttpURLConnection, method: String, path: String, bearer: Boolean): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.let { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
        }.orEmpty()
        if (code !in 200..299) {
            throw ApiError("HTTP $code${if (bearer) "" else " (no bearer)"}${if (text.isBlank()) "" else ": ${text.take(140)}"}")
        }
        return text
    }

    /** POST, falling back to api-key-only auth when the server rejects the bearer token. */
    private fun post(path: String, body: String): String {
        val first = runCatching { request("POST", path, body, bearer = true) }
        val error = first.exceptionOrNull()
        if (error == null) return first.getOrThrow()
        val message = error.message.orEmpty()
        if (message.startsWith("HTTP 401") || message.startsWith("HTTP 403")) {
            return request("POST", path, body, bearer = false)
        }
        throw error
    }

    private fun get(path: String): String {
        val first = runCatching { request("GET", path, null, bearer = true) }
        val error = first.exceptionOrNull()
        if (error == null) return first.getOrThrow()
        val message = error.message.orEmpty()
        if (message.startsWith("HTTP 401") || message.startsWith("HTTP 403")) {
            return request("GET", path, null, bearer = false)
        }
        throw error
    }

    /* ----------------------------------------------------------------- shape */

    private fun extractRunId(body: String): String? {
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
        listOf("run_id", "runId")
            .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } }
            ?.let { return it }
        listOf("run", "data", "result", "output", "job").forEach { key ->
            val nested = o.optJSONObject(key) ?: return@forEach
            listOf("run_id", "runId", "id")
                .firstNotNullOfOrNull { k -> nested.optString(k).takeIf { it.isNotBlank() } }
                ?.let { return it }
        }
        return null
    }

    private fun isTerminal(body: String): Boolean {
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return true
        val status = sequenceOf(
            o.optString("status"),
            o.optJSONObject("run")?.optString("status").orEmpty(),
            o.optJSONObject("data")?.optString("status").orEmpty(),
        ).firstOrNull { it.isNotBlank() }?.lowercase() ?: return false
        return status !in setOf("queued", "pending", "running", "in_progress", "started", "created", "processing")
    }

    private companion object {
        const val TAG = "KhajabadEvents"
        const val POLL_DEADLINE_MS = 30_000L
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) Khajabad/1.2"
    }
}
