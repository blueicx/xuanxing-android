package com.xuanji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.MysticCharacterProfile

data class MysticCharacterUiModel(
    val id: MysticCharacterId,
    val title: String,
    val subtitle: String,
    val cultureLabel: String,
    val visualStyleId: String,
    val specialties: List<String>,
    val primarySpecialty: String,
    val specialtySources: List<String>,
    val actions: List<String>,
    val usesScenePlate: Boolean
) {
    companion object {
        fun from(profile: MysticCharacterProfile): MysticCharacterUiModel = MysticCharacterUiModel(
            id = profile.id,
            title = profile.displayName,
            subtitle = profile.title,
            cultureLabel = profile.cultureLabel,
            visualStyleId = profile.visualStyleId,
            specialties = profile.specialties.map { it.label },
            primarySpecialty = profile.specialties.firstOrNull()?.label ?: "综合合参",
            specialtySources = profile.specialties.map { it.sourceLabel },
            actions = listOf("进入对话", "查看专长", "来一盘象棋"),
            usesScenePlate = profile.visualStyleId != "elder_ink"
        )
    }
}

data class MysticDialogueBubbleModel(
    val fullText: String,
    val expanded: Boolean = false,
    val previewLimit: Int = 112
) {
    val shouldCollapse: Boolean get() = fullText.length > previewLimit
    val preview: String
        get() = if (!shouldCollapse || expanded) fullText else fullText.take(previewLimit).trimEnd() + "…"

    fun toggle(): MysticDialogueBubbleModel = copy(expanded = !expanded)
}

data class MysticGameThemePalette(
    val key: String,
    val label: String,
    val panelColor: Color,
    val boardColor: Color,
    val lineColor: Color,
    val redPieceColor: Color,
    val blackPieceColor: Color
)

fun mysticGameThemePalette(key: String): MysticGameThemePalette = when (key) {
    "jiangnan_wood" -> MysticGameThemePalette(
        key, "江南木案",
        Color(0xFF2A2117), Color(0xFFE3C999), Color(0xFF76543B), Color(0xFF9B2E26), Color(0xFF241D19)
    )
    "ink_paper" -> MysticGameThemePalette(
        key, "水墨宣纸",
        Color(0xFF252525), Color(0xFFE8E1D1), Color(0xFF4A4A45), Color(0xFF7D2723), Color(0xFF171717)
    )
    "academy_star" -> MysticGameThemePalette(
        key, "学院星盘",
        Color(0xFF1B1D34), Color(0xFFDDD8C2), Color(0xFF6A5A75), Color(0xFF9E3B43), Color(0xFF20233A)
    )
    "silkroad_copper" -> MysticGameThemePalette(
        key, "丝路铜盘",
        Color(0xFF302017), Color(0xFFD7B27A), Color(0xFF815735), Color(0xFF9D4430), Color(0xFF29201A)
    )
    else -> MysticGameThemePalette(
        "default", "玄师棋局",
        Color(0xFF221A13), Color(0xFFE8D5B0), Color(0xFF7A5C43), Color(0xFFB3261E), Color(0xFF26211C)
    )
}

@Composable
internal fun MysticCharacterGallery(
    selectedId: MysticCharacterId,
    onSelected: (MysticCharacterId) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = MysticCharacterCatalog.all
    val selectedIndex = profiles.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    LaunchedEffect(selectedId) {
        listState.animateScrollToItem(selectedIndex)
    }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        profiles.getOrNull(listState.firstVisibleItemIndex)?.let { profile ->
            if (profile.id != selectedId) onSelected(profile.id)
        }
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "玄师角色画廊，可左右滑动切换角色" },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(profiles, key = { it.id.key }) { profile ->
            val selected = profile.id == selectedId
            Surface(
                onClick = { onSelected(profile.id) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Color(0xCC2B1B42) else Color(0x99201731),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color(0xFFD9C58B) else Color(0x557B668F)
                ),
                modifier = Modifier
                    .size(width = 156.dp, height = 66.dp)
                    .testTag("character-card-${profile.id.key}")
                    .semantics {
                        contentDescription = "${profile.displayName}，${profile.title}，${profile.cultureLabel}"
                    }
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFF4EEE5)
                    )
                    Text(
                        profile.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD9C58B)
                    )
                    Text(
                        profile.cultureLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCEBEDD),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun MysticCharacterActionBar(
    character: MysticCharacterProfile,
    onConversation: () -> Unit,
    onSpecialties: () -> Unit,
    onGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        OutlinedButton(
            onClick = onConversation,
            modifier = Modifier.weight(1f).semantics { contentDescription = "与${character.displayName}进入对话" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 5.dp)
        ) { Text("进入对话", style = MaterialTheme.typography.labelSmall) }
        OutlinedButton(
            onClick = onSpecialties,
            modifier = Modifier.weight(1f).semantics { contentDescription = "查看${character.displayName}的专长" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 5.dp)
        ) { Text("查看专长", style = MaterialTheme.typography.labelSmall) }
        OutlinedButton(
            onClick = onGame,
            modifier = Modifier.weight(1f).semantics { contentDescription = "与${character.displayName}来一盘象棋" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 5.dp)
        ) { Text("来一盘象棋", style = MaterialTheme.typography.labelSmall) }
    }
}
