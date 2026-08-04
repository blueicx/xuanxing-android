package com.xuanji.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自定义矢量图标：为各地区与占卜体系提供更具辨识度的图形。
 * 地图轮廓为简化示意（离线矢量，随主题色着色），非精确地理边界。
 */

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()
}

/** 亚洲轮廓（含印度、中南半岛、日本，更明显） */
val AsiaIcon: ImageVector by lazy {
    icon("AsiaIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 主体大陆（大而饱满，向 viewBox 四周扩展以更醒目）
            moveTo(5f, 3f)
            lineTo(13f, 2f)
            lineTo(19f, 3.5f)
            lineTo(22.5f, 7f)
            lineTo(23f, 12f)
            lineTo(20f, 16.5f)
            lineTo(15f, 19f)
            lineTo(10f, 18f)
            lineTo(5.5f, 15f)
            lineTo(2.5f, 11f)
            lineTo(2f, 6.5f)
            close()
            // 印度次大陆（下方三角，更突出）
            moveTo(8f, 9f)
            lineTo(13.5f, 8f)
            lineTo(11.5f, 16.5f)
            close()
            // 中南半岛
            moveTo(14f, 10f)
            lineTo(18.5f, 9.5f)
            lineTo(16f, 15f)
            lineTo(14f, 14f)
            close()
            // 日本列岛（右侧，更明显）
            moveTo(20f, 5f)
            lineTo(22f, 6.5f)
            lineTo(21f, 10f)
            lineTo(19.5f, 8f)
            close()
        }
    }
}

/** 非洲轮廓（倒三角，更明显） */
val AfricaIcon: ImageVector by lazy {
    icon("AfricaIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 2f)
            lineTo(15f, 3.5f)
            lineTo(17f, 6f)
            lineTo(19.5f, 9f)
            lineTo(19f, 13f)
            lineTo(15f, 18f)
            lineTo(11f, 22f)
            lineTo(8f, 18f)
            lineTo(5.5f, 15f)
            lineTo(6f, 12f)
            lineTo(4.5f, 9f)
            lineTo(6f, 6f)
            lineTo(8.5f, 3.5f)
            close()
            // 马达加斯加（更明显）
            moveTo(19f, 14f)
            lineTo(21f, 17f)
            lineTo(18.5f, 19f)
            lineTo(17f, 16f)
            close()
        }
    }
}

/** 欧洲轮廓（含半岛，更明显） */
val EuropeIcon: ImageVector by lazy {
    icon("EuropeIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 2f)
            lineTo(16f, 1.5f)
            lineTo(18.5f, 3f)
            lineTo(18f, 6f)
            lineTo(15f, 8.5f)
            lineTo(11f, 8f)
            lineTo(8f, 5.5f)
            close()
            // 斯堪的纳维亚
            moveTo(12f, 1.5f)
            lineTo(14f, 5.5f)
            lineTo(11f, 5f)
            close()
            // 伊比利亚
            moveTo(8.5f, 5.5f)
            lineTo(10f, 9.5f)
            lineTo(7f, 10f)
            close()
            // 意大利
            moveTo(14f, 8f)
            lineTo(15f, 12f)
            lineTo(13f, 11f)
            close()
            // 巴尔干
            moveTo(16f, 6f)
            lineTo(18f, 9f)
            lineTo(16f, 9.5f)
            close()
        }
    }
}

/** 美洲轮廓（南北美洲连体，更明显） */
val AmericaIcon: ImageVector by lazy {
    icon("AmericaIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 北美洲
            moveTo(10f, 1.5f)
            lineTo(15f, 2f)
            lineTo(16.5f, 4.5f)
            lineTo(14f, 7f)
            lineTo(9.5f, 7.5f)
            lineTo(6.5f, 5f)
            lineTo(8f, 2.5f)
            close()
            // 中美地峡
            moveTo(9.5f, 7.5f)
            lineTo(13f, 9.5f)
            lineTo(10f, 11f)
            close()
            // 南美洲
            moveTo(10f, 11f)
            lineTo(14f, 11.5f)
            lineTo(15f, 16f)
            lineTo(11.5f, 21.5f)
            lineTo(7.5f, 17f)
            lineTo(8f, 13f)
            close()
        }
    }
}


/** 东南亚（马来半岛 + 群岛） */
val SoutheastAsiaIcon: ImageVector by lazy {
    icon("SoutheastAsiaIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 中南半岛南端
            moveTo(9f, 2f)
            lineTo(15f, 2f)
            lineTo(14f, 6f)
            lineTo(10f, 6f)
            close()
            // 马来半岛
            moveTo(11.5f, 6f)
            lineTo(13f, 10f)
            lineTo(11f, 10f)
            close()
            // 苏门答腊
            moveTo(9f, 7f)
            lineTo(9.5f, 12f)
            lineTo(8f, 11f)
            close()
            // 爪哇
            moveTo(10f, 12f)
            lineTo(14f, 12f)
            lineTo(13f, 13.5f)
            lineTo(10.5f, 13.5f)
            close()
            // 加里曼丹/苏拉威西等群岛
            moveTo(13f, 8f)
            lineTo(16f, 8.5f)
            lineTo(15f, 10f)
            close()
            moveTo(14f, 10.5f)
            lineTo(16.5f, 12f)
            lineTo(15f, 13f)
            close()
        }
    }
}

/** 南亚（印度次大陆） */
val SouthAsiaIcon: ImageVector by lazy {
    icon("SouthAsiaIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 2f)
            lineTo(14f, 2f)
            lineTo(16f, 5f)
            lineTo(14.5f, 9f)
            lineTo(12f, 13f)
            lineTo(9f, 9f)
            lineTo(7f, 5f)
            close()
            // 斯里兰卡
            moveTo(11.5f, 14f)
            lineTo(12.5f, 16f)
            lineTo(11f, 16f)
            close()
        }
    }
}

/** 中东（阿拉伯半岛） */
val MiddleEastIcon: ImageVector by lazy {
    icon("MiddleEastIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 阿拉伯半岛
            moveTo(6f, 3f)
            lineTo(13f, 2f)
            lineTo(17f, 4f)
            lineTo(16f, 7f)
            lineTo(13f, 9f)
            lineTo(9f, 8f)
            lineTo(6f, 6f)
            close()
            // 半岛南端（阿拉伯海侧）
            moveTo(9f, 8f)
            lineTo(13f, 9f)
            lineTo(12f, 12f)
            lineTo(9.5f, 11f)
            close()
            // 安纳托利亚（小亚细亚）
            moveTo(13f, 2f)
            lineTo(16f, 2.5f)
            lineTo(15f, 4f)
            lineTo(13f, 3.5f)
            close()
        }
    }
}

/** 中国轮廓（鸡形简化） */
val ChinaIcon: ImageVector by lazy {
    icon("ChinaIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 中国主体轮廓（简化）
            moveTo(9f, 2.5f)
            lineTo(14f, 2f)
            lineTo(18f, 3f)
            lineTo(20.5f, 5f)
            lineTo(20f, 8f)
            lineTo(17f, 10f)
            lineTo(15.5f, 8f)
            lineTo(13.5f, 9f)
            lineTo(15f, 12f)
            lineTo(13f, 14f)
            lineTo(10f, 13f)
            lineTo(8f, 11f)
            lineTo(6.5f, 9f)
            lineTo(6f, 6f)
            lineTo(7.5f, 4f)
            close()
            // 辽东半岛
            moveTo(15f, 4.5f)
            lineTo(16.5f, 6f)
            lineTo(15f, 6.5f)
            close()
            // 雷州半岛（南）
            moveTo(9.5f, 12f)
            lineTo(10f, 14f)
            lineTo(9f, 13.5f)
            close()
            // 海南岛
            moveTo(11f, 14.5f)
            lineTo(12.5f, 15f)
            lineTo(11.5f, 16f)
            close()
        }
    }
}

/** 日本轮廓（列岛） */
val JapanIcon: ImageVector by lazy {
    icon("JapanIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 北海道
            moveTo(14f, 1.5f)
            lineTo(17f, 2.5f)
            lineTo(16f, 4f)
            lineTo(14f, 3.5f)
            close()
            // 本州
            moveTo(12.5f, 4f)
            lineTo(18f, 4.5f)
            lineTo(16f, 8f)
            lineTo(12f, 7f)
            close()
            // 四国
            moveTo(13f, 8f)
            lineTo(15f, 8.5f)
            lineTo(14f, 9.5f)
            lineTo(12.5f, 9f)
            close()
            // 九州
            moveTo(11.5f, 8f)
            lineTo(12.5f, 11f)
            lineTo(10.5f, 10f)
            close()
        }
    }
}

/** 塔罗牌 */
val TarotIcon: ImageVector by lazy {
    icon("TarotIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 3f)
            lineTo(17f, 3f)
            lineTo(20f, 6f)
            lineTo(20f, 21f)
            lineTo(7f, 21f)
            lineTo(4f, 18f)
            close()
            moveTo(4f, 3f)
            lineTo(4f, 18f)
            lineTo(7f, 21f)
            lineTo(20f, 21f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 9f)
            lineTo(12.8f, 11.2f)
            lineTo(15f, 11.4f)
            lineTo(13.3f, 13f)
            lineTo(13.9f, 15.2f)
            lineTo(12f, 14f)
            lineTo(10.1f, 15.2f)
            lineTo(10.7f, 13f)
            lineTo(9f, 11.4f)
            lineTo(11.2f, 11.2f)
            close()
        }
    }
}

/** 贝壳 */
val ShellIcon: ImageVector by lazy {
    icon("ShellIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 20f)
            lineTo(5f, 9f)
            arcTo(9f, 9f, 0f, false, false, 12f, 4f)
            arcTo(9f, 9f, 0f, false, false, 19f, 9f)
            close()
            moveTo(9f, 9f)
            lineTo(12f, 17f)
            moveTo(15f, 9f)
            lineTo(12f, 17f)
            moveTo(8f, 7f)
            lineTo(12f, 13f)
            moveTo(16f, 7f)
            lineTo(12f, 13f)
        }
    }
}

/** 水晶球 */
val CrystalBallIcon: ImageVector by lazy {
    icon("CrystalBallIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 14f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 3f)
            close()
            moveTo(7f, 15f)
            lineTo(17f, 15f)
            lineTo(18f, 21f)
            lineTo(6f, 21f)
            close()
        }
    }
}

/** 灵签（竹签筒） */
val LotteryStickIcon: ImageVector by lazy {
    icon("LotteryStickIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10f, 2f)
            lineTo(14f, 2f)
            lineTo(14f, 18f)
            lineTo(10f, 18f)
            close()
            moveTo(5f, 16f)
            arcTo(7f, 7f, 0f, false, false, 19f, 16f)
            lineTo(19f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }
}

/** 星象（用于星座/占星体系） */
val AstroStarIcon: ImageVector by lazy {
    icon("AstroStarIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(13.5f, 9.5f)
            lineTo(20f, 10f)
            lineTo(15f, 14f)
            lineTo(17f, 20f)
            lineTo(12f, 16f)
            lineTo(7f, 20f)
            lineTo(9f, 14f)
            lineTo(4f, 10f)
            lineTo(10.5f, 9.5f)
            close()
        }
    }
}

/** 东方/五行（八卦双鱼抽象） */
val DivinationWheelIcon: ImageVector by lazy {
    icon("DivinationWheelIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, false, false, 12f, 22f)
            arcTo(10f, 10f, 0f, false, false, 12f, 2f)
            close()
            moveTo(12f, 6f)
            arcTo(6f, 6f, 0f, false, false, 12f, 18f)
            arcTo(6f, 6f, 0f, false, false, 12f, 6f)
            close()
        }
    }
}

/** 地区 → 主题色：让每个板块一眼可区分 */
val REGION_COLORS: Map<String, Color> = mapOf(
    "asia" to Color(0xFFB23A48),      // 亚洲·红金
    "africa" to Color(0xFFE07A3F),    // 非洲·橙
    "europe" to Color(0xFF3A6FB0),    // 欧洲·蓝
    "america" to Color(0xFF3E7C59),   // 美洲·绿
    "oceania" to Color(0xFF2F9E8F),   // 大洋洲·青
    "modern" to Color(0xFF7B4FA6),    // 近现代新兴·紫
    "common" to Color(0xFFC77E23)     // 常用占卜·琥珀
)

/** 近现代新兴板块：星点汇聚 + 能量放射 */
val ModernIcon: ImageVector by lazy {
    icon("ModernIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 放射的星光 + 环绕能量
            moveTo(12f, 3f)
            lineTo(13.5f, 10.5f)
            lineTo(21f, 12f)
            lineTo(13.5f, 13.5f)
            lineTo(12f, 21f)
            lineTo(10.5f, 13.5f)
            lineTo(3f, 12f)
            lineTo(10.5f, 10.5f)
            close()
            // 外圈环绕
            moveTo(12f, 5f)
            arcTo(7f, 7f, 0f, false, false, 12f, 19f)
            arcTo(7f, 7f, 0f, false, false, 12f, 5f)
            close()
        }
    }
}

/** 常用占卜板块：三枚签 + 骰子点 */
val CommonDivinationIcon: ImageVector by lazy {
    icon("CommonDivinationIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 三枚倾斜的签
            moveTo(6f, 2f)
            lineTo(8f, 2f)
            lineTo(5f, 22f)
            lineTo(3f, 22f)
            close()
            moveTo(10f, 2f)
            lineTo(12f, 2f)
            lineTo(9f, 22f)
            lineTo(7f, 22f)
            close()
            moveTo(14f, 2f)
            lineTo(16f, 2f)
            lineTo(13f, 22f)
            lineTo(11f, 22f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 骰子点
            moveTo(18f, 16f)
            arcTo(2.5f, 2.5f, 0f, false, false, 18f, 21f)
            arcTo(2.5f, 2.5f, 0f, false, false, 18f, 16f)
            close()
        }
    }
}

/**
 * 彩色圆底 + 白色地区轮廓图标：每个板块用专属主题色，一眼可区分。
 * @param icon    地区/分区图标（如 AsiaIcon）
 * @param color   该板块主题色（来自 REGION_COLORS）
 * @param sizeDp  圆底直径（默认 40dp）
 */
@Composable
fun RegionBadge(
    icon: ImageVector,
    color: Color,
    sizeDp: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(color = color.copy(alpha = 0.22f), radius = size.minDimension / 2f, center = center)
            drawCircle(color = color, radius = size.minDimension * 0.34f, center = center)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(sizeDp * 0.56f)
        )
    }
}
