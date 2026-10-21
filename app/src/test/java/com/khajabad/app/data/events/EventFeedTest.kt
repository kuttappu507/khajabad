package com.khajabad.app.data.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * End-to-end test of the events client against a real socket.
 *
 * The Notte response shape was never observable from the build environment, so
 * the client is written to survive several shapes. These tests pin that behaviour:
 * direct arrays, `output`-as-a-JSON-string, async runs that need polling, and a
 * server that rejects the Bearer header.
 *
 * The stub server is hand-rolled on [ServerSocket] because Android unit tests do
 * not get `com.sun.net.httpserver` on their compile classpath.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class EventFeedTest {

    @Volatile
    private var route: (Request) -> Reply = { Reply(404, "{}") }

    private val seen = mutableListOf<String>()
    private lateinit var server: ServerSocket
    private lateinit var worker: Thread
    private var port = 0

    private data class Request(val method: String, val path: String, val headers: Map<String, String>)
    private data class Reply(val code: Int, val body: String)

    @Before
    fun startServer() {
        server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        port = server.localPort
        worker = Thread {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                runCatching { serve(socket) }
            }
        }.apply {
            isDaemon = true
            name = "event-feed-test-server"
            start()
        }
    }

    @After
    fun stopServer() {
        runCatching { server.close() }
    }

    private fun serve(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            val method = parts.getOrElse(0) { "GET" }
            val path = parts.getOrElse(1) { "/" }.substringBefore('?')
            val headers = LinkedHashMap<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            if (length > 0) {
                val buffer = CharArray(length)
                var read = 0
                while (read < length) {
                    val n = reader.read(buffer, read, length - read)
                    if (n < 0) break
                    read += n
                }
            }
            synchronized(seen) { seen += "$method $path" }
            val reply = route(Request(method, path, headers))
            val bytes = reply.body.toByteArray(Charsets.UTF_8)
            val head = buildString {
                append("HTTP/1.1 ${reply.code} ${if (reply.code in 200..299) "OK" else "Error"}\r\n")
                append("Content-Type: application/json\r\n")
                append("Content-Length: ${bytes.size}\r\n")
                append("Connection: close\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)
            s.getOutputStream().apply {
                write(head)
                write(bytes)
                flush()
            }
        }
    }

    private fun api() = EventApi(
        baseUrl = "http://127.0.0.1:$port",
        functionId = "fn-test",
        apiKey = "sk-test",
    )

    private val eventsJson = """
        {"events":[
          {"title":"മൗലിദ് ദിനം","start":"2026-11-02T18:30:00+05:30","venue":"ജുമാ മസ്ജിദ്"},
          {"title":"സ്വലാത്ത് ജൽസ","date":"2026-11-14","place":"Town Hall"}
        ]}
    """.trimIndent()

    /* ----------------------------------------------------------------- parser */

    @Test
    fun parsesPlainEventArray() {
        val events = EventParser.parse(eventsJson)
        assertEquals(2, events.size)
        assertEquals("മൗലിദ് ദിനം", events[0].title)
        assertEquals("ജുമാ മസ്ജിദ്", events[0].location)
        assertTrue("start date should parse", events[0].start != null)
    }

    @Test
    fun parsesEventsEmbeddedInARunOutputString() {
        val output = "Here is what I found:\n```json\n$eventsJson\n```"
        val body = """{"run_id":"r1","status":"completed","output":${quote(output)}}"""
        val events = EventParser.parse(body)
        assertEquals(2, events.size)
        assertEquals("സ്വലാത്ത് ജൽസ", events[1].title)
    }

    /** Prose + a fence around the payload, i.e. what an LLM step actually returns. */
    @Test
    fun parsesFencedJsonSurroundedByProse() {
        val body = "Here are the upcoming events:\n```json\n$eventsJson\n```\nLet me know!"
        val events = EventParser.parse(body)
        assertEquals(2, events.size)
    }

    @Test
    fun ignoresNonEventJson() {
        assertEquals(0, EventParser.parse("""{"status":"queued","run_id":"r9"}""").size)
        assertEquals(0, EventParser.parse("upstream error").size)
    }

    /* ------------------------------------------------------------------- http */

    @Test
    fun fallsBackThroughEndpointsUntilEventsArrive() {
        route = { request ->
            when {
                request.path.endsWith("/runs/start") -> Reply(404, """{"error":"unknown endpoint"}""")
                request.path.endsWith("/runs") && request.method == "POST" ->
                    Reply(200, """{"run_id":"abc","status":"running"}""")
                request.path == "/runs/abc" ->
                    Reply(200, """{"status":"completed","output":${quote(eventsJson)}}""")
                else -> Reply(404, "{}")
            }
        }

        val api = api()
        val (events, fetchedAt) = runBlocking { api.fetchEvents() }
        assertEquals(2, events.size)
        assertTrue(fetchedAt > 0)
        assertTrue("should have polled the run:\n${api.lastTrace}", seen.contains("GET /runs/abc"))
    }

    @Test
    fun retriesWithoutBearerWhenRejected() {
        route = { request ->
            if (request.headers["authorization"] != null) Reply(401, """{"error":"bad bearer"}""")
            else Reply(200, eventsJson)
        }

        val (events, _) = runBlocking { api().fetchEvents() }
        assertEquals(2, events.size)
    }

    @Test
    fun reportsEveryAttemptWhenNothingWorks() {
        route = { Reply(500, """{"error":"boom"}""") }
        val api = api()
        val error = runCatching { runBlocking { api.fetchEvents() } }.exceptionOrNull()
        assertTrue("expected ApiError, got $error", error is EventApi.ApiError)
        val trace = (error as EventApi.ApiError).trace
        assertTrue("trace should list attempts:\n$trace", trace.contains("/functions/fn-test/runs/start"))
        assertTrue(trace.contains("HTTP 500"))
    }

    /** Minimal JSON string quoting for embedding a document inside a JSON field. */
    private fun quote(text: String): String =
        "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
