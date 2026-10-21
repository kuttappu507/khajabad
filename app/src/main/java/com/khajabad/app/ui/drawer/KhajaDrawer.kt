package com.khajabad.app.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khajabad.app.ui.Route
import com.khajabad.app.ui.components.rememberPressScale
import com.khajabad.app.ui.theme.KhIcons
import com.khajabad.app.ui.theme.KhIcon
import com.khajabad.app.ui.theme.Khaja
import com.khajabad.app.ui.theme.Motion
import com.khajabad.app.ui.theme.Type

@Composable
fun KhajaDrawer(
    open: Boolean,
    current: Route,
    dark: Boolean,
    bookCount: Int,
    bookmarkCount: Int,
    eventSubtitle: String,
    onClose: () -> Unit,
    onNavigate: (Route) -> Unit,
    onToggleDark: () -> Unit,
) {
    val colors = Khaja.colors
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = 0.42f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onClose()
                    },
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally(tween(340), initialOffsetX = { -it }),
            exit = slideOutHorizontally(tween(260), targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Column(
                Modifier
                    .width(312.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp))
                    .background(colors.paper)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.mint),
                    ) {
                        KhIcon(KhIcons.Book, Modifier.size(21.dp), colors.onMint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        BasicText(
                            text = "khajabad",
                            style = Type.Title.copy(color = colors.ink, fontSize = 18.sp),
                        )
                        BasicText(
                            text = "ഓഫ്‌ലൈൻ പുസ്തകശാല",
                            style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 10.5.sp),
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))
                BasicText(
                    text = "LIBRARY",
                    style = Type.Label.copy(color = colors.inkSoft, fontSize = 9.sp),
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                )

                DrawerItem(KhIcons.Home, "Home", "ഹോം", current is Route.Home) { onNavigate(Route.Home) }
                DrawerItem(KhIcons.Grid, "Library", "$bookCount പുസ്തകങ്ങൾ", current is Route.Library) {
                    onNavigate(Route.Library)
                }
                DrawerItem(KhIcons.Bookmark, "Saved pages", "$bookmarkCount ബുക്ക്മാർക്കുകൾ", current is Route.Bookmarks) {
                    onNavigate(Route.Bookmarks)
                }
                DrawerItem(KhIcons.Beads, "Counter", "ദിക്ർ കൗണ്ടർ", current is Route.Counter) {
                    onNavigate(Route.Counter)
                }
                DrawerItem(KhIcons.Search, "Search", "വേഗത്തിൽ കണ്ടെത്തുക", current is Route.Search) {
                    onNavigate(Route.Search)
                }
                DrawerItem(KhIcons.Star8, "Events", eventSubtitle, current is Route.Events) {
                    onNavigate(Route.Events)
                }

                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.line),
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onToggleDark)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    KhIcon(
                        if (dark) KhIcons.Moon else KhIcons.Sun,
                        Modifier.size(17.dp),
                        colors.inkSoft,
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicText(
                        text = "Night reading",
                        style = Type.Title.copy(color = colors.ink, fontSize = 13.sp),
                        modifier = Modifier.weight(1f),
                    )
                    KhajaSwitch(checked = dark, onChange = { onToggleDark() })
                }

                Spacer(Modifier.weight(1f))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.paperAlt)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KhIcon(KhIcons.Star8, Modifier.size(13.dp), colors.mintDeep)
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "khajabad · v1.0",
                            style = Type.Label.copy(color = colors.ink, fontSize = 9.sp),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    BasicText(
                        text = "ഇന്റർനെറ്റ് ഇല്ലാതെയും വായിക്കാം. നിങ്ങളുടെ പുസ്തകങ്ങൾ ഫോണിൽ തന്നെ സുരക്ഷിതം.",
                        style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 10.sp, lineHeight = 16.sp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = Khaja.colors
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.97f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.mintSoft else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) colors.mint else colors.paperAlt),
        ) {
            KhIcon(icon, Modifier.size(15.dp), if (selected) colors.onMint else colors.inkSoft)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = Type.Title.copy(color = colors.ink, fontSize = 13.sp),
            )
            BasicText(
                text = subtitle,
                style = Type.MalLabel.copy(color = colors.inkSoft, fontSize = 10.sp),
            )
        }
        if (selected) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.mintDeep),
            )
        }
    }
}

@Composable
fun KhajaSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Khaja.colors
    val offset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = Motion.snappy(),
        label = "switch",
    )
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) colors.mint else colors.ink.copy(alpha = 0.12f))
            .border(1.dp, colors.line, CircleShape)
            .clickable { onChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) colors.onMint else colors.card)
                .graphicsLayer { shadowElevation = 6f },
        )
    }
}
