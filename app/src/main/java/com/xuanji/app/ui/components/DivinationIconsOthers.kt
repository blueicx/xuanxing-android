package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 各地区占卜体系专属图标（美洲 + 近现代新兴 + 常用占卜）。
 * 单色轮廓，随主题色着色，风格与 CustomIcons.kt 中的 TarotIcon 一致。
 */

private fun divIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 玛雅占星：圆形历法轮（中央脸 + 环绕刻度圈） */
val MayaTzolkinIcon: ImageVector by lazy {
    divIcon("MayaTzolkinIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 外历法轮（圆环）
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            // 内圈（反向绕行形成镂空）
            moveTo(12f, 4.5f)
            arcTo(7.5f, 7.5f, 0f, false, false, 12f, 19.5f)
            arcTo(7.5f, 7.5f, 0f, false, false, 12f, 4.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中央脸：双眼
            moveTo(8.6f, 10f); lineTo(10.2f, 10.8f); lineTo(8.6f, 11.6f); close()
            moveTo(15.4f, 10f); lineTo(13.8f, 10.8f); lineTo(15.4f, 11.6f); close()
            // 鼻梁
            moveTo(12f, 10.8f); lineTo(12.9f, 11.8f); lineTo(11.1f, 11.8f); close()
            // 嘴
            moveTo(9.8f, 14f); lineTo(14.2f, 14f); lineTo(12f, 15.8f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 环绕刻度（8 个楔形）
            moveTo(12f, 4.5f); lineTo(11.3f, 1.8f); lineTo(12.7f, 1.8f); close()
            moveTo(12f, 19.5f); lineTo(11.3f, 22.2f); lineTo(12.7f, 22.2f); close()
            moveTo(4.5f, 12f); lineTo(1.8f, 11.3f); lineTo(1.8f, 12.7f); close()
            moveTo(19.5f, 12f); lineTo(22.2f, 11.3f); lineTo(22.2f, 12.7f); close()
            // 对角刻度
            moveTo(16.4f, 7.6f); lineTo(18.7f, 5.3f); lineTo(18.1f, 4.7f); close()
            moveTo(7.6f, 16.4f); lineTo(5.3f, 18.7f); lineTo(4.7f, 18.1f); close()
            moveTo(7.6f, 7.6f); lineTo(4.7f, 5.3f); lineTo(5.3f, 4.7f); close()
            moveTo(16.4f, 16.4f); lineTo(18.1f, 18.7f); lineTo(18.7f, 18.1f); close()
        }
    }
}

/** 玛雅星系印记：星系图腾 + 20 刻轮（图腾符号 + 圆轮） */
val MayaGalacticIcon: ImageVector by lazy {
    divIcon("MayaGalacticIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 刻轮（圆环）
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            moveTo(12f, 3.5f)
            arcTo(8.5f, 8.5f, 0f, false, false, 12f, 20.5f)
            arcTo(8.5f, 8.5f, 0f, false, false, 12f, 3.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 轮上四向刻度
            moveTo(12f, 3.5f); lineTo(11.4f, 1.6f); lineTo(12.6f, 1.6f); close()
            moveTo(12f, 20.5f); lineTo(11.4f, 22.4f); lineTo(12.6f, 22.4f); close()
            moveTo(3.5f, 12f); lineTo(1.6f, 11.4f); lineTo(1.6f, 12.6f); close()
            moveTo(20.5f, 12f); lineTo(22.4f, 11.4f); lineTo(22.4f, 12.6f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中心图腾（菱形 + 横线标记）
            moveTo(12f, 6.5f); lineTo(15f, 10.5f); lineTo(12f, 14.5f); lineTo(9f, 10.5f); close()
            moveTo(6.2f, 10.5f); lineTo(17.8f, 10.5f); lineTo(17.8f, 11f); lineTo(6.2f, 11f); close()
            // 图腾两侧小圆点
            moveTo(6f, 14f); arcTo(0.8f, 0.8f, 0f, false, true, 6f, 15.6f); arcTo(0.8f, 0.8f, 0f, false, true, 6f, 14f); close()
            moveTo(18f, 14f); arcTo(0.8f, 0.8f, 0f, false, true, 18f, 15.6f); arcTo(0.8f, 0.8f, 0f, false, true, 18f, 14f); close()
        }
    }
}

/** 阿兹特克占星：太阳石轮盘（放射光线 + 中央日神脸） */
val AztecIcon: ImageVector by lazy {
    divIcon("AztecIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 太阳石圆盘
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            moveTo(12f, 5f)
            arcTo(7f, 7f, 0f, false, false, 12f, 19f)
            arcTo(7f, 7f, 0f, false, false, 12f, 5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 放射光线（8 个楔形）
            moveTo(12f, 5f); lineTo(11.2f, 1.8f); lineTo(12.8f, 1.8f); close()
            moveTo(12f, 19f); lineTo(11.2f, 22.2f); lineTo(12.8f, 22.2f); close()
            moveTo(5f, 12f); lineTo(1.8f, 11.2f); lineTo(1.8f, 12.8f); close()
            moveTo(19f, 12f); lineTo(22.2f, 11.2f); lineTo(22.2f, 12.8f); close()
            moveTo(16.9f, 7.1f); lineTo(19.3f, 4.7f); lineTo(18.7f, 4.1f); close()
            moveTo(7.1f, 16.9f); lineTo(4.7f, 19.3f); lineTo(4.1f, 18.7f); close()
            moveTo(7.1f, 7.1f); lineTo(4.1f, 4.7f); lineTo(4.7f, 4.1f); close()
            moveTo(16.9f, 16.9f); lineTo(18.7f, 19.3f); lineTo(19.3f, 18.7f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中央日神脸：双眼
            moveTo(8.6f, 9.8f); lineTo(10.2f, 10.6f); lineTo(8.6f, 11.4f); close()
            moveTo(15.4f, 9.8f); lineTo(13.8f, 10.6f); lineTo(15.4f, 11.4f); close()
            // 方形口（阿兹特克特征，中间留齿隙）
            moveTo(9.4f, 13.2f); lineTo(11.6f, 13.2f); lineTo(11.6f, 15.2f); lineTo(9.4f, 15.2f); close()
            moveTo(12.4f, 13.2f); lineTo(14.6f, 13.2f); lineTo(14.6f, 15.2f); lineTo(12.4f, 15.2f); close()
        }
    }
}

/** 北美药轮：圆 + 十字方向 + 内圈 + 中心点 */
val MedicineWheelIcon: ImageVector by lazy {
    divIcon("MedicineWheelIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 药轮外环
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            moveTo(12f, 6f)
            arcTo(6f, 6f, 0f, false, false, 12f, 18f)
            arcTo(6f, 6f, 0f, false, false, 12f, 6f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 十字方向（四方位）
            moveTo(11.6f, 3f); lineTo(12.4f, 3f); lineTo(12.4f, 21f); lineTo(11.6f, 21f); close()
            moveTo(3f, 11.6f); lineTo(21f, 11.6f); lineTo(21f, 12.4f); lineTo(3f, 12.4f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中心圆点
            moveTo(12f, 10.2f); arcTo(1.8f, 1.8f, 0f, false, true, 12f, 13.8f); arcTo(1.8f, 1.8f, 0f, false, true, 12f, 10.2f); close()
        }
    }
}

/** 人类图：菱形人体 + 九个能量中心节点连线 */
val HumanDesignIcon: ImageVector by lazy {
    divIcon("HumanDesignIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 头部（空心菱形）
            moveTo(12f, 1.6f); lineTo(15f, 4.8f); lineTo(12f, 8f); lineTo(9f, 4.8f); close()
            moveTo(12f, 2.6f); lineTo(10f, 4.8f); lineTo(12f, 7f); lineTo(14f, 4.8f); close()
            // 身体（空心轮廓）
            moveTo(12f, 8.8f); lineTo(16.5f, 11.5f); lineTo(14.5f, 20.5f); lineTo(9.5f, 20.5f); lineTo(7.5f, 11.5f); close()
            moveTo(12f, 9.8f); lineTo(8.4f, 12f); lineTo(10.1f, 19.7f); lineTo(13.9f, 19.7f); lineTo(15.6f, 12f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中心能量线
            moveTo(11.7f, 8f); lineTo(12.3f, 8f); lineTo(12.3f, 18f); lineTo(11.7f, 18f); close()
            // 左右连接线
            moveTo(7.9f, 12.7f); lineTo(16.1f, 12.7f); lineTo(16.1f, 13.3f); lineTo(7.9f, 13.3f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 九个能量中心节点（实心小圆）
            moveTo(12f, 2.6f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 4f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 2.6f); close()
            moveTo(12f, 5.2f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 6.6f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 5.2f); close()
            moveTo(12f, 9.5f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 10.9f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 9.5f); close()
            moveTo(12f, 11.9f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 13.3f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 11.9f); close()
            moveTo(8.8f, 13.3f); arcTo(0.7f, 0.7f, 0f, false, true, 8.8f, 14.7f); arcTo(0.7f, 0.7f, 0f, false, true, 8.8f, 13.3f); close()
            moveTo(15.2f, 13.3f); arcTo(0.7f, 0.7f, 0f, false, true, 15.2f, 14.7f); arcTo(0.7f, 0.7f, 0f, false, true, 15.2f, 13.3f); close()
            moveTo(12f, 14.9f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 16.3f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 14.9f); close()
            moveTo(12f, 17.5f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 18.9f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 17.5f); close()
            moveTo(12f, 20f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 21.4f); arcTo(0.7f, 0.7f, 0f, false, true, 12f, 20f); close()
        }
    }
}

/** 吸引力法则：磁吸的无限符号（∞） + 能量星点 */
val LawOfAttractionIcon: ImageVector by lazy {
    divIcon("LawOfAttractionIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 无限符号（两个相切圆环，随引力吸引）
            moveTo(12f, 12f)
            arcTo(3.5f, 3.5f, 0f, false, false, 5f, 12f)
            arcTo(3.5f, 3.5f, 0f, false, true, 12f, 12f)
            arcTo(3.5f, 3.5f, 0f, false, false, 19f, 12f)
            arcTo(3.5f, 3.5f, 0f, false, true, 12f, 12f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 能量星点（菱形闪光）
            moveTo(3f, 4f); lineTo(4.6f, 5.6f); lineTo(3f, 7.2f); lineTo(1.4f, 5.6f); close()
            moveTo(20.4f, 16f); lineTo(21.8f, 17.4f); lineTo(20.4f, 18.8f); lineTo(19f, 17.4f); close()
            moveTo(5.6f, 18.8f); lineTo(6.6f, 17.8f); lineTo(7.6f, 18.8f); lineTo(6.6f, 19.8f); close()
        }
    }
}

/** 13 月亮历：月相新月 + 环形轨道（十三月相之轮） */
val ThirteenMoonIcon: ImageVector by lazy {
    divIcon("ThirteenMoonIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 新月（外圆减去错位内圆）
            moveTo(10f, 5f)
            arcTo(7f, 7f, 0f, false, true, 10f, 19f)
            arcTo(7f, 7f, 0f, false, true, 10f, 5f)
            close()
            moveTo(13.4f, 5.8f)
            arcTo(6.2f, 6.2f, 0f, false, false, 13.4f, 18.2f)
            arcTo(6.2f, 6.2f, 0f, false, false, 13.4f, 5.8f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 环形轨道
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 12f, 21f)
            arcTo(9f, 9f, 0f, false, false, 12f, 3f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 月相小点（十三月亮历的月份标记）
            moveTo(5.5f, 16f); arcTo(0.9f, 0.9f, 0f, false, true, 5.5f, 17.8f); arcTo(0.9f, 0.9f, 0f, false, true, 5.5f, 16f); close()
            moveTo(8f, 19.5f); arcTo(0.7f, 0.7f, 0f, false, true, 8f, 20.9f); arcTo(0.7f, 0.7f, 0f, false, true, 8f, 19.5f); close()
            moveTo(3.5f, 12.5f); arcTo(0.6f, 0.6f, 0f, false, true, 3.5f, 13.7f); arcTo(0.6f, 0.6f, 0f, false, true, 3.5f, 12.5f); close()
            moveTo(18.5f, 18f); arcTo(0.8f, 0.8f, 0f, false, true, 18.5f, 19.6f); arcTo(0.8f, 0.8f, 0f, false, true, 18.5f, 18f); close()
            moveTo(19.8f, 8f); arcTo(0.7f, 0.7f, 0f, false, true, 19.8f, 9.4f); arcTo(0.7f, 0.7f, 0f, false, true, 19.8f, 8f); close()
        }
    }
}

/** 今日算命：日历页 + 抽出日签（日签纸 + 日期格） */
val TodayOracleIcon: ImageVector by lazy {
    divIcon("TodayOracleIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 日历页
            moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 20f); lineTo(5f, 20f); close()
            // 装订环
            moveTo(8f, 3.4f); arcTo(0.9f, 0.9f, 0f, false, true, 8f, 5.2f); arcTo(0.9f, 0.9f, 0f, false, true, 8f, 3.4f); close()
            moveTo(16f, 3.4f); arcTo(0.9f, 0.9f, 0f, false, true, 16f, 5.2f); arcTo(0.9f, 0.9f, 0f, false, true, 16f, 3.4f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 抽出的日签（斜伸向页外）
            moveTo(3.6f, 19f); lineTo(9.4f, 12.4f); lineTo(10.2f, 13f); lineTo(4.4f, 19.6f); close()
            moveTo(15f, 6f); lineTo(17.8f, 2.8f); lineTo(18.5f, 3.5f); lineTo(15.7f, 6.7f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 今日标记点 + 日期格
            moveTo(16.6f, 9.4f); arcTo(0.9f, 0.9f, 0f, false, true, 16.6f, 11.2f); arcTo(0.9f, 0.9f, 0f, false, true, 16.6f, 9.4f); close()
            moveTo(10.6f, 14.6f); lineTo(13.4f, 14.6f); lineTo(13.4f, 16.6f); lineTo(10.6f, 16.6f); close()
        }
    }
}

/** 藏传签卜：藏式法轮（八辐轮 + 中央毂） */
val TibetanDivIcon: ImageVector by lazy {
    divIcon("TibetanDivIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 法轮外环
            moveTo(12f, 1.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 22.5f)
            arcTo(10.5f, 10.5f, 0f, false, true, 12f, 1.5f)
            close()
            moveTo(12f, 3.5f)
            arcTo(8.5f, 8.5f, 0f, false, false, 12f, 20.5f)
            arcTo(8.5f, 8.5f, 0f, false, false, 12f, 3.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 八根辐条
            moveTo(11.7f, 10.2f); lineTo(12.3f, 10.2f); lineTo(12.3f, 3.5f); lineTo(11.7f, 3.5f); close()
            moveTo(11.7f, 13.8f); lineTo(12.3f, 13.8f); lineTo(12.3f, 20.5f); lineTo(11.7f, 20.5f); close()
            moveTo(10.2f, 11.7f); lineTo(10.2f, 12.3f); lineTo(3.5f, 12.3f); lineTo(3.5f, 11.7f); close()
            moveTo(13.8f, 11.7f); lineTo(13.8f, 12.3f); lineTo(20.5f, 12.3f); lineTo(20.5f, 11.7f); close()
            // 对角辐条
            moveTo(13.4f, 10.6f); lineTo(18f, 6f); lineTo(18.5f, 6.5f); lineTo(13.9f, 11.1f); close()
            moveTo(10.6f, 13.4f); lineTo(6f, 18f); lineTo(6.5f, 18.5f); lineTo(11.1f, 13.9f); close()
            moveTo(10.6f, 10.6f); lineTo(6f, 6f); lineTo(6.5f, 5.5f); lineTo(11.1f, 11.1f); close()
            moveTo(13.4f, 13.4f); lineTo(18f, 18f); lineTo(18.5f, 17.5f); lineTo(13.9f, 12.9f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中央毂
            moveTo(12f, 10.4f); arcTo(1.6f, 1.6f, 0f, false, true, 12f, 13.6f); arcTo(1.6f, 1.6f, 0f, false, true, 12f, 10.4f); close()
        }
    }
}

/** 泰国暹罗签：泰国尖顶佛塔（层叠塔身 + 塔尖宝珠） */
val ThaiSiamIcon: ImageVector by lazy {
    divIcon("ThaiSiamIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 基座与塔身层级
            moveTo(6.5f, 20.5f); lineTo(17.5f, 20.5f); lineTo(15.5f, 17f); lineTo(8.5f, 17f); close()
            moveTo(8f, 17f); lineTo(16f, 17f); lineTo(14.5f, 13.5f); lineTo(9.5f, 13.5f); close()
            moveTo(9.2f, 13.5f); lineTo(14.8f, 13.5f); lineTo(13.5f, 10.5f); lineTo(10.5f, 10.5f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 钟形塔身（穹顶）
            moveTo(10.5f, 10.5f)
            arcTo(1.5f, 3.2f, 0f, false, false, 13.5f, 10.5f)
            lineTo(10.5f, 10.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 塔尖
            moveTo(11.8f, 8.5f); lineTo(12.2f, 8.5f); lineTo(12.2f, 3.5f); lineTo(12f, 1.5f); lineTo(11.8f, 3.5f); close()
            // 塔顶宝珠
            moveTo(12f, 3.3f); arcTo(1f, 1f, 0f, false, true, 12f, 5.3f); arcTo(1f, 1f, 0f, false, true, 12f, 3.3f); close()
        }
    }
}

/** 古希腊神谕：德尔斐三脚架（鼎盆 + 三足 + 神谕火焰） */
val GreekOracleIcon: ImageVector by lazy {
    divIcon("GreekOracleIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 鼎盆（碗）与盆沿
            moveTo(7.5f, 7.5f); lineTo(16.5f, 7.5f); lineTo(15.3f, 11f); lineTo(8.7f, 11f); close()
            moveTo(7f, 7.5f); lineTo(17f, 7.5f); lineTo(17f, 8f); lineTo(7f, 8f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 三足
            moveTo(11.6f, 11f); lineTo(12.4f, 11f); lineTo(12.4f, 20f); lineTo(11.6f, 20f); close()
            moveTo(10.1f, 11f); lineTo(10.6f, 11.2f); lineTo(7.2f, 20f); lineTo(6.7f, 19.7f); close()
            moveTo(13.9f, 11f); lineTo(13.4f, 11.2f); lineTo(16.8f, 20f); lineTo(17.3f, 19.7f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 神谕火焰 + 烟气
            moveTo(11f, 5.5f); lineTo(13f, 5.5f); lineTo(12f, 2f); close()
            moveTo(9f, 4.5f); lineTo(9.6f, 4f); lineTo(10.2f, 4.5f); lineTo(9.6f, 5f); close()
            moveTo(14f, 4.5f); lineTo(14.6f, 4f); lineTo(15.2f, 4.5f); lineTo(14.6f, 5f); close()
        }
    }
}

/** 圣经掣签：打开的书 + 十字 + 掣签 */
val BibleLotIcon: ImageVector by lazy {
    divIcon("BibleLotIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 打开的书（左右两页 + 书脊 + 封面下缘）
            moveTo(4f, 4.5f); lineTo(11.5f, 4.5f); lineTo(11.5f, 18.5f); lineTo(4f, 18.5f); close()
            moveTo(12.5f, 4.5f); lineTo(20f, 4.5f); lineTo(20f, 18.5f); lineTo(12.5f, 18.5f); close()
            moveTo(11.5f, 4.5f); lineTo(12.5f, 4.5f); lineTo(12.5f, 18.5f); lineTo(11.5f, 18.5f); close()
            moveTo(4f, 18.5f); lineTo(20f, 18.5f); lineTo(20f, 19.6f); lineTo(4f, 19.6f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 十字（圣经）
            moveTo(15.4f, 7f); lineTo(16.6f, 7f); lineTo(16.6f, 11f); lineTo(15.4f, 11f); close()
            moveTo(14f, 8.2f); lineTo(18f, 8.2f); lineTo(18f, 9.4f); lineTo(14f, 9.4f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 掣签（木签斜插）+ 签头
            moveTo(5.5f, 15f); lineTo(9.5f, 11.6f); lineTo(10.1f, 12.3f); lineTo(6.1f, 15.7f); close()
            moveTo(9.3f, 11f); arcTo(0.6f, 0.6f, 0f, false, true, 9.3f, 12.2f); arcTo(0.6f, 0.6f, 0f, false, true, 9.3f, 11f); close()
        }
    }
}

/** 翻书占卜：打开的书 + 翻起的书页 + 文字行 */
val BibliomancyIcon: ImageVector by lazy {
    divIcon("BibliomancyIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 打开的书（两页 + 书脊）
            moveTo(3f, 5f); lineTo(11f, 5f); lineTo(11f, 19f); lineTo(3f, 19f); close()
            moveTo(13f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(13f, 19f); close()
            moveTo(11f, 5f); lineTo(13f, 5f); lineTo(13f, 19f); lineTo(11f, 19f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 翻起的书页（从右侧扬起）
            moveTo(16.5f, 6f); lineTo(20f, 4f); lineTo(17.5f, 1.5f); lineTo(14.5f, 3f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 页面文字行
            moveTo(5.5f, 9f); lineTo(10f, 9f); lineTo(10f, 9.6f); lineTo(5.5f, 9.6f); close()
            moveTo(5.5f, 12f); lineTo(10f, 12f); lineTo(10f, 12.6f); lineTo(5.5f, 12.6f); close()
            moveTo(14.5f, 9f); lineTo(19.5f, 9f); lineTo(19.5f, 9.6f); lineTo(14.5f, 9.6f); close()
            moveTo(14.5f, 12f); lineTo(19.5f, 12f); lineTo(19.5f, 12.6f); lineTo(14.5f, 12.6f); close()
        }
    }
}

/** 水占卜神签：水滴 + 水中浮现的签纸 + 水波 */
val MizuKujiIcon: ImageVector by lazy {
    divIcon("MizuKujiIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 水滴（上尖下圆）
            moveTo(12f, 1.8f)
            lineTo(17.8f, 11.5f)
            arcTo(5.8f, 5.8f, 0f, false, true, 6.2f, 11.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 水中浮现的签纸
            moveTo(11.2f, 9f); lineTo(12.8f, 9f); lineTo(12.8f, 16.5f); lineTo(11.2f, 16.5f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 水波 + 气泡
            moveTo(6.5f, 20f); lineTo(10.5f, 20f); lineTo(10.5f, 20.6f); lineTo(6.5f, 20.6f); close()
            moveTo(13f, 21f); lineTo(18f, 21f); lineTo(18f, 21.6f); lineTo(13f, 21.6f); close()
            moveTo(15.5f, 9f); arcTo(0.8f, 0.8f, 0f, false, true, 15.5f, 10.6f); arcTo(0.8f, 0.8f, 0f, false, true, 15.5f, 9f); close()
            moveTo(8f, 14f); arcTo(0.6f, 0.6f, 0f, false, true, 8f, 15.2f); arcTo(0.6f, 0.6f, 0f, false, true, 8f, 14f); close()
        }
    }
}

/** 日本御神签：折起的签纸（折纸结 + 折叠签条 + 朱印） */
val OmikujiIcon: ImageVector by lazy {
    divIcon("OmikujiIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 顶部折纸结
            moveTo(10.8f, 2f); lineTo(13.2f, 2f); lineTo(12f, 5f); close()
            moveTo(9.6f, 3f); lineTo(10.6f, 4.4f); lineTo(9.4f, 6f); lineTo(8.2f, 4.6f); close()
            moveTo(14.4f, 3f); lineTo(13.4f, 4.4f); lineTo(14.6f, 6f); lineTo(15.8f, 4.6f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 签条本体
            moveTo(7f, 6f); lineTo(17f, 6f); lineTo(17f, 21f); lineTo(7f, 21f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 手风琴折叠线
            moveTo(7.4f, 9f); lineTo(16.6f, 9f); lineTo(16.6f, 9.7f); lineTo(7.4f, 9.7f); close()
            moveTo(7.4f, 13f); lineTo(16.6f, 13f); lineTo(16.6f, 13.7f); lineTo(7.4f, 13.7f); close()
            moveTo(7.4f, 17f); lineTo(16.6f, 17f); lineTo(16.6f, 17.7f); lineTo(7.4f, 17.7f); close()
            // 签文朱印
            moveTo(13.4f, 15.3f); lineTo(15.6f, 15.3f); lineTo(15.6f, 16.6f); lineTo(13.4f, 16.6f); close()
        }
    }
}
