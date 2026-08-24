package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.xuanji.app.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class CardLayoutState(
    val order: Map<String, Int> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val collapsed: Set<String> = emptySet()
) {
    val hiddenCount: Int get() = hidden.size
}

data class CardMeta(
    val id: String,
    val title: String,
    val shareCard: ShareCard? = null,
    val content: @Composable () -> Unit
)

object CardLayouts {
    val COMPOSITE = listOf(
        "overall", "luck", "dim-peach", "dim-emotion", "dim-career",
        "dim-study", "dim-wealth", "dim-health", "caution"
    )
    val EASTERN = listOf(
        "hours", "pillars", "conclusion", "geju", "elements",
        "ten-gods", "ten-god-ratio", "strength", "yongji", "dayun",
        "relations", "shensha", "atlas", "fortune"
    )
    val WESTERN = listOf(
        "wheel", "natal", "axes", "planets", "planet-meaning",
        "aspects", "aspect-meaning", "conclusion", "fortune"
    )

    fun default(page: String): CardLayoutState {
        val ids = when (page) {
            "composite" -> COMPOSITE
            "eastern" -> EASTERN
            else -> WESTERN
        }
        return CardLayoutState(ids.withIndex().associate { (index, id) -> id to index + 1 })
    }

    fun normalize(page: String, savedOrder: Map<String, Int>?, savedHidden: Set<String>?): CardLayoutState {
        val defaults = default(page)
        val order = defaults.order.toMutableMap()
        savedOrder?.forEach { (id, value) ->
            if (order.containsKey(id)) order[id] = value
        }
        val validIds = order.keys
        return defaults.copy(
            order = order,
            hidden = savedHidden?.filterTo(mutableSetOf()) { it in validIds } ?: emptySet()
        )
    }

    fun move(state: CardLayoutState, page: String, id: String, delta: Int): CardLayoutState {
        val visible = default(page).order.keys
            .filterNot(state.hidden::contains)
            .sortedBy { state.order[it] ?: Int.MAX_VALUE }
        val from = visible.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return state
        val target = visible[to]
        return state.copy(
            order = state.order.toMutableMap().apply {
                this[id] = state.order[target] ?: to + 1
                this[target] = state.order[id] ?: from + 1
            }
        )
    }

    fun ordered(cards: List<CardMeta>, state: CardLayoutState): List<CardMeta> =
        cards.filterNot { it.id in state.hidden }
            .sortedBy { state.order[it.id] ?: Int.MAX_VALUE }
}

private data class StoredCardLayout(
    val order: Map<String, Int> = emptyMap(),
    val hidden: List<String> = emptyList(),
    val collapsed: List<String> = emptyList()
)

class CardLayoutStore(private val context: android.content.Context) {
    private val gson = Gson()

    fun flow(page: String, profileKey: String): Flow<CardLayoutState> {
        val key = stringPreferencesKey("card_layout_${page}_${fingerprint(profileKey)}")
        return context.dataStore.data.map { prefs ->
            prefs[key]?.let { json ->
                runCatching { gson.fromJson(json, StoredCardLayout::class.java) }.getOrNull()
                    ?.let { CardLayouts.normalize(page, it.order, it.hidden.toSet()) }
            } ?: CardLayouts.default(page)
        }
    }

    suspend fun save(page: String, profileKey: String, state: CardLayoutState) {
        val key = stringPreferencesKey("card_layout_${page}_${fingerprint(profileKey)}")
        val stored = StoredCardLayout(
            order = state.order,
            hidden = state.hidden.toList(),
            collapsed = state.collapsed.toList()
        )
        context.dataStore.edit { prefs -> prefs[key] = gson.toJson(stored) }
    }

    suspend fun reset(page: String, profileKey: String) {
        val key = stringPreferencesKey("card_layout_${page}_${fingerprint(profileKey)}")
        context.dataStore.edit { prefs -> prefs.remove(key) }
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

data class CardLayoutController(
    val page: String,
    val profileKey: String,
    val state: CardLayoutState,
    val editingCardId: String?,
    private val onEditingChange: (String?) -> Unit,
    private val store: CardLayoutStore,
    private val onStateChange: (CardLayoutState) -> Unit
) {
    fun move(id: String, delta: Int) = update(CardLayouts.move(state, page, id, delta))

    fun moveBy(id: String, delta: Int) {
        var next = state
        repeat(kotlin.math.abs(delta)) {
            next = CardLayouts.move(next, page, id, if (delta > 0) 1 else -1)
        }
        update(next)
    }

    fun hide(id: String) = update(state.copy(hidden = state.hidden + id))

    fun startEdit(id: String) {
        onEditingChange(id)
    }

    fun stopEdit() {
        onEditingChange(null)
    }

    fun reset() {
        onStateChange(CardLayouts.default(page))
    }

    private fun update(next: CardLayoutState) {
        onStateChange(next)
    }

    suspend fun persist() = store.save(page, profileKey, state)
}

@Composable
fun rememberCardLayoutController(page: String, profileKey: String): CardLayoutController {
    val context = LocalContext.current
    val store = remember(context) { CardLayoutStore(context) }
    var state by remember(page, profileKey) { mutableStateOf(CardLayouts.default(page)) }
    var editingCardId by remember(page, profileKey) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, profileKey) {
        store.flow(page, profileKey).collect { loaded -> state = loaded }
    }

    return remember(state, editingCardId) {
        CardLayoutController(
            page = page,
            profileKey = profileKey,
            state = state,
            editingCardId = editingCardId,
            onEditingChange = { editingCardId = it },
            store = store
        ) { next ->
            state = next
            scope.launch { runCatching { store.save(page, profileKey, next) } }
        }
    }
}

val LocalCardLayout = staticCompositionLocalOf<CardLayoutController?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardControls(
    title: String,
    cardId: String,
    controller: CardLayoutController,
    modifier: Modifier = Modifier,
    shareCard: ShareCard? = null
) {
    if (controller.editingCardId == cardId) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("拖动排序", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                if (shareCard != null) {
                    ShareButton(sharedCard = shareCard)
                }
                ControlTool("隐藏", "×") { controller.hide(cardId) }
            }
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "完成整理",
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClickLabel = "退出编辑", onClick = controller::stopEdit)
                        .padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Text(
            title,
            modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Modifier.cardDragReorder(
    enabled: Boolean,
    cardId: String,
    controller: CardLayoutController
): Modifier {
    if (!enabled) return this
    val currentController by rememberUpdatedState(controller)
    val density = LocalDensity.current
    val dragStep = with(density) { 104.dp.toPx() }
    var dragOffset by remember(cardId) { mutableStateOf(0f) }

    return this
        .graphicsLayer { translationY = dragOffset }
        .draggable(
            orientation = Orientation.Vertical,
            enabled = true,
            state = rememberDraggableState { delta ->
                dragOffset += delta
                val slots = (dragOffset / dragStep).toInt()
                if (slots != 0) {
                    currentController.moveBy(cardId, slots)
                    dragOffset -= slots * dragStep
                }
            },
            onDragStopped = { dragOffset = 0f }
        )
}

@Composable
private fun ControlTool(label: String, symbol: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            symbol,
            Modifier
                .width(30.dp)
                .clickable(onClickLabel = label, onClick = onClick)
                .padding(vertical = 5.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RestoreCardsBar(controller: CardLayoutController, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            Modifier
                .clickable(onClickLabel = "恢复默认卡片") { controller.reset() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("已隐藏 ${controller.state.hiddenCount} 张卡片")
            Spacer(Modifier.width(10.dp))
            Text("恢复默认", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
