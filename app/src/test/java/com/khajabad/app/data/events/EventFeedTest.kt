package com.khajabad.app.data.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

/**
 * End-to-end test of the events client against a local HTTP server.
 *
 * The Notte response shape was never observable from the build environment, so the
 * client is written to survive several shapes. These tests pin that behaviour:
 * direct arrays, `output`-as-a-JSON-string, async runs that need polling, and a
 * server that rejects the Bearer header.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class EventFeedTest {

    private lateinit var server: HttpServer
    private var port = 0
    private val seen = mutableListOf<String>()

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            executor = null
            createContext("/") { exchange -> handle(exchange) }
            start()
        }
        port = server.address.port
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    /** Set per test. */
    private var route: (HttpExchange) -> Unit = { exchange -> respond(exchange, 404, "{}") }

    private fun handle(exchange: HttpExchange) {
        synchronized(seen) {
            seen += "${exchange.requestMethod} ${exchange.requestURI.path}"
        }
        route(exchange)
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
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
        val body = """
            {"run_id":"r1","status":"completed",
             "output":"Here is what I found:\n```json\n$eventsJson\n```"}
        """.trimIndent()
        val events = EventParser.parse(body)
        assertEquals(2, events.size)
        assertEquals("സ്വലാത്ത് ജൽസ", events[1].title)
    }

    @Test
    fun ignoresNonEventJson() {
        assertEquals(0, EventParser.parse("""{"status":"queued","run_id":"r9"}""").size)
        assertEquals(0, EventParser.parse("upstream error").size)
    }

    /* ------------------------------------------------------------------- http */

    @Test
    fun fallsBackThroughEndpointsUntilEventsArrive() {
        route = { exchange ->
            val path = exchange.requestURI.path
            when {
                path.endsWith("/runs/start") ->
                    respond(exchange, 404, """{"error":"unknown endpoint"}""")
                path.endsWith("/runs") && exchange.requestMethod == "POST" ->
                    respond(exchange, 200, """{"run_id":"abc","status":"running"}""")
                path == "/runs/abc" -> respond(
                    exchange,
                    200,
                    """{"status":"completed","output":${quote(eventsJson)}}""",
                )
                else -> respond(exchange, 404, "{}")
            }
        }

        val (events, fetchedAt) = runBlocking { api().fetchEvents() }
        assertEquals(2, events.size)
        assertTrue(fetchedAt > 0)
        assertTrue(
            "trace should mention the poll: ${api().lastTrace}",
            seen.any { it == "GET /runs/abc" },
        )
    }

    @Test
    fun retriesWithoutBearerWhenRejected() {
        route = { exchange ->
            val auth = exchange.requestHeaders.getFirst("Authorization")
            if (auth != null) {
                respond(exchange, 401, """{"error":"bad bearer"}""")
            } else {
                respond(exchange, 200, eventsJson)
            }
        }

        val (events, _) = runBlocking { api().fetchEvents() }
        assertEquals(2, events.size)
    }

    @Test
    fun reportsEveryAttemptWhenNothingWorks() {
        route = { exchange -> respond(exchange, 500, """{"error":"boom"}""") }
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
