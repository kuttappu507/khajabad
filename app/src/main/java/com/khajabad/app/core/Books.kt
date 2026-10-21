package com.khajabad.app.core

import androidx.compose.ui.graphics.vector.ImageVector
import com.khajabad.app.ui.theme.KhIcons

/**
 * A book is either bundled in assets or imported by the user from storage.
 */
data class Book(
    val id: String,
    val title: String,
    val latin: String,
    val subtitle: String,
    val categoryId: String,
    val pages: Int,
    val assetPath: String? = null,
    val filePath: String? = null,
    val addedAt: Long = 0L,
) {
    val isImported: Boolean get() = filePath != null
}

data class Category(
    val id: String,
    val label: String,
    val latin: String,
    val icon: ImageVector,
    val bookId: String? = null,
)

object Catalog {

    val shajarath = Book(
        id = "shajarath",
        title = "ശജരത്തുൽ മുബാറക്ക",
        latin = "Shajarathul Mubarak",
        subtitle = "മൗലിദ് · ദുആ · ഖിസ്സ",
        categoryId = "mawlid",
        pages = 12,
        assetPath = "books/shajarath.pdf",
    )

    val swalathJalsah = Book(
        id = "swalath_jalsah",
        title = "സ്വലാത്ത് ജൽസ",
        latin = "Swalath Jalsah",
        subtitle = "മാസാനത്ത് സ്വലാത്ത് · 10 പേജ്",
        categoryId = "swalath",
        pages = 10,
        assetPath = "books/swalath_jalsah.pdf",
    )

    val builtIn: List<Book> = listOf(shajarath, swalathJalsah)

    val categories: List<Category> = listOf(
        Category("dhikr", "ദിക്ർ", "Dhikr", KhIcons.Beads),
        Category("dua", "ദുആ", "Dua", KhIcons.Hands),
        Category("swalath", "സ്വലാത്ത്", "Swalath", KhIcons.Dome, swalathJalsah.id),
        Category("mawlid", "മൗലിദ്", "Mawlid", KhIcons.Book, shajarath.id),
        Category("bait", "ബൈത്തുകൾ", "Bait", KhIcons.Bait),
        Category("malappattu", "മാലപ്പാട്ട്", "Malappattu", KhIcons.Scroll),
        Category("ratheeb", "രാത്തീബ്", "Ratheeb", KhIcons.Mihrab, shajarath.id),
        Category("majlis", "മജ്ലിസുന്നൂർ", "Majlisunnoor", KhIcons.Lantern),
        Category("more", "മറ്റുള്ളവ", "More", KhIcons.Dots),
    )

    fun find(id: String, extra: List<Book> = emptyList()): Book? =
        (builtIn + extra).firstOrNull { it.id == id }
}
