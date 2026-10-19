package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.ReadingFavoriteCodec
import com.xuanji.app.domain.ReadingFavoriteDataState
import kotlinx.coroutines.launch

@Composable
internal fun FortuneDetailPage(
    title: String,
    onDismiss: () -> Unit,
    detailContext: MysticDetailContext? = null,
    favoriteType: String = "fortune",
    contentIncludesEvidence: Boolean = false,
    onAskMystic: ((MysticDetailContext) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val favoriteStore = LocalReadingFavoriteStore.current
    val profileKey = LocalReadingFavoriteProfileKey.current
    val coroutineScope = rememberCoroutineScope()
    val favorite = remember(detailContext, favoriteType) {
        detailContext?.let { detail ->
            ReadingFavorite(
                id = ReadingFavoriteCodec.stableId(favoriteType, detail),
                type = favoriteType,
                title = detail.title,
                dateKey = detail.dateKey,
                period = detail.period,
                mode = detail.mode,
                body = detail.result,
                evidence = detail.evidence,
                algorithmVersion = detail.algorithmVersion,
                createdAt = System.currentTimeMillis()
            )
        }
    }
    var isFavorite by remember(profileKey, favorite?.id) { mutableStateOf(false) }
    var favoriteMessage by remember(profileKey, favorite?.id) { mutableStateOf<String?>(null) }
    var favoriteBusy by remember(profileKey, favorite?.id) { mutableStateOf(false) }
    LaunchedEffect(favoriteStore, profileKey, favorite?.id) {
        if (favoriteStore == null || profileKey.isNullOrBlank() || favorite == null) return@LaunchedEffect
        val loaded = runCatching { favoriteStore.list(profileKey) }.getOrNull() ?: return@LaunchedEffect
        if (loaded.state in setOf(ReadingFavoriteDataState.Ready, ReadingFavoriteDataState.Migrated, ReadingFavoriteDataState.Missing)) {
            isFavorite = loaded.entries.any { it.id == favorite.id }
            favoriteMessage = null
        } else {
            favoriteMessage = "收藏数据暂不可读；请到「我的收藏」处理后再保存。"
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fortune-detail-page")
                .testTag("fortune-detail-page-$title"),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "返回$title"
                    }
                ) {
                    Text("‹ 返回")
                }
                Text(
                    text = "单项详解",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    modifier = Modifier.testTag("fortune-detail-title-$title"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                        text = "展开阅读具体解释与计算依据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "具体解释",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    content()
                    detailContext?.let { detail ->
                        if (!contentIncludesEvidence && detail.evidence.isNotBlank()) {
                            Text("依据", style = MaterialTheme.typography.titleMedium)
                            Text(detail.evidence, style = MaterialTheme.typography.bodySmall)
                        }
                        RelatedLearningTerms("${detail.title} ${detail.result} ${detail.evidence}")
                    }
                    detailContext?.let { SaveReadingSnapshotAction(it) }
                }
                if (detailContext != null && (onAskMystic != null || (favoriteStore != null && !profileKey.isNullOrBlank()))) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (favoriteStore != null && !profileKey.isNullOrBlank() && favorite != null) {
                            OutlinedButton(
                                onClick = {
                                    if (favoriteBusy) return@OutlinedButton
                                    favoriteBusy = true
                                    coroutineScope.launch {
                                        try {
                                            val current = runCatching { favoriteStore.list(profileKey) }.getOrNull()
                                            if (current == null || current.state !in setOf(
                                                    ReadingFavoriteDataState.Ready,
                                                    ReadingFavoriteDataState.Migrated,
                                                    ReadingFavoriteDataState.Missing
                                                )) {
                                                favoriteMessage = "收藏数据暂不可读；请到「我的收藏」处理后再保存。"
                                            } else {
                                                val updated = runCatching {
                                                    if (current.entries.any { it.id == favorite.id }) {
                                                        favoriteStore.remove(profileKey, favorite.id)
                                                    } else {
                                                        favoriteStore.upsert(profileKey, favorite)
                                                    }
                                                }.getOrNull()
                                                if (updated != null && updated.state in setOf(
                                                        ReadingFavoriteDataState.Ready,
                                                        ReadingFavoriteDataState.Missing
                                                    )) {
                                                    isFavorite = updated.entries.any { it.id == favorite.id }
                                                    favoriteMessage = if (isFavorite) "已收藏到「我的收藏」" else "已取消收藏"
                                                } else {
                                                    favoriteMessage = "收藏暂时无法保存，请稍后重试。"
                                                }
                                            }
                                        } finally {
                                            favoriteBusy = false
                                        }
                                    }
                                },
                                enabled = !favoriteBusy,
                                modifier = Modifier.testTag("fortune-detail-toggle-favorite")
                            ) { Text(if (isFavorite) "取消收藏" else "收藏") }
                        }
                        if (onAskMystic != null) {
                            Button(
                                onClick = { onAskMystic.invoke(detailContext) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fortune-detail-ask-mystic")
                                    .semantics { contentDescription = "带着${detailContext.title}的当前结果询问玄师" }
                            ) {
                                Text("问玄师")
                            }
                        }
                    }
                    favoriteMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
