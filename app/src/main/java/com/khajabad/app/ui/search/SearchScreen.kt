package com.khajabad.app.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.core.Catalog
import com.khajabad.app.data.PrefsState
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type

@Composable
fun SearchScreen(
    books: List<Book>,
    prefs: PrefsState,
    onOpenBook: (Book, Int?) -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Give the field a frame to attach before asking for focus.
        kotlinx.coroutines.delay(220)
        runCatching { focus.requestFocus() }
    }

    val needle = query.trim()
    val matched = if (needle.isEmpty()) emptyList() else books.filter {
        it.title.contains(needle, true) || it.latin.contains(needle, true)
    }
    val numeric = needle.toIntOrNull()
    val bookmarkPairs = remember(prefs.bookmarks, books) {
        prefs.bookmarks.mapNotNull { entry ->
            val parts = entry.split(':')
            val book = books.firstOrNull { it.id == parts.getOrNull(0) } ?: return@mapNotNull null
            val page = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            book to page
        }.filter { needle.isEmpty() || it.first.title.contains(needle, true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BasicText(text = "Search", style = Type.H1.copy(color = colors.ink))
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = "പുസ്തകങ്ങൾ, പേജുകൾ, ബുക്ക്മാർക്കുകൾ",
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
            )
        }
        item {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" },
                focusRequester = focus,
                onSubmit = { keyboard?.hide() },
            )
        }
        if (needle.isEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = "SUGGESTIONS",
                    style = Type.Label.copy(color = colors.inkSoft, fontSize = 9.5.sp),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Catalog.categories.take(6)) { category ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(100))
                                .background(colors.card)
                                .border(1.dp, colors.line, RoundedCornerShape(100))
                                .clickable { query = category.label }
                                .padding(horizontal = 15.dp, vertical = 9.dp),
                        ) {
                            BasicText(
                                text = category.label,
                                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                BasicText(text = "ALL BOOKS", style = Type.Label.copy(color = colors.inkSoft, fontSize = 9.5.sp))
            }
            items(books.size) { index ->
                val book = books[index]
                Revealed(index = index, delayStep = 40) {
                    ResultRow(
                        title = book.title,
                        subtitle = "${book.pages} പേജ് · ${book.latin}",
                        icon = KhIcons.Book,
                        onClick = { onOpenBook(book, null) },
                    )
                }
            }
            if (bookmarkPairs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    BasicText(
                        text = "SAVED PAGES",
                        style = Type.Label.copy(color = colors.inkSoft, fontSize = 9.5.sp),
                    )
                }
                items(bookmarkPairs.size) { index ->
                    val (book, page) = bookmarkPairs[index]
                    ResultRow(
                        title = "${book.title} · പേജ് ${page + 1}",
                        subtitle = book.latin,
                        icon = KhIcons.BookmarkFull,
                        onClick = { onOpenBook(book, page) },
                    )
                }
            }
        } else {
            if (matched.isNotEmpty()) {
                item {
                    BasicText(
                        text = "${matched.size} പുസ്തകം",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 11.sp),
                    )
                }
                items(matched.size) { index ->
                    val book = matched[index]
                    Revealed(index = index, delayStep = 40) {
                        ResultRow(
                            title = book.title,
                            subtitle = "${book.pages} പേജ് · ${book.latin}",
                            icon = KhIcons.Book,
                            onClick = { onOpenBook(book, null) },
                        )
                    }
                }
            }
            if (numeric != null) {
                item {
                    BasicText(
                        text = "പേജിലേക്ക് പോകുക",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 11.sp),
                    )
                }
                items(books.size) { index ->
                    val book = books[index]
                    val page = (numeric - 1).coerceIn(0, book.pages - 1)
                    ResultRow(
                        title = "${book.title} · പേജ് ${page + 1}",
                        subtitle = "ഈ പേജിലേക്ക് തുറക്കുക",
                        icon = KhIcons.ChevronRight,
                        onClick = { onOpenBook(book, page) },
                    )
                }
            }
            if (matched.isEmpty() && numeric == null && bookmarkPairs.isEmpty()) {
                item {
                    EmptyState(query = needle, onOpenBookmarks = onOpenBookmarks)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        targetValue = if (focused || query.isNotEmpty()) colors.mint else colors.line,
        animationSpec = Motion.fluid(),
        label = "border",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.4.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
    ) {
        KhIcon(KhIcons.Search, Modifier.size(18.dp), colors.inkSoft)
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = Type.Title.copy(color = colors.ink, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.mintDeep),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            interactionSource = interaction,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        BasicText(
                            text = "തിരയുക…",
                            style = Type.MalLabel.copy(color = colors.inkSoft.copy(alpha = 0.65f), fontSize = 14.sp),
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            RoundIconButton(
                KhIcons.Close,
                onClear,
                size = 28,
                tint = colors.inkSoft,
                background = Color.Transparent,
                borderColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.mintSoft),
        ) {
            KhIcon(icon, Modifier.size(16.dp), colors.mintDeep)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = Type.MalLabel.copy(color = colors.ink, fontSize = 13.5.sp),
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = subtitle,
                style = Type.Small.copy(color = colors.inkSoft, fontSize = 10.5.sp),
                maxLines = 1,
            )
        }
        KhIcon(KhIcons.ChevronRight, Modifier.size(15.dp), colors.inkSoft)
    }
}

@Composable
private fun EmptyState(query: String, onOpenBookmarks: () -> Unit) {
    val colors = Khaja.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
    ) {
        KhIcon(KhIcons.Search, Modifier.size(34.dp), colors.mintDeep.copy(alpha = 0.6f))
        Spacer(Modifier.height(14.dp))
        BasicText(
            text = "“$query” കണ്ടെത്തിയില്ല",
            style = Type.MalTitle.copy(
                color = colors.ink,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = "മറ്റൊരു വാക്ക് ശ്രമിക്കുക അല്ലെങ്കിൽ സേവ് ചെയ്ത പേജുകൾ നോക്കുക",
            style = Type.MalLabel.copy(
                color = colors.inkSoft,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(100))
                .background(colors.mint)
                .clickable(onClick = onOpenBookmarks)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BasicText(
                text = "ബുക്ക്മാർക്കുകൾ",
                style = Type.MalLabel.copy(color = colors.onMint, fontSize = 12.sp),
            )
        }
    }
}
