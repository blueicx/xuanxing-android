package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 中国占卜体系专属图标：每个体系一个贴合主题的符号。
 * 单色轮廓，随主题色着色，小尺寸下清晰可辨。
 */
private fun divIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 紫微斗数：星盘 + 星曜（圆盘内三星点 + 宫位分割） */
val ZiweiIcon: ImageVector by lazy {
    divIcon("ZiweiIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, false, false, 12f, 22f)
            arcTo(10f, 10f, 0f, false, false, 12f, 2f)
            close()
            moveTo(12f, 5.5f)
            arcTo(6.5f, 6.5f, 0f, false, false, 12f, 18.5f)
            arcTo(6.5f, 6.5f, 0f, false, false, 12f, 5.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 9.5f)
            arcTo(0.9f, 0.9f, 0f, false, false, 12f, 11.3f)
            arcTo(0.9f, 0.9f, 0f, false, false, 12f, 9.5f)
            close()
            moveTo(8f, 13.5f)
            arcTo(0.8f, 0.8f, 0f, false, false, 8f, 15.1f)
            arcTo(0.8f, 0.8f, 0f, false, false, 8f, 13.5f)
            close()
            moveTo(16f, 13.5f)
            arcTo(0.8f, 0.8f, 0f, false, false, 16f, 15.1f)
            arcTo(0.8f, 0.8f, 0f, false, false, 16f, 13.5f)
            close()
        }
    }
}

/** 奇门遁甲：九宫格 + 中央太极 */
val QiMenIcon: ImageVector by lazy {
    divIcon("QiMenIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 21f)
            lineTo(3f, 21f)
            close()
            // 九宫分割线
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
            // 中央阴阳鱼简化为小圆
            moveTo(12f, 10.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 13.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 12f, 10.5f)
            close()
        }
    }
}

/** 风水：罗盘圆盘 + 十字方位 + 八卦外圈 */
val FengShuiIcon: ImageVector by lazy {
    divIcon("FengShuiIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
            moveTo(12f, 6.5f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 17.5f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 6.5f)
            close()
            // 十字方位
            moveTo(12f, 4f)
            lineTo(12f, 20f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
    }
}

/** 六爻：三枚铜钱（三个圆）+ 爻线 */
val LiuYaoIcon: ImageVector by lazy {
    divIcon("LiuYaoIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 5f)
            arcTo(2f, 2f, 0f, false, false, 6f, 9f)
            arcTo(2f, 2f, 0f, false, false, 6f, 5f)
            close()
            moveTo(12f, 3f)
            arcTo(2f, 2f, 0f, false, false, 12f, 7f)
            arcTo(2f, 2f, 0f, false, false, 12f, 3f)
            close()
            moveTo(18f, 5f)
            arcTo(2f, 2f, 0f, false, false, 18f, 9f)
            arcTo(2f, 2f, 0f, false, false, 18f, 5f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 爻象（阳爻/阴爻）
            moveTo(7f, 14f)
            lineTo(17f, 14f)
            moveTo(7f, 18f)
            lineTo(11f, 18f)
            moveTo(13f, 18f)
            lineTo(17f, 18f)
        }
    }
}

/** 易经六爻占：六条爻线（卦象） */
val IChingIcon: ImageVector by lazy {
    divIcon("IChingIcon") {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(5f, 4f)
            lineTo(19f, 4f)
            moveTo(5f, 7.5f)
            lineTo(19f, 7.5f)
            moveTo(5f, 11f)
            lineTo(19f, 11f)
            moveTo(5f, 14.5f)
            lineTo(19f, 14.5f)
            // 中间一条阴爻（断开）
            moveTo(5f, 18f)
            lineTo(10f, 18f)
            moveTo(14f, 18f)
            lineTo(19f, 18f)
        }
    }
}

/** 相术：人脸轮廓 + 五官分区 */
val PhysiognomyIcon: ImageVector by lazy {
    divIcon("PhysiognomyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 侧脸轮廓
            moveTo(6f, 10f)
            arcTo(6f, 6f, 0f, false, true, 18f, 10f)
            lineTo(18f, 18f)
            lineTo(6f, 18f)
            close()
            // 眼
            moveTo(12f, 8f)
            arcTo(1.2f, 1.2f, 0f, false, false, 12f, 10.4f)
            arcTo(1.2f, 1.2f, 0f, false, false, 12f, 8f)
            close()
            // 嘴
            moveTo(11f, 13.5f)
            lineTo(13f, 13.5f)
        }
    }
}

/** 姓名学：印章 + 笔划 */
val NameologyIcon: ImageVector by lazy {
    divIcon("NameologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 4f)
            lineTo(18f, 4f)
            lineTo(18f, 20f)
            lineTo(6f, 20f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 印章内笔划（横竖交错的"名"字简化）
            moveTo(9f, 8f)
            lineTo(15f, 8f)
            moveTo(12f, 6f)
            lineTo(12f, 14f)
            moveTo(8f, 12f)
            lineTo(16f, 12f)
            moveTo(10f, 16f)
            lineTo(14f, 16f)
        }
    }
}

/** 太乙神数：八角星 + 中心 */
val TaiYiIcon: ImageVector by lazy {
    divIcon("TaiYiIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(15f, 9f)
            lineTo(21f, 9f)
            lineTo(16f, 13f)
            lineTo(18f, 19f)
            lineTo(12f, 15.5f)
            lineTo(6f, 19f)
            lineTo(8f, 13f)
            lineTo(3f, 9f)
            lineTo(9f, 9f)
            close()
            // 中心点
            moveTo(12f, 11.5f)
            arcTo(1f, 1f, 0f, false, false, 12f, 13.5f)
            arcTo(1f, 1f, 0f, false, false, 12f, 11.5f)
            close()
        }
    }
}

/** 大六壬：天盘地盘（内外两圆）+ 箭头 */
val LiuRenIcon: ImageVector by lazy {
    divIcon("LiuRenIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            arcTo(8f, 8f, 0f, false, false, 12f, 20f)
            arcTo(8f, 8f, 0f, false, false, 12f, 4f)
            close()
            moveTo(12f, 8f)
            arcTo(4f, 4f, 0f, false, false, 12f, 16f)
            arcTo(4f, 4f, 0f, false, false, 12f, 8f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 三传箭头
            moveTo(16f, 4f)
            lineTo(19f, 4f)
            lineTo(19f, 7f)
        }
    }
}

/** 梅花易数：五瓣梅花 + 中心卦 */
val MeiHuaIcon: ImageVector by lazy {
    divIcon("MeiHuaIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            arcTo(3f, 3f, 0f, false, true, 15f, 8f)
            arcTo(3f, 3f, 0f, false, true, 12f, 11f)
            arcTo(3f, 3f, 0f, false, true, 12f, 4f)
            close()
            moveTo(6f, 8f)
            arcTo(3f, 3f, 0f, false, true, 9f, 12f)
            arcTo(3f, 3f, 0f, false, true, 6f, 8f)
            close()
            moveTo(18f, 8f)
            arcTo(3f, 3f, 0f, false, true, 15f, 12f)
            arcTo(3f, 3f, 0f, false, true, 18f, 8f)
            close()
            moveTo(9f, 16f)
            arcTo(3f, 3f, 0f, false, true, 12f, 20f)
            arcTo(3f, 3f, 0f, false, true, 9f, 16f)
            close()
            moveTo(15f, 16f)
            arcTo(3f, 3f, 0f, false, true, 12f, 20f)
            arcTo(3f, 3f, 0f, false, true, 15f, 16f)
            close()
        }
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            // 中心卦（一阳一阴）
            moveTo(10.5f, 12.5f)
            lineTo(13.5f, 12.5f)
            moveTo(11f, 15f)
            lineTo(13f, 15f)
        }
    }
}

/** 七政四余：太阳 + 行星轨道 */
val QiZhengIcon: ImageVector by lazy {
    divIcon("QiZhengIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 6f)
            arcTo(2f, 2f, 0f, false, false, 12f, 10f)
            arcTo(2f, 2f, 0f, false, false, 12f, 6f)
            close()
            moveTo(12f, 3.5f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 14.5f)
            arcTo(5.5f, 5.5f, 0f, false, false, 12f, 3.5f)
            close()
            moveTo(12f, 1.5f)
            arcTo(8f, 8f, 0f, false, false, 12f, 17.5f)
            arcTo(8f, 8f, 0f, false, false, 12f, 1.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(3.5f, 9f)
            arcTo(0.8f, 0.8f, 0f, false, false, 3.5f, 10.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 3.5f, 9f)
            close()
            moveTo(20.5f, 9f)
            arcTo(0.8f, 0.8f, 0f, false, false, 20.5f, 10.6f)
            arcTo(0.8f, 0.8f, 0f, false, false, 20.5f, 9f)
            close()
        }
    }
}

/** 二十八宿：星宿连线（星点 + 连线成星座） */
val ErshibaIcon: ImageVector by lazy {
    divIcon("ErshibaIcon") {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(4f, 5f)
            lineTo(9f, 8f)
            lineTo(8f, 12f)
            lineTo(12f, 15f)
            lineTo(16f, 12f)
            lineTo(20f, 14f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 5f)
            arcTo(0.9f, 0.9f, 0f, false, false, 4f, 6.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 4f, 5f)
            close()
            moveTo(9f, 8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 9f, 9.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 9f, 8f)
            close()
            moveTo(8f, 12f)
            arcTo(0.9f, 0.9f, 0f, false, false, 8f, 13.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 8f, 12f)
            close()
            moveTo(12f, 15f)
            arcTo(0.9f, 0.9f, 0f, false, false, 12f, 16.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 12f, 15f)
            close()
            moveTo(16f, 12f)
            arcTo(0.9f, 0.9f, 0f, false, false, 16f, 13.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 16f, 12f)
            close()
            moveTo(20f, 14f)
            arcTo(0.9f, 0.9f, 0f, false, false, 20f, 15.8f)
            arcTo(0.9f, 0.9f, 0f, false, false, 20f, 14f)
            close()
        }
    }
}

/** 西藏占星：日月组合（藏式转经筒 / 日月） */
val TibetAstrologyIcon: ImageVector by lazy {
    divIcon("TibetAstrologyIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 4f)
            arcTo(4.5f, 4.5f, 0f, false, false, 7f, 13f)
            arcTo(4.5f, 4.5f, 0f, false, false, 7f, 4f)
            close()
            // 月牙
            moveTo(17f, 4f)
            arcTo(4.5f, 4.5f, 0f, false, false, 17f, 13f)
            arcTo(4.5f, 4.5f, 0f, false, false, 17f, 4f)
            close()
            moveTo(17f, 6f)
            arcTo(3f, 3f, 0f, false, true, 17f, 11f)
            arcTo(3f, 3f, 0f, false, true, 17f, 6f)
            close()
            // 底柱
            moveTo(9f, 14f)
            lineTo(15f, 14f)
            lineTo(15f, 18f)
            lineTo(9f, 18f)
            close()
        }
    }
}
