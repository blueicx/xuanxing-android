package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
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

class CardLayoutController(
    val page: String,
    val profileKey: String,
    private val store: CardLayoutStore,
    initialState: CardLayoutState,
    private val persistState: (CardLayoutState) -> Unit
) {
    var state by mutableStateOf(initialState)
        private set
    var editingCardId by mutableStateOf<String?>(null)
        private set

    private var dragSession: CardDragSession? = null
    var draggingCardId by mutableStateOf<String?>(null)
        private set
    private val cardMetrics = mutableMapOf<String, CardMetric>()
    private val previewOffsets = mutableStateMapOf<String, Float>()

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
        editingCardId = id
    }

    fun stopEdit() {
        editingCardId = null
    }

    fun reset() {
        update(CardLayouts.default(page))
    }

    private fun update(next: CardLayoutState) {
        state = next
        persistState(next)
    }

    fun updateFromStore(loaded: CardLayoutState) {
        if (draggingCardId == null) {
            state = loaded
        }
    }

    suspend fun persist() = store.save(page, profileKey, state)

    fun recordMetric(id: String, top: Float, height: Float) {
        cardMetrics[id] = CardMetric(top, height)
    }

    fun beginDrag(id: String): Boolean {
        val metric = cardMetrics[id] ?: return false
        dragSession = CardDragSession(
            cardId = id,
            originalOrder = visibleIds(),
            startCenterY = metric.top + metric.height / 2f,
            draggedHeight = metric.height,
            gap = visibleGap()
        )
        draggingCardId = id
        previewOffsets.clear()
        return true
    }

    fun previewDragTo(dragOffset: Float) {
        val session = dragSession ?: return
        val draggedCenter = session.startCenterY + dragOffset
        val insertionIndex = session.originalOrder.count { other ->
            other != session.cardId && isCenterAbove(other, draggedCenter)
        }
        if (insertionIndex == session.lastInsertionIndex) return
        session.lastInsertionIndex = insertionIndex
        updatePreviewOffsets(session, insertionIndex)
    }

    fun endDrag() {
        commitDrag()
    }

    fun commitDrag() {
        val session = dragSession ?: return
        val next = state.copy(order = reorderedMap(session.cardId, session.lastInsertionIndex))
        dragSession = null
        draggingCardId = null
        editingCardId = null
        previewOffsets.clear()
        state = next
        persistState(next)
    }

    fun previewOffset(id: String): Float =
        if (draggingCardId == null) 0f else previewOffsets[id] ?: 0f

    private fun visibleIds(): List<String> = CardLayouts.default(page).order.keys
        .filterNot(state.hidden::contains)
        .sortedBy { state.order[it] ?: Int.MAX_VALUE }

    private fun isCenterAbove(id: String, draggedCenterY: Float): Boolean {
        val metric = cardMetrics[id] ?: return false
        return metric.top + metric.height / 2f < draggedCenterY
    }

    private fun visibleGap(): Float {
        val ids = visibleIds()
        val gaps = ids.zipWithNext { currentId, nextId ->
            val current = cardMetrics[currentId] ?: return@zipWithNext null
            val next = cardMetrics[nextId] ?: return@zipWithNext null
            next.top - (current.top + current.height)
        }.filterNotNull().filter { it >= 0f }
        return if (gaps.isEmpty()) 0f else gaps.average().toFloat()
    }

    private fun updatePreviewOffsets(session: CardDragSession, insertionIndex: Int) {
        val oldIndex = session.originalOrder.indexOf(session.cardId)
        val shift = session.draggedHeight + session.gap
        val nextOffsets = session.originalOrder.associateWith { id ->
            if (id == session.cardId) return@associateWith 0f
            val index = session.originalOrder.indexOf(id)
            when {
                index in (insertionIndex until oldIndex) -> shift
                index in ((oldIndex + 1)..insertionIndex) -> -shift
                else -> 0f
            }
        }
        previewOffsets.clear()
        previewOffsets.putAll(nextOffsets)
    }

    private fun reorderedMap(cardId: String, insertionIndex: Int): Map<String, Int> {
        val visible = visibleIds().toMutableList()
        visible.remove(cardId)
        visible.add(insertionIndex.coerceIn(0, visible.size), cardId)
        return state.order.toMutableMap().apply {
            visible.forEachIndexed { index, id -> this[id] = index + 1 }
        }
    }
}

private class CardDragSession(
    val cardId: String,
    val originalOrder: List<String>,
    val startCenterY: Float,
    val draggedHeight: Float,
    val gap: Float
) {
    var lastInsertionIndex = originalOrder.indexOf(cardId).coerceAtLeast(0)
}

private data class CardMetric(
    val top: Float,
    val height: Float
)

@Composable
fun rememberCardLayoutController(page: String, profileKey: String): CardLayoutController {
    val context = LocalContext.current
    val store = remember(context) { CardLayoutStore(context) }
    val scope = rememberCoroutineScope()
    val controller = remember(page, profileKey) {
        CardLayoutController(
            page = page,
            profileKey = profileKey,
            store = store,
            initialState = CardLayouts.default(page)
        ) { next ->
            scope.launch { runCatching { store.save(page, profileKey, next) } }
        }
    }

    LaunchedEffect(controller) {
        store.flow(page, profileKey).collect { loaded ->
            controller.updateFromStore(loaded)
        }
    }

    return controller
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
    val currentController by rememberUpdatedState(controller)
    val dragOffset = remember(cardId) { mutableFloatStateOf(0f) }
    var lastLayoutTop by remember(cardId) { mutableStateOf(Float.NaN) }
    val previewTarget by rememberUpdatedState(currentController.previewOffset(cardId))
    val animatedPreview by animateFloatAsState(
        targetValue = previewTarget,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cardDragPreview"
    )

    val measured = onGloballyPositioned { coordinates ->
        val top = coordinates.positionInRoot().y
        val height = coordinates.size.height.toFloat()
        lastLayoutTop = top
        currentController.recordMetric(cardId, top, height)
    }

    if (!enabled) {
        return measured
    }

    return measured
        .zIndex(if (currentController.draggingCardId == cardId) 3f else 0f)
        .graphicsLayer {
            val active = currentController.draggingCardId == cardId
            translationY = when {
                active -> dragOffset.floatValue
                currentController.draggingCardId != null -> animatedPreview
                else -> 0f
            }
            scaleX = if (active) 1.01f else 1f
            scaleY = if (active) 1.01f else 1f
            alpha = if (active) 0.98f else 1f
        }
        .draggable(
            orientation = Orientation.Vertical,
            enabled = true,
            state = rememberDraggableState { delta ->
                if (currentController.draggingCardId != cardId) return@rememberDraggableState
                dragOffset.floatValue += delta
                currentController.previewDragTo(dragOffset.floatValue)
            },
            onDragStarted = {
                dragOffset.floatValue = 0f
                currentController.beginDrag(cardId)
            },
            onDragStopped = {
                currentController.commitDrag()
            }
        )
}

@Composable
private fun ControlTool(label: String, symbol: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (symbol == "×") {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = label,
                    modifier = Modifier.size(19.dp)
                )
            } else {
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
