package com.khajabad.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khajabad.app.KhajabadViewModel
import com.khajabad.app.core.Book
import com.khajabad.app.ui.bookmarks.BookmarksScreen
import com.khajabad.app.ui.components.BarDestination
import com.khajabad.app.ui.components.KhajaBottomBar
import com.khajabad.app.ui.components.ToastPill
import com.khajabad.app.ui.counter.CounterScreen
import com.khajabad.app.ui.events.EventsScreen
import com.khajabad.app.data.events.EventsState
import com.khajabad.app.ui.drawer.KhajaDrawer
import com.khajabad.app.ui.home.HomeScreen
import com.khajabad.app.ui.library.LibraryScreen
import com.khajabad.app.ui.reader.ReaderScreen
import com.khajabad.app.ui.search.SearchScreen
import com.khajabad.app.ui.splash.SplashScreen
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.KhajabadTheme

@Composable
fun KhajabadApp(vm: KhajabadViewModel = viewModel()) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val counter by vm.counter.collectAsStateWithLifecycle()
    val counterTarget by vm.counterTarget.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val route = vm.route
    val context = LocalContext.current
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !prefs.dark
            isAppearanceLightNavigationBars = !prefs.dark
        }
    }

    KhajabadTheme(dark = prefs.dark) {
        val colors = Khaja.colors
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) vm.importPdf(uri, displayName(context, uri))
        }

        val recent = remember(prefs.recent, books) {
            prefs.recent.mapNotNull { id -> books.firstOrNull { it.id == id } }
                .ifEmpty { books }
        }
        val bookmarkItems = remember(prefs.bookmarks, books) {
            prefs.bookmarks.mapNotNull { entry ->
                val parts = entry.split(':')
                val book = books.firstOrNull { it.id == parts.getOrNull(0) } ?: return@mapNotNull null
                val page = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                book to page
            }.sortedWith(compareBy({ it.first.id }, { it.second }))
        }

        BackHandler(enabled = vm.drawerOpen || (route !is Route.Splash && vm.canGoBack())) {
            if (vm.drawerOpen) vm.setDrawer(false) else vm.back()
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(colors.paper),
        ) {
            AnimatedContent(
                targetState = route,
                transitionSpec = { appTransition(initialState, targetState) },
                label = "route",
                modifier = Modifier.fillMaxSize(),
            ) { target ->
                when (target) {
                    Route.Splash -> SplashScreen(onFinished = { vm.replace(Route.Home) })

                    Route.Home -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        HomeScreen(
                            books = books,
                            recent = recent,
                            prefs = prefs,
                            onMenu = { vm.setDrawer(true) },
                            onBookmarks = { vm.navigate(Route.Bookmarks) },
                            onShare = { shareBook(context, recent.firstOrNull()) },
                            onOpenBook = { vm.openBook(it.id) },
                            onSeeAll = { vm.navigate(Route.Library) },
                            onCategory = { category ->
                                if (category.bookId != null) vm.openBook(category.bookId)
                                else vm.toast("“${category.label}” ഉടൻ വരുന്നു")
                            },
                            events = events,
                            onOpenEvents = { vm.navigate(Route.Events) },
                            onRefreshEvents = { vm.refreshEvents(force = true) },
                        )
                    }

                    Route.Library -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        LibraryScreen(
                            books = books,
                            prefs = prefs,
                            bookmarkCount = prefs.bookmarks.size,
                            onOpenBook = { vm.openBook(it.id) },
                            onBookmarks = { vm.navigate(Route.Bookmarks) },
                            onImport = { picker.launch(arrayOf("application/pdf")) },
                            onRemove = { vm.removeBook(it) },
                        )
                    }

                    Route.Counter -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        CounterScreen(
                            count = counter,
                            target = counterTarget,
                            onTap = vm::tapCounter,
                            onUndo = vm::undoCounter,
                            onReset = vm::resetCounter,
                            onTarget = vm::setCounterTarget,
                        )
                    }

                    Route.Events -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        EventsScreen(
                            state = events,
                            configured = vm.eventsConfigured,
                            onBack = { vm.back() },
                            onRefresh = { vm.refreshEvents(force = true) },
                            onShare = { text -> shareText(context, text) },
                        )
                    }

                    Route.Search -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        SearchScreen(
                            books = books,
                            prefs = prefs,
                            onOpenBook = { book, page -> vm.openBook(book.id, page) },
                            onOpenBookmarks = { vm.navigate(Route.Bookmarks) },
                        )
                    }

                    Route.Bookmarks -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        BookmarksScreen(
                            items = bookmarkItems,
                            onBack = { vm.back() },
                            onOpen = { book, page -> vm.openBook(book.id, page) },
                            onRemove = { book, page -> vm.toggleBookmark(book.id, page) },
                        )
                    }

                    is Route.Reader -> {
                        val book = books.firstOrNull { it.id == target.bookId }
                        if (book == null) {
                            SplashScreen(onFinished = { vm.replace(Route.Home) })
                        } else {
                            ReaderScreen(
                                book = book,
                                initialPage = target.page,
                                bookmarked = { page -> vm.isBookmarked(book.id, page) },
                                onBack = { vm.back() },
                                onPageChanged = { vm.savePage(book.id, it) },
                                onToggleBookmark = { vm.toggleBookmark(book.id, it) },
                            )
                        }
                    }
                }
            }

            val showBar = route is Route.Home || route is Route.Library || route is Route.Counter
            AnimatedVisibility(
                visible = showBar,
                enter = fadeIn(tween(260)) + slideInVertically(tween(340)) { it / 2 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
            ) {
                KhajaBottomBar(
                    destinations = destinations,
                    selectedIndex = when (route) {
                        Route.Home -> 0
                        Route.Library -> 1
                        Route.Counter -> 2
                        Route.Search -> 3
                        else -> 0
                    },
                    onSelect = { index ->
                        when (index) {
                            0 -> vm.replaceOrNavigate(Route.Home)
                            1 -> vm.replaceOrNavigate(Route.Library)
                            2 -> vm.replaceOrNavigate(Route.Counter)
                            3 -> vm.replaceOrNavigate(Route.Search)
                        }
                    },
                )
            }

            KhajaDrawer(
                open = vm.drawerOpen,
                current = route,
                dark = prefs.dark,
                bookCount = books.size,
                bookmarkCount = prefs.bookmarks.size,
                eventSubtitle = when (val state = events) {
                    is EventsState.Live -> state.next?.title?.take(28) ?: "${state.events.size} പരിപാടികൾ"
                    is EventsState.Stale -> "${state.events.size} പരിപാടികൾ · ഓഫ്‌ലൈൻ"
                    is EventsState.Loading -> "അപ്ഡേറ്റ് ചെയ്യുന്നു…"
                    is EventsState.Failed -> "ലഭ്യമല്ല · റിട്രൈ ചെയ്യുക"
                    EventsState.Idle -> "അടുത്ത പരിപാടികൾ"
                },
                onClose = { vm.setDrawer(false) },
                onNavigate = { target ->
                    vm.setDrawer(false)
                    if (target is Route.Home) vm.replaceOrNavigate(Route.Home) else vm.navigate(target)
                },
                onToggleDark = vm::toggleDark,
            )

            var lastMessage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(vm.message) {
                if (vm.message != null) lastMessage = vm.message
            }
            AnimatedVisibility(
                visible = vm.message != null,
                enter = fadeIn(tween(200)) + slideInVertically(tween(280)) { it },
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 108.dp),
            ) {
                ToastPill(lastMessage.orEmpty())
            }
        }
    }
}

private val destinations = listOf(
    BarDestination("home", "Home", KhIcons.Home),
    BarDestination("library", "Library", KhIcons.Grid),
    BarDestination("counter", "Counter", KhIcons.Beads),
    BarDestination("search", "Search", KhIcons.Search),
)

private fun androidx.compose.animation.AnimatedContentTransitionScope<Route>.appTransition(
    from: Route,
    to: Route,
) = when {
    from is Route.Splash ->
        (fadeIn(tween(560)) + scaleIn(tween(700), initialScale = 1.05f))
            .togetherWith(fadeOut(tween(420)) + scaleOut(tween(520), targetScale = 1.1f))

    to is Route.Reader ->
        (fadeIn(tween(280)) + slideInVertically(tween(420)) { it / 8 })
            .togetherWith(fadeOut(tween(200)) + scaleOut(tween(320), targetScale = 0.96f))

    from is Route.Reader ->
        fadeIn(tween(260))
            .togetherWith(fadeOut(tween(220)) + slideOutVertically(tween(320)) { it / 8 })

    else ->
        (fadeIn(tween(300)) + slideInHorizontally(tween(360)) { it / 16 })
            .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(280)) { -it / 22 })
}

private fun displayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return cursor.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) it.getString(index) else null
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private fun shareBook(context: Context, book: Book?) {
    val text = if (book != null) {
        "${book.title} (${book.latin}) — khajabad-ൽ വായിക്കൂ. ഓഫ്‌ലൈൻ PDF ലൈബ്രറി."
    } else {
        "khajabad — ഓഫ്‌ലൈൻ PDF ലൈബ്രറി"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
