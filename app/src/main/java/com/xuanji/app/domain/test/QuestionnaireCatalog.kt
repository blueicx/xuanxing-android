package com.xuanji.app.domain.test

object QuestionnaireCatalog {
    val ids: Set<String> = setOf("bigfive", "cattell-style", "holland-style", "mmpi-style", "mbti-style", "hogwarts") +
        FunTests.SUBTESTS.map { "fun1-${it.id.lowercase()}" } +
        FunTests2.SUBTESTS.map { "fun2-${it.id.lowercase()}" } +
        FunTests3.SUBTESTS.map { "fun3-${it.id.lowercase()}" }
}
