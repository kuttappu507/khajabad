package com.khajabad.app.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.khajabad.app.core.Book
import com.khajabad.app.core.Catalog
import com.khajabad.app.core.PdfPages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "khajabad")

data class PrefsState(
    val progress: Map<String, Int> = emptyMap(),
    val recent: List<String> = emptyList(),
    val bookmarks: Set<String> = emptySet(),
    val counter: Int = 0,
    val counterTarget: Int = 33,
    val dark: Boolean = false,
) {
    fun pageOf(bookId: String): Int = progress[bookId] ?: 0
}

/**
 * Tiny persistence layer on top of Preferences DataStore.
 * Everything is stored as compact strings — no schema migrations needed.
 */
class KhajabadRepository(private val context: Context) {

    private object K {
        val progress = stringPreferencesKey("progress")
        val recent = stringPreferencesKey("recent")
        val bookmarks = stringSetPreferencesKey("bookmarks")
        val counter = intPreferencesKey("counter")
        val counterTarget = intPreferencesKey("counter_target")
        val dark = booleanPreferencesKey("dark")
        val imported = stringPreferencesKey("imported_books")
    }

    val prefs: Flow<PrefsState> = context.dataStore.data.map { p ->
        PrefsState(
            progress = parseProgress(p[K.progress].orEmpty()),
            recent = p[K.recent].orEmpty().split('|').filter { it.isNotBlank() },
            bookmarks = p[K.bookmarks].orEmpty(),
            counter = p[K.counter] ?: 0,
            counterTarget = p[K.counterTarget] ?: 33,
            dark = p[K.dark] ?: false,
        )
    }

    val importedBooks: Flow<List<Book>> = context.dataStore.data.map { p ->
        parseBooks(p[K.imported].orEmpty())
    }

    val allBooks: Flow<List<Book>> = importedBooks.map { Catalog.builtIn + it }

    suspend fun setPage(bookId: String, page: Int) {
        context.dataStore.edit { p ->
            val map = parseProgress(p[K.progress].orEmpty()).toMutableMap()
            map[bookId] = page
            p[K.progress] = map.entries.joinToString(",") { "${it.key}=${it.value}" }
        }
    }

    suspend fun markRecent(bookId: String) {
        context.dataStore.edit { p ->
            val list = p[K.recent].orEmpty().split('|').filter { it.isNotBlank() }.toMutableList()
            list.remove(bookId)
            list.add(0, bookId)
            p[K.recent] = list.take(8).joinToString("|")
        }
    }

    suspend fun toggleBookmark(bookId: String, page: Int) {
        context.dataStore.edit { p ->
            val set = p[K.bookmarks].orEmpty().toMutableSet()
            val key = "$bookId:$page"
            if (!set.add(key)) set.remove(key)
            p[K.bookmarks] = set
        }
    }

    suspend fun setCounter(value: Int) {
        context.dataStore.edit { it[K.counter] = value.coerceAtLeast(0) }
    }

    suspend fun setCounterTarget(value: Int) {
        context.dataStore.edit {
            it[K.counterTarget] = value
            it[K.counter] = 0
        }
    }

    suspend fun resetCounter() {
        context.dataStore.edit { it[K.counter] = 0 }
    }

    suspend fun setDark(dark: Boolean) {
        context.dataStore.edit { it[K.dark] = dark }
    }

    /** Copies a picked PDF into app storage and registers it. */
    suspend fun import(uri: Uri, displayName: String?): Result<Book> = runCatching {
        val id = "imp_" + UUID.randomUUID().toString().take(8)
        val dir = File(context.filesDir, "books").apply { mkdirs() }
        val target = File(dir, "$id.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read the selected file")

        val pages = PdfPages.pageCount(context, Book(
            id = id, title = displayName ?: "PDF", latin = "", subtitle = "",
            categoryId = "more", pages = 0, filePath = target.absolutePath,
        )).coerceAtLeast(1)

        val name = (displayName ?: "Imported PDF")
            .removeSuffix(".pdf").removeSuffix(".PDF")

        val book = Book(
            id = id,
            title = name,
            latin = "Imported",
            subtitle = "$pages പേജ് · ഇംപോർട്ട് ചെയ്തത്",
            categoryId = "more",
            pages = pages,
            filePath = target.absolutePath,
            addedAt = System.currentTimeMillis(),
        )

        context.dataStore.edit { p ->
            val list = parseBooks(p[K.imported].orEmpty()).toMutableList()
            list.add(0, book)
            p[K.imported] = writeBooks(list)
        }
        book
    }

    suspend fun remove(book: Book) {
        context.dataStore.edit { p ->
            val list = parseBooks(p[K.imported].orEmpty()).filterNot { it.id == book.id }
            p[K.imported] = writeBooks(list)
        }
        book.filePath?.let { runCatching { File(it).delete() } }
    }

    private fun parseProgress(raw: String): Map<String, Int> =
        raw.split(',').mapNotNull { part ->
            val bits = part.split('=')
            if (bits.size == 2) bits[0] to (bits[1].toIntOrNull() ?: 0) else null
        }.toMap()

    private fun parseBooks(raw: String): List<Book> = runCatching {
        if (raw.isBlank()) return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val path = o.optString("path")
            if (path.isBlank() || !File(path).exists()) return@mapNotNull null
            Book(
                id = o.optString("id"),
                title = o.optString("title"),
                latin = o.optString("latin"),
                subtitle = o.optString("subtitle"),
                categoryId = o.optString("category"),
                pages = o.optInt("pages", 1),
                filePath = path,
                addedAt = o.optLong("addedAt"),
            )
        }
    }.getOrDefault(emptyList())

    private fun writeBooks(books: List<Book>): String {
        val arr = JSONArray()
        books.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("latin", b.latin)
                put("subtitle", b.subtitle)
                put("category", b.categoryId)
                put("pages", b.pages)
                put("path", b.filePath)
                put("addedAt", b.addedAt)
            })
        }
        return arr.toString()
    }
}
