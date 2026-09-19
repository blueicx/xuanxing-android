package com.xuanji.app.domain.action

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.TestRecord
import kotlin.math.roundToInt

data class LifeProfileInput(
    val profileKey: String,
    val chart: BaziChart,
    val zodiacKey: String,
    val zodiacElement: Element,
    val tests: List<TestRecord>,
    val preferredCityKey: String?
)

class LifeProfilePlanner {
    fun plan(input: LifeProfileInput): LifeProfile {
        val favorable = input.chart.favorableElements.toSet().ifEmpty { setOf(input.chart.dayMasterElement) }
        val testText = input.tests.joinToString(" ") { "${it.testName} ${it.resultCode} ${it.resultName}" }
        val hasTests = input.tests.isNotEmpty()

        val careers = ActionCatalog.careers.map { candidate ->
            val chartFit = elementFit(favorable, candidate.elementTags)
            val zodiacFit = zodiacFit(input.zodiacElement, candidate.zodiacTags)
            val testFit = testFit(testText, candidate.testTags)
            val score = if (hasTests) {
                (chartFit * 0.45 + zodiacFit * 0.25 + testFit * 0.30).roundToInt()
            } else {
                (chartFit * 0.60 + zodiacFit * 0.40).roundToInt()
            }.coerceIn(0, 100)
            val reasons = buildList {
                add("五行匹配 ${chartFit} 分：${candidate.elementTags.joinToString { elementName(it) }}")
                add("星座元素匹配 ${zodiacFit} 分：${input.zodiacKey}")
                if (hasTests) add("已完成测试匹配 ${testFit} 分：${testLabel(input.tests)}")
                else add("未读取到职业/性格测试，暂不填默认分")
            }
            CareerCluster(candidate.key, candidate.label, score, reasons) to candidate
        }.sortedWith(compareByDescending<Pair<CareerCluster, CareerCandidate>> { it.first.score }
            .thenBy { StableScore.tieBreak(input.profileKey, "life", it.first.key) })
            .take(4)
            .map { it.first }

        val colors = ActionCatalog.colors.map { candidate ->
            val elementFit = elementFit(favorable, candidate.elementTags)
            val zodiacFit = zodiacFit(input.zodiacElement, candidate.zodiacTags)
            val styleFit = testFit(testText, candidate.testTags)
            val score = (elementFit * 0.60 + zodiacFit * 0.25 + styleFit * 0.15).roundToInt().coerceIn(0, 100)
            ColorCluster(
                key = candidate.key,
                label = candidate.label,
                hex = candidate.hex,
                reasons = listOf(
                    "五行色彩匹配 $elementFit 分",
                    "星座调色匹配 $zodiacFit 分",
                    if (hasTests) "测试风格匹配 $styleFit 分" else "没有测试风格输入，不填默认人格分"
                )
            ) to score
        }.sortedWith(compareByDescending<Pair<ColorCluster, Int>> { it.second }
            .thenBy { StableScore.tieBreak(input.profileKey, "colors", it.first.key) })
            .take(5)
            .map { it.first }

        val topCareer = ActionCatalog.careers.firstOrNull { it.key == careers.firstOrNull()?.key }
        val environmentTags = (topCareer?.environmentTags.orEmpty() + favorable.map { environmentFor(it) })
            .distinct().take(4)
        val environment = EnvironmentProfile(
            tags = environmentTags,
            rhythm = if (input.zodiacElement == Element.FIRE || input.zodiacElement == Element.WOOD) "适合有变化、可表达的节奏" else "适合留出整理、观察和恢复的节奏",
            reasons = listOf(
                "环境标签来自职业簇 ${topCareer?.label ?: "基础命盘"}",
                "五行偏好：${favorable.joinToString { elementName(it) }}",
                "星座元素：${elementName(input.zodiacElement)}"
            )
        )

        val regions = CityProfileCatalog.profiles.map { city ->
            val elementFit = elementEnvironmentFit(favorable, city.tags)
            val zodiacFit = zodiacEnvironmentFit(input.zodiacElement, city.tags)
            val careerFit = candidateEnvironmentFit(topCareer?.environmentTags.orEmpty(), city.tags)
            val explicitFit = if (city.key == input.preferredCityKey) 100 else 40
            val score = (elementFit * 0.35 + zodiacFit * 0.25 + careerFit * 0.20 + explicitFit * 0.20)
                .roundToInt().coerceIn(0, 100)
            val expectedTags = (topCareer?.environmentTags.orEmpty() + environmentTags).distinct()
            val matched = expectedTags.intersect(city.tags).toList()
            val unmatched = expectedTags.filterNot { it in city.tags }.take(3)
            RegionCandidate(city.country, city.city, matched, unmatched, score) to city
        }.sortedWith(compareByDescending<Pair<RegionCandidate, CityProfile>> { it.first.score }
            .thenBy { StableScore.tieBreak(input.profileKey, "regions", it.second.key) })
            .take(3)
            .map { it.first }

        val missing = buildList {
            if (!hasTests) add("职业/性格测试")
            if (input.preferredCityKey.isNullOrBlank()) add("手动城市偏好")
        }
        val confidence = when {
            !hasTests -> ConfidenceLevel.Low
            !input.preferredCityKey.isNullOrBlank() -> ConfidenceLevel.High
            else -> ConfidenceLevel.Medium
        }
        val evidence = listOf(
            ActionEvidence(ActionSource.FiveElements, "favorable", "喜用五行：${favorable.joinToString { elementName(it) }}", 45),
            ActionEvidence(ActionSource.Zodiac, input.zodiacKey, "星座元素：${elementName(input.zodiacElement)}", 25)
        ) + if (hasTests) listOf(
            ActionEvidence(ActionSource.DailyFortune, "tests", "读取已保存测试结果：${testLabel(input.tests)}", 30)
        ) else emptyList()

        return LifeProfile(
            profileKey = input.profileKey,
            careerClusters = careers,
            colorPalette = colors,
            environmentProfile = environment,
            regionCandidates = regions,
            evidence = evidence,
            confidence = confidence,
            missingInputs = missing,
            disclaimer = "这是基于命盘、星座、已保存测试和静态环境标签的兴趣/生活方式画像，不是能力评估、心理诊断、招聘建议、移民或旅行安全结论。"
        )
    }

    private fun elementFit(favorable: Set<Element>, candidate: Set<Element>): Int =
        if (candidate.isEmpty()) 0 else ((favorable.intersect(candidate).size.toDouble() / candidate.size) * 100).roundToInt().coerceIn(0, 100)

    private fun zodiacFit(zodiac: Element, candidate: Set<Element>): Int = if (zodiac in candidate) 100 else 35

    private fun testFit(text: String, tags: Set<String>): Int {
        if (text.isBlank() || tags.isEmpty()) return 0
        val hits = tags.count { text.contains(it, ignoreCase = true) }
        return (hits.toDouble() / tags.size * 100).roundToInt().coerceIn(0, 100)
    }

    private fun elementEnvironmentFit(favorable: Set<Element>, tags: Set<String>): Int {
        val expected = favorable.flatMap { elementEnvironmentTags(it) }.toSet()
        return if (expected.isEmpty()) 40 else (expected.intersect(tags).size * 100 / expected.size).coerceIn(0, 100)
    }

    private fun zodiacEnvironmentFit(zodiac: Element, tags: Set<String>): Int =
        if (elementEnvironmentTags(zodiac).any { it in tags }) 100 else 35

    private fun candidateEnvironmentFit(expected: Set<String>, actual: Set<String>): Int =
        if (expected.isEmpty()) 40 else (expected.intersect(actual).size * 100 / expected.size).coerceIn(0, 100)

    private fun elementEnvironmentTags(element: Element): Set<String> = when (element) {
        Element.WOOD -> setOf("green", "forest", "culture")
        Element.FIRE -> setOf("sun", "night", "urban")
        Element.EARTH -> setOf("slow", "food", "history")
        Element.METAL -> setOf("culture", "quiet", "urban")
        Element.WATER -> setOf("waterfront", "coast", "quiet")
    }

    private fun environmentFor(element: Element): String = when (element) {
        Element.WOOD -> "green"
        Element.FIRE -> "urban"
        Element.EARTH -> "slow"
        Element.METAL -> "culture"
        Element.WATER -> "waterfront"
    }

    private fun testLabel(tests: List<TestRecord>): String =
        tests.take(3).joinToString("、") { "${it.testName}:${it.resultCode}" }

    private fun elementName(element: Element): String = when (element) {
        Element.WOOD -> "木"
        Element.FIRE -> "火"
        Element.EARTH -> "土"
        Element.METAL -> "金"
        Element.WATER -> "水"
    }
}
