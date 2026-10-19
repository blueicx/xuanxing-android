package com.xuanji.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.xuanji.app.data.local.DataStorePreferenceBridge
import com.xuanji.app.data.local.ReadingPositionStore
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.ReadingFavoriteCodec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
internal fun rememberDetailReadingPosition(owner: String?, detail: MysticDetailContext?): ScrollState {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { ReadingPositionStore(DataStorePreferenceBridge(context)) }
    val reading = detail?.let { ReadingFavoriteCodec.stableId("reading-position", it) }
    val scroll = key(owner, reading) { rememberScrollState() }
    LaunchedEffect(owner, reading) {
        if (owner.isNullOrBlank() || reading == null) return@LaunchedEffect
        if (scroll.value == 0) runCatching { store.load(owner, reading) }.getOrNull()?.let { scroll.scrollTo(it) }
        snapshotFlow { scroll.value }.debounce(300).collect { position ->
            runCatching { store.save(owner, reading, position) }
        }
    }
    return scroll
}
