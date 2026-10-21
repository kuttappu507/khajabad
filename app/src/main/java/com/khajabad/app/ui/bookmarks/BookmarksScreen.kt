package com.khajabad.app.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.ui.components.PdfPageImage
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Type

@Composable
fun BookmarksScreen(
    items: List<Pair<Book, Int>>,
    onBack: () -> Unit,
    onOpen: (Book, Int) -> Unit,
    onRemove: (Book, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    KhIcons.Back,
                    onBack,
                    size = 42,
                    background = colors.surface,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    BasicText(text = "Saved pages", style = Type.H2.copy(color = colors.text))
                    BasicText(
                        text = "${items.size} ബുക്ക്മാർക്കുകൾ",
                        style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 11.sp),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
        if (items.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                ) {
                    KhIcon(
                        KhIcons.Bookmark,
                        Modifier.size(30.dp),
                        colors.accentDeep.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.height(14.dp))
                    BasicText(
                        text = "സേവ് ചെയ്ത പേജുകൾ ഇല്ല",
                        style = Type.MalTitle.copy(color = colors.text, fontSize = 14.sp),
                    )
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = "വായിക്കുമ്പോൾ മുകളിലെ ബുക്ക്മാർക്ക് ഐക്കൺ ടാപ്പ് ചെയ്യുക",
                        style = Type.MalLabel.copy(color = colors.textSoft, fontSize = 11.sp),
                    )
                }
            }
        }
        items(items) { pair ->
            val (book, page) = pair
            Revealed(index = 0, delayStep = 30) {
                SavedRow(
                    book = book,
                    page = page,
                    onClick = { onOpen(book, page) },
                    onRemove = { onRemove(book, page) },
                )
            }
        }
    }
}

@Composable
private fun SavedRow(
    book: Book,
    page: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
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
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .size(width = 46.dp, height = 62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
        ) {
            PdfPageImage(book = book, page = page, targetWidth = 240, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = book.title,
                style = Type.MalLabel.copy(color = colors.text, fontSize = 13.sp),
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = "പേജ് ${page + 1} / ${book.pages}",
                style = Type.Small.copy(color = colors.accentDeep, fontSize = 11.sp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
        ) {
            KhIcon(KhIcons.BookmarkFull, Modifier.size(15.dp), colors.accentDeep)
        }
    }
}
