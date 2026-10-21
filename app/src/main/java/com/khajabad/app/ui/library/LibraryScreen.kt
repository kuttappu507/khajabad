package com.khajabad.app.ui.library

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.core.Book
import com.khajabad.app.data.PrefsState
import com.khajabad.app.ui.components.PdfPageImage
import com.khajabad.app.ui.components.Revealed
import com.khajabad.app.ui.components.RoundIconButton
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Type

@Composable
fun LibraryScreen(
    books: List<Book>,
    prefs: PrefsState,
    bookmarkCount: Int,
    onOpenBook: (Book) -> Unit,
    onBookmarks: () -> Unit,
    onImport: () -> Unit,
    onRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Khaja.colors
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    BasicText(text = "Library", style = Type.H1.copy(color = colors.ink))
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = "${books.size} പുസ്തകങ്ങൾ · ഓഫ്‌ലൈൻ",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 12.sp),
                    )
                }
                RoundIconButton(
                    KhIcons.Plus,
                    onImport,
                    size = 46,
                    background = colors.mint,
                    borderColor = Color.Transparent,
                    tint = colors.onMint,
                )
            }
        }
        item {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(top = 2.dp)
                    .background(colors.line),
            )
        }
        item {
            Revealed(index = 0) {
                SavedPagesCard(count = bookmarkCount, onClick = onBookmarks)
            }
        }
        itemsIndexed(books, key = { _, b -> b.id }) { index, book ->
            Revealed(index = index + 1, delayStep = 50) {
                BookRow(
                    book = book,
                    page = prefs.pageOf(book.id),
                    onClick = { onOpenBook(book) },
                    onRemove = { onRemove(book) },
                )
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.4.dp, colors.line, RoundedCornerShape(26.dp))
                    .clickable(onClick = onImport),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.mintSoft),
                    ) {
                        KhIcon(KhIcons.Plus, Modifier.size(16.dp), colors.mintDeep)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        BasicText(
                            text = "PDF ചേർക്കുക",
                            style = Type.MalLabel.copy(color = colors.ink, fontSize = 13.5.sp),
                        )
                        BasicText(
                            text = "ഫോണിൽ നിന്ന് ഒരു പുസ്തകം തിരഞ്ഞെടുക്കുക",
                            style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 11.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPagesCard(count: Int, onClick: () -> Unit) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.98f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(26.dp))
            .background(colors.paperAlt)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.mint),
        ) {
            KhIcon(KhIcons.BookmarkFull, Modifier.size(18.dp), colors.onMint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = "സേവ് ചെയ്ത പേജുകൾ",
                style = Type.MalLabel.copy(color = colors.ink, fontSize = 13.5.sp),
            )
            BasicText(
                text = if (count == 0) "ഇനിയും ഒന്നും സേവ് ചെയ്തിട്ടില്ല"
                else "$count പേജ് ബുക്ക്മാർക്ക് ചെയ്തിരിക്കുന്നു",
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 11.sp),
            )
        }
        KhIcon(KhIcons.ChevronRight, Modifier.size(16.dp), colors.inkSoft)
    }
}

@Composable
private fun BookRow(
    book: Book,
    page: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    val progress = ((page + 1).toFloat() / book.pages.coerceAtLeast(1)).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(28.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(28.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .size(width = 66.dp, height = 88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
        ) {
            if (book.pages > 0) {
                PdfPageImage(
                    book = book,
                    page = 0,
                    targetWidth = 360,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = book.title,
                style = Type.MalTitle.copy(color = colors.ink, fontSize = 14.5.sp, lineHeight = 21.sp),
                maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = book.latin,
                style = Type.Small.copy(color = colors.inkSoft, fontSize = 11.sp),
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.paperAlt),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.02f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.mintDeep),
                )
            }
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = "${book.pages} പേജ് · ${(progress * 100).toInt()}%",
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 10.5.sp),
            )
        }
        Spacer(Modifier.width(6.dp))
        if (book.isImported) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
            ) {
                KhIcon(KhIcons.Close, Modifier.size(13.dp), colors.inkSoft.copy(alpha = 0.7f))
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.mintSoft),
            ) {
                KhIcon(KhIcons.ChevronRight, Modifier.size(15.dp), colors.mintDeep)
            }
        }
    }
}
