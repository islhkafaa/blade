package app.blade.engine

import java.util.UUID

data class TabInfo(
    val id: String = UUID.randomUUID().toString(),
    val isPrivate: Boolean = false,
    val state: BrowserState = BrowserState()
)
