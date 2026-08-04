package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 中东与西亚 / 非洲占卜体系专属图标。 */
private fun divIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 巴比伦占星：楔形泥板 + 星 */
val BabylonianIcon: ImageVector by lazy {
    divIcon("BabylonianIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 泥板（斜方）
            moveTo(4f, 5f)
            lineTo(20f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 楔形刻痕
            moveTo(7f, 9f)
            lineTo(10f, 9f)
            lineTo(10f, 10f)
            lineTo(7f, 10f)
            close()
            moveTo(7f, 13f)
            lineTo(11f, 13f)
            lineTo(11f, 14f)
            lineTo(7f, 14f)
            close()
            // 星
            moveTo(16f, 9f)
            lineTo(16.6f, 10.8f)
            lineTo(18.4f, 11f)
            lineTo(17.2f, 12.2f)
            lineTo(17.6f, 14f)
            lineTo(16f, 13.2f)
            lineTo(14.4f, 14f)
            lineTo(14.8f, 12.2f)
            lineTo(13.6f, 11f)
            lineTo(15.4f, 10.8f)
            close()
        }
    }
}

/** 阿拉伯占星：新月 + 星 */
val ArabicAstrologyIcon: ImageVector by lazy {
    divIcon("ArabicAstrologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 新月
            moveTo(14f, 3f)
            arcTo(9f, 9f, 0f, false, false, 14f, 21f)
            arcTo(7f, 7f, 0f, false, true, 14f, 3f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 八角星
            moveTo(6f, 13f)
            lineTo(7f, 11f)
            lineTo(8f, 13f)
            lineTo(10f, 13f)
            lineTo(8.5f, 14.3f)
            lineTo(9.2f, 16.3f)
            lineTo(7f, 15.2f)
            lineTo(4.8f, 16.3f)
            lineTo(5.5f, 14.3f)
            lineTo(4f, 13f)
            close()
        }
    }
}

/** 波斯占星：波斯星轮 / 火焰轮盘 */
val PersianAstrologyIcon: ImageVector by lazy {
    divIcon("PersianAstrologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
            // 内轮 + 辐条
            moveTo(12f, 7f)
            arcTo(5f, 5f, 0f, false, false, 12f, 17f)
            arcTo(5f, 5f, 0f, false, false, 12f, 7f)
            close()
            moveTo(12f, 3f)
            lineTo(12f, 7f)
            moveTo(12f, 17f)
            lineTo(12f, 21f)
            moveTo(3f, 12f)
            lineTo(7f, 12f)
            moveTo(17f, 12f)
            lineTo(21f, 12f)
        }
        path(fill = SolidColor(Color.Black)) {
            // 火焰点
            moveTo(12f, 10.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 13.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 10.5f)
            close()
        }
    }
}

/** 也门占星：南阿拉伯庙塔 + 星宫 */
val YemeniAstrologyIcon: ImageVector by lazy {
    divIcon("YemeniAstrologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 尖塔
            moveTo(8f, 20f)
            lineTo(4f, 20f)
            lineTo(12f, 3f)
            lineTo(20f, 20f)
            lineTo(16f, 20f)
            lineTo(12f, 10f)
            close()
            // 塔基
            moveTo(6f, 20f)
            lineTo(18f, 20f)
            lineTo(18f, 22f)
            lineTo(6f, 22f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 星点
            moveTo(12f, 14f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 15.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 14f)
            close()
        }
    }
}

/** 犹太占星（卡巴拉）：生命之树（十质点连线） */
val KabbalahIcon: ImageVector by lazy {
    divIcon("KabbalahIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 十质点 + 连线（简化生命之树）
            moveTo(12f, 3f)
            lineTo(9f, 6f)
            lineTo(15f, 6f)
            close()
            moveTo(12f, 6f)
            lineTo(12f, 10f)
            lineTo(9f, 6f)
            moveTo(12f, 6f)
            lineTo(15f, 6f)
            // 中央柱
            moveTo(12f, 10f)
            lineTo(12f, 19f)
            moveTo(12f, 10f)
            lineTo(6f, 16f)
            moveTo(12f, 10f)
            lineTo(18f, 16f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 4.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 3f)
            close()
            moveTo(9f, 6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 9f, 7.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 9f, 6f)
            close()
            moveTo(15f, 6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 15f, 7.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 15f, 6f)
            close()
            moveTo(12f, 10f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 11.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 10f)
            close()
            moveTo(6f, 16f)
            arcTo(0.8f, 0.8f, 0f, false, false, 6f, 17.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 6f, 16f)
            close()
            moveTo(18f, 16f)
            arcTo(0.8f, 0.8f, 0f, false, false, 18f, 17.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 18f, 16f)
            close()
            moveTo(12f, 19f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 20.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 12f, 19f)
            close()
        }
    }
}

/** 艾法预言：Opele 链 + 棕榈果 */
val IfaIcon: ImageVector by lazy {
    divIcon("IfaIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 链（一串点连线）
            moveTo(5f, 5f)
            arcTo(1.6f, 1.6f, 0f, false, false, 5f, 8.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 5f, 5f)
            close()
            moveTo(10f, 9f)
            arcTo(1.6f, 1.6f, 0f, false, false, 10f, 12.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 10f, 9f)
            close()
            moveTo(15f, 5f)
            arcTo(1.6f, 1.6f, 0f, false, false, 15f, 8.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 15f, 5f)
            close()
            moveTo(19f, 12f)
            arcTo(1.6f, 1.6f, 0f, false, false, 19f, 15.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 19f, 12f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 连线
            moveTo(6.6f, 6.6f)
            lineTo(8.4f, 10.4f)
            moveTo(11.6f, 10.6f)
            lineTo(13.4f, 6.6f)
            moveTo(16.6f, 6.6f)
            lineTo(17.4f, 10.4f)
        }
    }
}
