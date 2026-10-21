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
import java.util.concurrent.TimeUnit

/**
 * Client for the website's Notte function.
 *
 * `POST /functions/{id}/runs/start` starts a run; the answer may either carry
 * the data directly or a `run_id` to poll at `GET /runs/{run_id}`.
 * Both paths are handled here, with a hard deadline so the UI never hangs.
 */
class EventApi(
    private val baseUrl: String = BuildConfig.NOTTE_BASE_URL,
    private val functionId: String = BuildConfig.NOTTE_FUNCTION_ID,
    private val apiKey: String = BuildConfig.NOTTE_API_KEY,
) {

    class ApiError(message: String) : Exception(message)

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && functionId.isNotBlank()

    suspend fun fetchEvents(limit: Int = 50): Pair<List<Event>, Long> = withContext(Dispatchers.IO) {
        if (!isConfigured) throw ApiError("Events API key is not configured")

        val startedAt = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("function_id", functionId)
            put("variables", JSONObject().apply {
                put("query", "")
                put("highlighted_only", false)
                put("limit", limit)
            })
        }

        var body = post("$baseUrl/functions/$functionId/runs/start", payload.toString())
        var runId = extractRunId(body)

        // Async run → poll until it finishes (max ~40s, then fall back to cached data).
        if (runId != null && EventParser.parse(body).isEmpty()) {
            val deadline = startedAt + TimeUnit.SECONDS.toMillis(40)
            while (System.currentTimeMillis() < deadline) {
                delay(1200)
                body = get("$baseUrl/runs/$runId")
                if (EventParser.parse(body).isNotEmpty()) break
                if (isTerminal(body)) break
                if (runId != extractRunId(body)) extractRunId(body)?.let { runId = it }
            }
        }

        val events = EventParser.parse(body)
        if (events.isEmpty()) throw ApiError("No events in the response")

        val fetchedAt = System.currentTimeMillis()
        Log.i(TAG, "loaded ${events.size} events in ${fetchedAt - startedAt} ms (run=$runId)")
        events to fetchedAt
    }

    /* --------------------------------------------------------------- http */

    private fun headers(conn: HttpURLConnection) {
        conn.setRequestProperty("x-notte-api-key", apiKey)
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
    }

    private fun post(url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            headers(this)
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            read(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            headers(this)
        }
        return try {
            read(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun read(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.let { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
        }.orEmpty()
        if (code !in 200..299) {
            throw ApiError("HTTP $code${if (text.isBlank()) "" else ": ${text.take(180)}"}")
        }
        return text
    }

    /* -------------------------------------------------------------- shape */

    private fun extractRunId(body: String): String? {
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val direct = listOf("run_id", "runId", "id")
            .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } }
        if (direct != null) return direct
        return listOf("run", "data", "result", "output").firstNotNullOfOrNull { key ->
            (o.optJSONObject(key))?.let { nested ->
                listOf("run_id", "runId", "id")
                    .firstNotNullOfOrNull { k -> nested.optString(k).takeIf { it.isNotBlank() } }
            }
        }
    }

    private fun isTerminal(body: String): Boolean {
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return true
        val status = sequenceOf(
            o.optString("status"),
            o.optJSONObject("run")?.optString("status").orEmpty(),
            o.optJSONObject("data")?.optString("status").orEmpty(),
        ).firstOrNull { it.isNotBlank() }?.lowercase() ?: return true
        return status !in setOf("queued", "pending", "running", "in_progress", "started", "created")
    }

    private companion object {
        const val TAG = "KhajabadEvents"
    }
}
