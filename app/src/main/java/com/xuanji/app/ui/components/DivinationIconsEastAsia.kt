package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 东亚 / 东南亚 / 南亚占卜体系专属图标。 */
private fun divIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 九星气学：九宫格 + 中心星点 */
val NineStarsIcon: ImageVector by lazy {
    divIcon("NineStarsIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(3f, 9f)
            lineTo(21f, 9f)
            moveTo(3f, 15f)
            lineTo(21f, 15f)
            moveTo(9f, 3f)
            lineTo(9f, 21f)
            moveTo(15f, 3f)
            lineTo(15f, 21f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 10.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 13.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 10.5f)
            close()
        }
    }
}

/** 阴阳道：阴阳太极 + 北斗七星点 */
val OnmyodoIcon: ImageVector by lazy {
    divIcon("OnmyodoIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
            // 太极 S 线
            moveTo(12f, 3f)
            arcTo(4.5f, 4.5f, 0f, false, false, 12f, 12f)
            arcTo(4.5f, 4.5f, 0f, false, false, 12f, 21f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(15f, 7f)
            arcTo(0.9f, 0.9f, 0f, false, false, 15f, 8.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 15f, 7f)
            close()
            moveTo(17f, 10f)
            arcTo(0.9f, 0.9f, 0f, false, false, 17f, 11.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 17f, 10f)
            close()
            moveTo(18.5f, 13.5f)
            arcTo(0.9f, 0.9f, 0f, false, false, 18.5f, 15.3f)
            arcTo(0.9f, 0.9f, 0f, false, false, 18.5f, 13.5f)
            close()
        }
    }
}

/** 缅甸黄道带：八瓣曼陀罗轮 */
val MahaboteIcon: ImageVector by lazy {
    divIcon("MahaboteIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
            // 八瓣花瓣
            moveTo(12f, 3f)
            arcTo(2f, 2f, 0f, false, true, 12f, 8f)
            arcTo(2f, 2f, 0f, false, true, 12f, 3f)
            close()
            moveTo(12f, 16f)
            arcTo(2f, 2f, 0f, false, true, 12f, 21f)
            arcTo(2f, 2f, 0f, false, true, 12f, 16f)
            close()
            moveTo(5.4f, 8f)
            arcTo(2f, 2f, 0f, false, true, 8f, 10f)
            arcTo(2f, 2f, 0f, false, true, 5.4f, 8f)
            close()
            moveTo(16f, 14f)
            arcTo(2f, 2f, 0f, false, true, 18.6f, 16f)
            arcTo(2f, 2f, 0f, false, true, 16f, 14f)
            close()
        }
    }
}

/** 高棉占星：吴哥尖塔 */
val KhmerIcon: ImageVector by lazy {
    divIcon("KhmerIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 20f)
            lineTo(4f, 20f)
            lineTo(9f, 10f)
            lineTo(9f, 5f)
            lineTo(12f, 2f)
            lineTo(15f, 5f)
            lineTo(15f, 10f)
            lineTo(20f, 20f)
            lineTo(17f, 20f)
            lineTo(12f, 12f)
            close()
            moveTo(7f, 20f)
            lineTo(17f, 20f)
            lineTo(17f, 22f)
            lineTo(7f, 22f)
            close()
        }
    }
}

/** 那伽占雨：神蛇（波浪蛇形） */
val NagaRainIcon: ImageVector by lazy {
    divIcon("NagaRainIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 16f)
            arcTo(2.5f, 2.5f, 0f, false, true, 9f, 16f)
            arcTo(2.5f, 2.5f, 0f, false, true, 14f, 16f)
            arcTo(2.5f, 2.5f, 0f, false, true, 19f, 16f)
            lineTo(19f, 12f)
            // 蛇头
            arcTo(1.8f, 1.8f, 0f, false, false, 15.4f, 12f)
            lineTo(15.4f, 12f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 蛇眼
            moveTo(18f, 12f)
            arcTo(0.6f, 0.6f, 0f, false, false, 18f, 13.2f)
            arcTo(0.6f, 0.6f, 0f, false, false, 18f, 12f)
            close()
            // 雨滴
            moveTo(8f, 5f)
            lineTo(8f, 8f)
            moveTo(13f, 5f)
            lineTo(13f, 8f)
        }
    }
}

/** Tajul Muluk：王冠 + 宝石 */
val TajulMulukIcon: ImageVector by lazy {
    divIcon("TajulMulukIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 8f)
            lineTo(8f, 4f)
            lineTo(10f, 8f)
            lineTo(12f, 4f)
            lineTo(14f, 8f)
            lineTo(16f, 4f)
            lineTo(19f, 8f)
            lineTo(19f, 12f)
            lineTo(5f, 12f)
            close()
            moveTo(5f, 12f)
            lineTo(5f, 16f)
            lineTo(19f, 16f)
            lineTo(19f, 12f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 9f)
            arcTo(1.2f, 1.2f, 0f, false, false, 12f, 11.4f)
            arcTo(1.2f, 1.2f, 0f, false, false, 12f, 9f)
            close()
        }
    }
}

/** 印度占星（吠陀）：曼陀罗 + 月亮星宿 */
val VedicIcon: ImageVector by lazy {
    divIcon("VedicIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, false, false, 12f, 22f)
            arcTo(10f, 10f, 0f, false, false, 12f, 2f)
            close()
            // 曼陀罗花瓣
            moveTo(12f, 2f)
            arcTo(4f, 4f, 0f, false, true, 12f, 8f)
            arcTo(4f, 4f, 0f, false, true, 12f, 2f)
            close()
            moveTo(12f, 16f)
            arcTo(4f, 4f, 0f, false, true, 12f, 22f)
            arcTo(4f, 4f, 0f, false, true, 12f, 16f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 月亮（星宿）
            moveTo(12f, 10.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 13.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 10.5f)
            close()
        }
    }
}

/** 纳迪占星：棕榈叶 + 指纹 */
val NadiIcon: ImageVector by lazy {
    divIcon("NadiIcon") {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 棕榈叶
            moveTo(12f, 2f)
            lineTo(12f, 20f)
            moveTo(12f, 6f)
            lineTo(5f, 4f)
            moveTo(12f, 10f)
            lineTo(19f, 8f)
            moveTo(12f, 14f)
            lineTo(5f, 16f)
        }
        path(fill = SolidColor(Color.Black)) {
            // 指纹螺旋
            moveTo(18f, 14f)
            arcTo(3f, 3f, 0f, false, false, 18f, 20f)
            arcTo(3f, 3f, 0f, false, false, 18f, 14f)
            close()
            moveTo(18f, 16f)
            arcTo(1.5f, 1.5f, 0f, false, false, 18f, 19f)
            arcTo(1.5f, 1.5f, 0f, false, false, 18f, 16f)
            close()
        }
    }
}

/** 瓦斯图：方形建筑网格 + 门 */
val VastuIcon: ImageVector by lazy {
    divIcon("VastuIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 3f)
            lineTo(20f, 3f)
            lineTo(20f, 21f)
            lineTo(4f, 21f)
            close()
            // 内部网格
            moveTo(9f, 3f)
            lineTo(9f, 21f)
            moveTo(15f, 3f)
            lineTo(15f, 21f)
            moveTo(4f, 9f)
            lineTo(20f, 9f)
            moveTo(4f, 15f)
            lineTo(20f, 15f)
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 门（顶部开口）
            moveTo(10f, 3f)
            lineTo(10f, 8f)
            lineTo(14f, 8f)
            lineTo(14f, 3f)
        }
    }
}

/** 脉轮：七轮纵向排列 + 中柱 */
val ChakraIcon: ImageVector by lazy {
    divIcon("ChakraIcon") {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 中柱
            moveTo(12f, 4f)
            lineTo(12f, 20f)
        }
        path(fill = SolidColor(Color.Black)) {
            // 七个轮（圆）
            moveTo(12f, 4f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 6.2f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 4f)
            close()
            moveTo(12f, 6.8f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 9f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 6.8f)
            close()
            moveTo(12f, 9.6f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 11.8f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 9.6f)
            close()
            moveTo(12f, 12.4f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 14.6f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 12.4f)
            close()
            moveTo(12f, 15.2f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 17.4f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 15.2f)
            close()
            moveTo(12f, 18f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 20.2f)
            arcTo(1.1f, 1.1f, 0f, false, false, 12f, 18f)
            close()
        }
    }
}

/** 普拉萨那：问号 + 星盘 */
val PrasnaIcon: ImageVector by lazy {
    divIcon("PrasnaIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            arcTo(6f, 6f, 0f, false, false, 12f, 16f)
            arcTo(6f, 6f, 0f, false, false, 12f, 4f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 问号
            moveTo(12f, 7f)
            arcTo(2f, 2f, 0f, false, true, 12f, 11f)
            lineTo(12f, 12.5f)
            moveTo(12f, 14.5f)
            arcTo(0.6f, 0.6f, 0f, false, false, 12f, 15.7f)
            arcTo(0.6f, 0.6f, 0f, false, false, 12f, 14.5f)
            close()
        }
    }
}
