package com.khajabad.app.data.events

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * One upcoming event, normalised from whatever the Notte function returns.
 *
 * The website function is free to change shape, so [EventParser] maps a wide set
 * of field aliases onto this model instead of binding one exact schema.
 */
data class Event(
    val id: String,
    val title: String,
    val start: Long?,          // epoch millis, UTC
    val end: Long? = null,
    val location: String? = null,
    val description: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
    val highlighted: Boolean = false,
    val raw: Map<String, String> = emptyMap(),
) {
    val hasDate: Boolean get() = start != null
}

sealed interface EventsState {

    val events: List<Event>

    data object Idle : EventsState {
        override val events: List<Event> get() = emptyList()
    }

    data object Loading : EventsState {
        override val events: List<Event> get() = emptyList()
    }

    data class Live(
        override val events: List<Event>,
        val fetchedAt: Long,
    ) : EventsState

    /** Cached payload shown while the network is unavailable. */
    data class Stale(
        override val events: List<Event>,
        val fetchedAt: Long,
        val error: String,
    ) : EventsState

    data class Failed(
        val error: String,
        /** Endpoint-by-endpoint log from the last attempt, shown in the UI. */
        val detail: String = "",
    ) : EventsState {
        override val events: List<Event> get() = emptyList()
    }

    /** First event that has not started yet (falls back to the first row). */
    val next: Event?
        get() = events.firstOrNull { it.start == null || it.start >= System.currentTimeMillis() }
            ?: events.firstOrNull()
}

/**
 * Tolerant reader for the function output.
 *
 * It walks the JSON tree (skipping the run envelope) looking for the first
 * non-trivial array of objects that "looks like" a list of events, then maps
 * every object with an alias table. Unknown extras are kept in [Event.raw].
 */
object EventParser {

    private val TITLE_KEYS = listOf(
        "title", "name", "event_name", "event", "heading", "label", "subject",
        "program", "programme", "caption", "text",
    )
    private val START_KEYS = listOf(
        "start", "start_date", "startdate", "start_time", "datetime", "date",
        "date_start", "event_date", "when", "begins", "begin", "from",
        "startDate", "Start", "Date",
    )
    private val END_KEYS = listOf("end", "end_date", "enddate", "end_time", "until", "to", "ends", "endDate")
    private val LOCATION_KEYS = listOf(
        "location", "venue", "place", "address", "where", "city", "hall",
        "masjid", "mosque",
    )
    private val DESC_KEYS = listOf(
        "description", "details", "summary", "body", "note", "notes", "info",
        "about", "content",
    )
    private val URL_KEYS = listOf("url", "link", "website", "registration_url", "href", "permalink")
    private val IMAGE_KEYS = listOf("image", "image_url", "imageUrl", "cover", "thumbnail", "photo", "poster")
    private val HIGHLIGHT_KEYS = listOf("highlighted", "featured", "is_highlighted", "pin", "pinned", "star")

    fun parse(body: String): List<Event> {
        val root = json(body) ?: return emptyList()
        val candidates = mutableListOf<List<Event>>()
        collect(root, candidates, depth = 0)
        return candidates.maxByOrNull { it.size }?.sortedWith(
            compareBy({ it.start ?: Long.MAX_VALUE }, { it.title.lowercase() })
        ) ?: emptyList()
    }

    private fun collect(node: Any?, sink: MutableList<List<Event>>, depth: Int) {
        if (depth > 8) return
        when (node) {
            is JSONObject -> {
                // A bare event object somewhere in the envelope.
                val single = toEvent(node)
                if (single != null && node.length() > 1) sink.add(listOf(single))
                node.keys().forEach { key ->
                    if (key in ENVELOPE_KEYS) return@forEach
                    collect(node.opt(key), sink, depth + 1)
                }
            }
            is JSONArray -> {
                val events = (0 until node.length()).mapNotNull { i ->
                    (node.opt(i) as? JSONObject)?.let { toEvent(it) }
                }
                if (events.size >= 1 && (node.length() == events.size || events.size >= 2)) {
                    sink.add(events)
                }
                for (i in 0 until node.length()) collect(node.opt(i), sink, depth + 1)
            }
            is String -> {
                // Notte functions often return their payload as a JSON (or
                // fenced ```json) *string* inside `output` / `result`.
                if (node.length in 8..400_000 && looksLikeJson(node)) {
                    json(node)?.let { collect(it, sink, depth + 1) }
                }
            }
        }
    }

    private fun looksLikeJson(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("{") || t.startsWith("[") ||
            (t.contains("```") && (t.contains("{") || t.contains("[")))
    }

    /** Parses JSON, tolerating fences, prose around the payload and whole-body strings. */
    private fun json(text: String): Any? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        runCatching { return JSONObject(trimmed) }
        runCatching { return JSONArray(trimmed) }

        val start = trimmed.indexOfFirst { it == '{' || it == '[' }
        if (start < 0) return null
        val slice = trimmed.substring(start).trimEnd()
        val end = slice.indexOfLast { it == '}' || it == ']' }
        if (end <= 0) return null
        val candidate = slice.substring(0, end + 1)
        runCatching { return JSONObject(candidate) }
        runCatching { return JSONArray(candidate) }
        return null
    }

    private fun toEvent(o: JSONObject): Event? {
        val raw = LinkedHashMap<String, String>()
        o.keys().forEach { key ->
            val value = o.opt(key)
            if (value is String || value is Number || value is Boolean) {
                raw[key] = value.toString()
            }
        }

        val title = firstString(o, TITLE_KEYS)
            ?: raw.entries.firstOrNull { it.value.length in 4..90 && it.value.any { c -> c.isLetter() } }?.value
            ?: return null

        val start = firstDate(o, START_KEYS)
        val end = firstDate(o, END_KEYS)

        // Without any date-like value and without a description it is noise.
        if (start == null && firstString(o, DESC_KEYS) == null && raw.size < 3) return null

        return Event(
            id = firstString(o, listOf("id", "uuid", "slug", "event_id"))
                ?: (title + (start ?: 0L)).hashCode().toString(),
            title = title.trim(),
            start = start,
            end = end,
            location = firstString(o, LOCATION_KEYS),
            description = firstString(o, DESC_KEYS),
            url = firstString(o, URL_KEYS)?.takeIf { it.startsWith("http") },
            imageUrl = firstString(o, IMAGE_KEYS)?.takeIf { it.startsWith("http") },
            highlighted = firstBool(o, HIGHLIGHT_KEYS),
            raw = raw,
        )
    }

    private const val ENVELOPE_KEYS_HINT = ""

    private val ENVELOPE_KEYS = setOf("variables", "input", "request", "params", "config", "function")

    private fun firstString(o: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (!o.has(key)) continue
            val v = o.opt(key)
            if (v is String && v.isNotBlank() && v != "null") return v
            if (v is Number || v is Boolean) return v.toString()
            if (v is JSONObject) {
                val nested = firstString(v, listOf("name", "title", "label", "text", "value", "raw", "formatted", "iso"))
                if (nested != null) return nested
            }
        }
        return null
    }

    private fun firstBool(o: JSONObject, keys: List<String>): Boolean {
        for (key in keys) {
            val v = o.opt(key) ?: continue
            if (v is Boolean) return v
            if (v is String) return v.equals("true", true) || v == "1"
        }
        return false
    }

    private fun firstDate(o: JSONObject, keys: List<String>): Long? {
        for (key in keys) {
            val v = o.opt(key) ?: continue
            when (v) {
                is Number -> return normaliseEpoch(v.toLong())
                is String -> Dates.parse(v)?.let { return it }
                is JSONObject -> {
                    val nested = firstString(v, listOf("iso", "datetime", "date", "value", "raw", "formatted", "start"))
                    if (nested != null) Dates.parse(nested)?.let { return it }
                }
            }
        }
        return null
    }

    private fun normaliseEpoch(value: Long): Long =
        if (value in 1_000_000_000L..9_999_999_999L) value * 1000L else value
}

/** Date parsing without java.time (works down to API 23). */
object Dates {

    private val ISO_INSTANT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    private val ISO_NO_MS = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private val ISO_OFFSET = "yyyy-MM-dd'T'HH:mmXXX"
    private val ISO_LOCAL = "yyyy-MM-dd'T'HH:mm:ss"
    private val ISO_LOCAL_SHORT = "yyyy-MM-dd'T'HH:mm"
    private val DATE_ONLY = "yyyy-MM-dd"
    private val SLASH = "yyyy/MM/dd"

    private val HUMAN = listOf(
        "dd MMM yyyy",
        "d MMM yyyy",
        "dd MMMM yyyy",
        "d MMMM yyyy",
        "MMM d, yyyy",
        "MMMM d, yyyy",
        "EEE, dd MMM yyyy",
        "EEE, MMM d, yyyy",
        "dd-MM-yyyy",
        "d-M-yyyy",
        "dd/MM/yyyy",
        "M/d/yyyy",
        "yyyyMMdd",
    )

    fun parse(input: String?): Long? {
        val text = input?.trim()?.takeIf { it.length >= 6 } ?: return null

        // Pure epoch seconds/millis
        text.toLongOrNull()?.let { n ->
            return if (n in 1_000_000_000L..9_999_999_999L) n * 1000L else if (n > 1_000_000_000_000L) n else null
        }

        val cleaned = text
            .replace("Z", "+00:00")
            .replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")

        val isoPatterns = listOf(
            ISO_INSTANT, ISO_NO_MS, ISO_OFFSET, ISO_LOCAL, ISO_LOCAL_SHORT, DATE_ONLY, SLASH,
        )
        for (pattern in isoPatterns) {
            parseWith(pattern, cleaned, utc = pattern.contains("X") || pattern.contains("'T'"))?.let { return it }
        }

        // Trim a leading weekday / trailing time for human formats.
        val simplified = text
            .replace(Regex("\\s+at\\s+.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+\\d{1,2}:\\d{2}.*$"), "")
            .trim()

        for (pattern in HUMAN) {
            parseWith(pattern, simplified, utc = false)?.let { return it }
            if (simplified != text) parseWith(pattern, text, utc = false)?.let { return it }
        }

        // Range like "24–26 Oct 2026" → take the first date.
        Regex("(\\d{1,2})\\s*[–\\-]\\s*(\\d{1,2})\\s+(\\p{L}+)\\s+(\\d{4})").find(text)?.let { m ->
            val (d1, _, month, year) = m.destructured
            return parseWith("d MMMM yyyy", "$d1 $month $year", utc = false)
        }

        return null
    }

    private fun parseWith(pattern: String, text: String, utc: Boolean): Long? = runCatching {
        val format = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = true
            timeZone = if (utc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        }
        format.parse(text)?.time
    }.getOrNull()

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun dayMonth(millis: Long): Pair<String, String> {
        val day = SimpleDateFormat("dd", Locale.US).format(Date(millis))
        val month = SimpleDateFormat("MMM", Locale.US).format(Date(millis)).uppercase(Locale.US)
        return day to month
    }

    fun readable(millis: Long, pattern: String = "EEE, d MMM yyyy · h:mm a"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

    fun timeOnly(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    fun monthKey(millis: Long): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))

    /** Malayalam "x minutes ago" used in copy. */
    fun relative(epochMillis: Long): String {
        val minutes = abs(epochMillis - System.currentTimeMillis()) / 60_000
        return when {
            minutes < 1 -> "ഇപ്പോൾ"
            minutes < 60 -> "$minutes മിനിറ്റ് മുൻപ്"
            minutes < 60 * 24 -> "${minutes / 60} മണിക്കൂർ മുൻപ്"
            else -> "${minutes / (60 * 24)} ദിവസം മുൻപ്"
        }
    }

    /** Compact English variant for status chips. */
    fun relativeShort(epochMillis: Long): String {
        val minutes = abs(epochMillis - System.currentTimeMillis()) / 60_000
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 60 * 24 -> "${minutes / 60} h ago"
            else -> "${minutes / (60 * 24)} d ago"
        }
    }

    private fun abs(v: Long) = if (v < 0) -v else v
}
