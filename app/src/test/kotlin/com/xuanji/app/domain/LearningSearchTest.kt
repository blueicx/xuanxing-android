package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test

class LearningSearchTest {
    @Test fun learning_directory_contains_sourced_distinct_terms_and_articles() {
        assertEquals(60, LearningCatalog.terms.size)
        assertEquals(16, LearningCatalog.articles.size)
        assertEquals(76, (LearningCatalog.terms + LearningCatalog.articles).map { it.id }.toSet().size)
        assertTrue((LearningCatalog.terms + LearningCatalog.articles).all {
            it.body.isNotBlank() && it.source.isNotBlank() && it.scope.isNotBlank()
        })
    }
    @Test fun local_search_handles_alias_category_and_current_profile_favorites() {
        val favorite = ReadingFavorite("f", "action", "番茄面", body = "收藏的家常菜", createdAt = 1)
        assertTrue(LocalSearchIndex.search("农历", emptyList()).any { it.entryId == "term-lunar" })
        assertTrue(LocalSearchIndex.search("塔罗牌阵", emptyList()).any { it.route == "divination/tarot" })
        assertEquals("f", LocalSearchIndex.search("番茄", listOf(favorite), SearchCategory.Favorite).single().favoriteId)
        assertTrue(LocalSearchIndex.search("番茄", emptyList(), SearchCategory.Favorite).isEmpty())
        assertTrue(LocalSearchIndex.search("不存在的词abc", listOf(favorite)).isEmpty())
    }
}
