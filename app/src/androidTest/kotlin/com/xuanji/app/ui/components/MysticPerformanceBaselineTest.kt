package com.xuanji.app.ui.components

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.xuanji.app.domain.MysticCharacterCatalog
import com.xuanji.app.domain.game.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Timing baselines, not screenshot or physical-device acceptance; synthetic UI only. */
class MysticPerformanceBaselineTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repeated_scenes_record_median_and_continuously_sample_memory_without_user_stores() {
        val opened = mutableStateOf(false)
        val role = mutableStateOf(MysticCharacterCatalog.all.first())
        compose.setContent {
            if (!opened.value) Button(onClick = { opened.value = true }) { Text("重复测量舞台") }
            else MysticStageLayout(role.value, role.value.legacySkinId, Color.DarkGray, Color.Yellow, 0f,
                onCharacterSelected = { role.value = MysticCharacterCatalog.byId(it) }, onConversation = {},
                onStartGame = {}, onClose = { opened.value = false }) { Text("合成性能消息") }
        }
        val samples = java.util.Collections.synchronizedList(mutableListOf<Int>())
        val sampler = CoroutineScope(Dispatchers.IO).launch {
            while(isActive) {
                samples.add(Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss)
                delay(50)
            }
        }
        val elapsed = mutableListOf<Long>()
        try {
            repeat(3) {
                MysticCharacterCatalog.all.forEach { character ->
                    compose.runOnIdle { role.value = character }
                    val started = SystemClock.elapsedRealtimeNanos()
                    compose.onNodeWithText("重复测量舞台").performClick()
                    compose.waitUntil(15_000) { compose.onAllNodesWithTag("mystic-artwork-${figureAssetId(character.visualStyleId).name}").fetchSemanticsNodes().isNotEmpty() }
                    elapsed.add((SystemClock.elapsedRealtimeNanos() - started) / 1_000_000)
                    compose.onNodeWithContentDescription("关闭玄师台").performClick()
                }
            }
        } finally { runBlocking { sampler.cancelAndJoin() } }
        assertEquals(12, elapsed.size)
        assertTrue(samples.size > 10)
        val pss = samples.toList()
        Log.i("X3RepeatedPerf", JSONObject().put("scope", "isolated component host, warmed cache; 50ms PSS sampling, observed maximum not absolute peak; not physical device acceptance")
            .put("readyMs", JSONArray(elapsed)).put("medianReadyMs", elapsed.sorted()[elapsed.size / 2])
            .put("sampleCount", pss.size).put("pssMinKiB", pss.minOrNull()).put("observedPeakPssKiB", pss.maxOrNull()).toString())
    }

    @Test fun measure_four_real_scenes_and_rules_checked_board_search_without_user_stores() {
        val opened = mutableStateOf(false)
        val role = mutableStateOf(MysticCharacterCatalog.all.first())
        compose.setContent {
            Box {
                if (!opened.value) Button(onClick = { opened.value = true }) { Text("打开测量舞台") }
                else MysticStageLayout(role.value, role.value.legacySkinId, Color.DarkGray, Color.Yellow, 0f,
                    onCharacterSelected = { role.value = MysticCharacterCatalog.byId(it) }, onConversation = {}, onStartGame = {},
                    onClose = { opened.value = false }) { Text("合成性能测试消息") }
            }
        }
        val samples = JSONArray()
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        MysticCharacterCatalog.all.forEach { character ->
            compose.runOnIdle { role.value = character }
            val started = SystemClock.elapsedRealtimeNanos()
            compose.onNodeWithText("打开测量舞台").performClick()
            val artworkTag = "mystic-artwork-${figureAssetId(character.visualStyleId).name}"
            compose.waitUntil(10_000) { compose.onAllNodesWithTag(artworkTag).fetchSemanticsNodes().isNotEmpty() }
            compose.waitForIdle()
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
            val memory = Debug.MemoryInfo().also(Debug::getMemoryInfo)
            val bitmap = runBlocking { MysticFigureBitmapCache.load(resources, resourceId(figureAssetId(character.visualStyleId))) }
            assertNotNull(bitmap)
            samples.put(JSONObject().put("character", character.id.key).put("stageReadyMs", elapsedMs)
                .put("sampledPssKiB", memory.totalPss).put("bitmapBytes", bitmap!!.allocationByteCount)
                .put("bitmapWidth", bitmap.width).put("bitmapHeight", bitmap.height))
            compose.onNodeWithContentDescription("关闭玄师台").performClick()
        }
        val searchSamples = JSONArray()
        val positions = EndgameCatalog.ALL.map { it.id to it.position() } + ("initial-black" to XiangqiBoard.initial().copy(sideToMove = PlayerColor.BLACK))
        positions.forEach { (id, position) ->
            var nodes = 0
            val started = SystemClock.elapsedRealtimeNanos()
            val result = runBlocking { SmartBoardEngine(SmartBoardEngine.NORMAL) { nodes++ }.bestMove(position, position.sideToMove, 1) }
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
            assertTrue(result is EngineResult.Move)
            assertTrue(XiangqiRules.apply(position, (result as EngineResult.Move).turn.move) is RuleResult.Applied)
            searchSamples.put(JSONObject().put("position", id).put("elapsedMs", elapsedMs).put("nodes", nodes))
        }
        Log.i("X3Perf", JSONObject().put("scope", "Debug isolated component host; observed PSS samples, not continuous peak; cache may be warm")
            .put("stage", samples).put("search", searchSamples).toString())
    }
}
