package app.blade.data

sealed class ScreenType {
    object Browser : ScreenType()
    object TabSwitcher : ScreenType()
    object History : ScreenType()
    object Bookmarks : ScreenType()
    object Settings : ScreenType()
}
