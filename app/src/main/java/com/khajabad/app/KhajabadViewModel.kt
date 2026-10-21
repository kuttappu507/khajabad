package com.khajabad.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khajabad.app.core.Book
import com.khajabad.app.data.KhajabadRepository
import com.khajabad.app.data.PrefsState
import com.khajabad.app.ui.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KhajabadViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = KhajabadRepository(app)

    val prefs: StateFlow<PrefsState> = repo.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, PrefsState())

    val books: StateFlow<List<Book>> = repo.allBooks
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.khajabad.app.core.Catalog.builtIn)

    /* --------------------------------------------------------------- nav */

    private val stack = mutableStateListOf<Route>(Route.Splash)
    val route: Route get() = stack.last()

    var drawerOpen by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    fun navigate(target: Route) {
        if (target == route) return
        if (target is Route.Splash) {
            stack.clear()
            stack.add(Route.Splash)
            return
        }
        if (stack.size > 1 && stack[stack.lastIndex - 1] == target) {
            stack.removeAt(stack.lastIndex)
            return
        }
        stack.add(target)
    }

    /** Replaces the whole stack (splash → home). */
    fun replace(target: Route) {
        stack.clear()
        stack.add(target)
    }

    /** Jumps between top-level tabs without piling up the back stack. */
    fun replaceOrNavigate(target: Route) {
        if (target == route) return
        val existing = stack.indexOfLast { it == target }
        if (existing >= 0) {
            while (stack.size > existing + 1) stack.removeAt(stack.lastIndex)
        } else {
            navigate(target)
        }
    }

    fun canGoBack(): Boolean = stack.size > 1

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun setDrawer(open: Boolean) {
        drawerOpen = open
    }

    fun toast(text: String) {
        message = text
        viewModelScope.launch {
            kotlinx.coroutines.delay(2400)
            if (message == text) message = null
        }
    }

    fun dismissMessage() { message = null }

    /* -------------------------------------------------------------- books */

    fun bookById(id: String): Book? = books.value.firstOrNull { it.id == id }

    fun openBook(id: String, page: Int? = null) {
        val book = bookById(id) ?: return
        val target = page?.coerceIn(0, (book.pages - 1).coerceAtLeast(0))
            ?: prefs.value.pageOf(id).coerceIn(0, (book.pages - 1).coerceAtLeast(0))
        viewModelScope.launch { repo.markRecent(id) }
        navigate(Route.Reader(id, target))
    }

    fun savePage(bookId: String, page: Int) {
        viewModelScope.launch { repo.setPage(bookId, page) }
    }

    fun toggleBookmark(bookId: String, page: Int) {
        viewModelScope.launch { repo.toggleBookmark(bookId, page) }
        toast(if (isBookmarked(bookId, page)) "ബുക്ക്മാർക്ക് നീക്കി" else "ബുക്ക്മാർക്ക് ചെയ്തു")
    }

    fun isBookmarked(bookId: String, page: Int) = prefs.value.bookmarks.contains("$bookId:$page")

    fun removeBook(book: Book) {
        viewModelScope.launch {
            repo.remove(book)
            toast("നീക്കം ചെയ്തു")
        }
    }

    fun importPdf(uri: Uri, name: String?) {
        viewModelScope.launch {
            repo.import(uri, name)
                .onSuccess {
                    toast("ഇംപോർട്ട് ചെയ്തു · ${it.title}")
                    navigate(Route.Reader(it.id, 0))
                }
                .onFailure { toast("ഇംപോർട്ട് പരാജയപ്പെട്ടു") }
        }
    }

    /* ------------------------------------------------------------ counter */

    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val _counterTarget = MutableStateFlow(33)
    val counterTarget: StateFlow<Int> = _counterTarget.asStateFlow()

    private var counterLoaded = false

    init {
        viewModelScope.launch {
            val snapshot = repo.prefs.first()
            if (!counterLoaded) {
                _counter.value = snapshot.counter
                _counterTarget.value = snapshot.counterTarget
                counterLoaded = true
            }
        }
    }

    fun tapCounter() {
        if (!counterLoaded) return
        _counter.value += 1
        viewModelScope.launch { repo.setCounter(_counter.value) }
    }

    fun undoCounter() {
        if (!counterLoaded || _counter.value == 0) return
        _counter.value -= 1
        viewModelScope.launch { repo.setCounter(_counter.value) }
    }

    fun resetCounter() {
        _counter.value = 0
        viewModelScope.launch { repo.resetCounter() }
    }

    fun setCounterTarget(target: Int) {
        _counterTarget.value = target
        _counter.value = 0
        viewModelScope.launch { repo.setCounterTarget(target) }
    }

    /* --------------------------------------------------------------- theme */

    private var darkLoaded = false

    fun toggleDark() {
        val next = !prefs.value.dark
        viewModelScope.launch { repo.setDark(next) }
        darkLoaded = true
    }

    fun setTheme(dark: Boolean) {
        viewModelScope.launch { repo.setDark(dark) }
    }

    /* --------------------------------------------------------------- misc */

    fun bookmarks(): List<Pair<Book, Int>> {
        val set = prefs.value.bookmarks
        if (set.isEmpty()) return emptyList()
        val byId = books.value.associateBy { it.id }
        return set.mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 2) return@mapNotNull null
            val book = byId[parts[0]] ?: return@mapNotNull null
            val page = parts[1].toIntOrNull() ?: return@mapNotNull null
            book to page
        }.sortedBy { it.first.id }
    }
}
