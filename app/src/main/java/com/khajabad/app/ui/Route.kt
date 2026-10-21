package com.khajabad.app.ui

sealed interface Route {
    data object Splash : Route
    data object Home : Route
    data object Library : Route
    data object Counter : Route
    data object Search : Route
    data object Bookmarks : Route
    data class Reader(val bookId: String, val page: Int = 0) : Route
}
