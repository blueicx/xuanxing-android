package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 欧洲占卜体系专属图标。 */
private fun divIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 希腊占星：神庙柱廊 + 星盘 */
val GreekAstrologyIcon: ImageVector by lazy {
    divIcon("GreekAstrologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 神庙三角顶
            moveTo(4f, 8f)
            lineTo(12f, 2f)
            lineTo(20f, 8f)
            close()
            // 横梁
            moveTo(3f, 8f)
            lineTo(21f, 8f)
            // 柱子
            moveTo(7f, 8f)
            lineTo(7f, 18f)
            moveTo(12f, 8f)
            lineTo(12f, 18f)
            moveTo(17f, 8f)
            lineTo(17f, 18f)
            // 基座
            moveTo(5f, 18f)
            lineTo(19f, 18f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 星
            moveTo(12f, 11.5f)
            arcTo(1.3f, 1.3f, 0f, false, false, 12f, 14.1f)
            arcTo(1.3f, 1.3f, 0f, false, false, 12f, 11.5f)
            close()
        }
    }
}

/** 凯尔特树历：树 + 欧甘 */
val CelticTreeIcon: ImageVector by lazy {
    divIcon("CelticTreeIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 树冠
            moveTo(12f, 2f)
            arcTo(4.5f, 4.5f, 0f, false, false, 12f, 11f)
            arcTo(4.5f, 4.5f, 0f, false, false, 12f, 2f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 树干
            moveTo(12f, 8f)
            lineTo(12f, 20f)
            // 根
            moveTo(12f, 20f)
            lineTo(9f, 22f)
            moveTo(12f, 20f)
            lineTo(15f, 22f)
        }
    }
}

/** 北欧符文：卢恩石 + 符文 */
val RuneIcon: ImageVector by lazy {
    divIcon("RuneIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 方石
            moveTo(5f, 3f)
            lineTo(19f, 3f)
            lineTo(19f, 21f)
            lineTo(5f, 21f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 符文（Fehu 意象）
            moveTo(8f, 7f)
            lineTo(16f, 7f)
            moveTo(8f, 7f)
            lineTo(8f, 17f)
            moveTo(8f, 10f)
            lineTo(12f, 10f)
        }
    }
}

/** 雷诺曼：卡牌 + 三叶草 */
val LenormandIcon: ImageVector by lazy {
    divIcon("LenormandIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 3f)
            lineTo(19f, 3f)
            lineTo(19f, 21f)
            lineTo(5f, 21f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 三叶草
            moveTo(12f, 11f)
            arcTo(2f, 2f, 0f, false, true, 12f, 15f)
            arcTo(2f, 2f, 0f, false, true, 12f, 11f)
            close()
            moveTo(9.5f, 12f)
            arcTo(1.6f, 1.6f, 0f, false, false, 9.5f, 15.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 9.5f, 12f)
            close()
            moveTo(14.5f, 12f)
            arcTo(1.6f, 1.6f, 0f, false, false, 14.5f, 15.2f)
            arcTo(1.6f, 1.6f, 0f, false, false, 14.5f, 12f)
            close()
        }
    }
}

/** 手相：手掌 + 掌纹 */
val PalmistryIcon: ImageVector by lazy {
    divIcon("PalmistryIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 手掌轮廓
            moveTo(8f, 5f)
            lineTo(16f, 5f)
            lineTo(18f, 8f)
            lineTo(18f, 14f)
            arcTo(2f, 2f, 0f, false, true, 14f, 15f)
            lineTo(14f, 17f)
            arcTo(1.5f, 1.5f, 0f, false, true, 11f, 17.5f)
            lineTo(11f, 15f)
            arcTo(2f, 2f, 0f, false, true, 7f, 15.5f)
            lineTo(7f, 11f)
            arcTo(2f, 2f, 0f, false, true, 6f, 9f)
            lineTo(6f, 7f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 掌纹（生命线/智慧线/感情线）
            moveTo(9f, 6f)
            arcTo(5f, 5f, 0f, false, false, 7f, 14f)
            moveTo(10f, 6f)
            arcTo(4f, 4f, 0f, false, true, 15f, 8f)
            moveTo(11f, 7f)
            arcTo(3f, 3f, 0f, false, false, 14f, 12f)
        }
    }
}

/** 数字命理学：数字 7 + 命理环 */
val NumerologyIcon: ImageVector by lazy {
    divIcon("NumerologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 数字 7
            moveTo(9f, 5f)
            lineTo(15f, 5f)
            lineTo(10f, 15f)
            lineTo(10f, 19f)
            lineTo(14f, 19f)
            lineTo(14f, 21f)
            lineTo(8f, 21f)
            lineTo(8f, 15f)
            lineTo(13f, 7f)
            lineTo(8f, 7f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(4f, 12f)
            arcTo(8f, 8f, 0f, false, false, 20f, 12f)
        }
    }
}

/** 赫尔墨斯 / 炼金术：硫汞盐三角 + 圆 */
val HermesAlchemyIcon: ImageVector by lazy {
    divIcon("HermesAlchemyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 炼金三角（硫）
            moveTo(12f, 3f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
            // 水平线（盐）
            moveTo(7f, 14f)
            lineTo(17f, 14f)
            // 垂直线（汞）
            moveTo(12f, 3f)
            lineTo(12f, 20f)
        }
        path(fill = SolidColor(Color.Black)) {
            // 外圈
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
        }
    }
}
