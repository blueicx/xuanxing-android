package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.*

@Composable
fun LearningSearchPanel(favorites: List<ReadingFavorite>, onNavigate: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(SearchCategory.All.name) }
    var selectedEntry by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFavorite by rememberSaveable { mutableStateOf<String?>(null) }
    val category = SearchCategory.entries.firstOrNull { it.name == categoryName } ?: SearchCategory.All
    val results = remember(query, favorites, category) { LocalSearchIndex.search(query, favorites, category) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(query, { query = it.take(100) }, modifier = Modifier.fillMaxWidth().testTag("unified-search-input"),
            label = { Text("搜索功能、术语、学习与收藏") }, singleLine = true)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(SearchCategory.All to "全部", SearchCategory.Feature to "功能", SearchCategory.Term to "术语",
                SearchCategory.Article to "短解说", SearchCategory.Favorite to "收藏").forEach { (value, label) ->
                FilterChip(value == category, { categoryName = value.name }, label = { Text(label) })
            }
        }
        if (results.isEmpty()) Text("没有匹配内容，试试简称或其他关键词。", modifier = Modifier.testTag("unified-search-empty"))
        results.forEach { result -> Surface(onClick = {
            result.route?.let(onNavigate)
            selectedEntry = result.entryId
            selectedFavorite = result.favoriteId
        }, modifier = Modifier.fillMaxWidth().testTag("search-result-${result.id}"), shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(result.title, style = MaterialTheme.typography.titleMedium)
                Text(result.preview.take(100), style = MaterialTheme.typography.bodySmall)
            }
        } }
    }
    selectedEntry?.let { id -> LearningCatalog.find(id)?.let { entry -> LearningEntryPage(entry) { selectedEntry = null } } }
    favorites.firstOrNull { it.id == selectedFavorite }?.let { item ->
        FortuneDetailPage(item.title, { selectedFavorite = null }) { Text(item.body); Text(item.evidence) }
    }
}

@Composable
internal fun LearningEntryPage(entry: LearningEntry, onDismiss: () -> Unit) {
    FortuneDetailPage(entry.title, onDismiss) {
        Text(entry.body, style = MaterialTheme.typography.bodyLarge)
        Text("来源", style = MaterialTheme.typography.titleMedium); Text(entry.source)
        Text("适用范围", style = MaterialTheme.typography.titleMedium); Text(entry.scope)
    }
}

@Composable
internal fun RelatedLearningTerms(text: String) {
    var selected by rememberSaveable(text) { mutableStateOf<String?>(null) }
    val terms = remember(text) { LearningCatalog.related(text) }
    if (terms.isNotEmpty()) {
        Text("相关术语", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            terms.forEach { term -> OutlinedButton(onClick = { selected = term.id }) { Text(term.title) } }
        }
    }
    selected?.let { id -> LearningCatalog.find(id)?.let { entry -> LearningEntryPage(entry) { selected = null } } }
}
