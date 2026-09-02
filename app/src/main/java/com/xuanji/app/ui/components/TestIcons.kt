package com.xuanji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 测试中心专属矢量图标：18 个测试入口各配一个符号化图标。
 * 风格与 CustomIcons 一致——手绘几何轮廓、单色实心、随主题色着色（40dp 下轮廓依然清晰）。
 */

private fun testIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply(block).build()

/** 1. MBTI 职业性格：名牌外框 + 四个字母格 + 底部下划线 */
val MbtiTestIcon: ImageVector by lazy {
    testIcon("MbtiTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 名牌外框（四条边）
            moveTo(3.5f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 6.3f); lineTo(3.5f, 6.3f); close()
            moveTo(3.5f, 17.7f); lineTo(20.5f, 17.7f); lineTo(20.5f, 19f); lineTo(3.5f, 19f); close()
            moveTo(3.5f, 5f); lineTo(4.8f, 5f); lineTo(4.8f, 19f); lineTo(3.5f, 19f); close()
            moveTo(19.2f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 19f); lineTo(19.2f, 19f); close()
            // 四个字母格
            moveTo(5.6f, 8.6f); lineTo(8.2f, 8.6f); lineTo(8.2f, 11.2f); lineTo(5.6f, 11.2f); close()
            moveTo(9f, 8.6f); lineTo(11.6f, 8.6f); lineTo(11.6f, 11.2f); lineTo(9f, 11.2f); close()
            moveTo(12.4f, 8.6f); lineTo(15f, 8.6f); lineTo(15f, 11.2f); lineTo(12.4f, 11.2f); close()
            moveTo(15.8f, 8.6f); lineTo(18.4f, 8.6f); lineTo(18.4f, 11.2f); lineTo(15.8f, 11.2f); close()
            // 底部下划线
            moveTo(5.6f, 13.6f); lineTo(18.4f, 13.6f); lineTo(18.4f, 14.8f); lineTo(5.6f, 14.8f); close()
        }
    }
}

/** 2. 卡特尔 16PF：4×4 因子网格 */
val CattellTestIcon: ImageVector by lazy {
    testIcon("CattellTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 外框（四条边）
            moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 5.3f); lineTo(4f, 5.3f); close()
            moveTo(4f, 18.7f); lineTo(20f, 18.7f); lineTo(20f, 20f); lineTo(4f, 20f); close()
            moveTo(4f, 4f); lineTo(5.3f, 4f); lineTo(5.3f, 20f); lineTo(4f, 20f); close()
            moveTo(18.7f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(18.7f, 20f); close()
            // 三条竖线
            moveTo(8.45f, 4f); lineTo(9.55f, 4f); lineTo(9.55f, 20f); lineTo(8.45f, 20f); close()
            moveTo(12.45f, 4f); lineTo(13.55f, 4f); lineTo(13.55f, 20f); lineTo(12.45f, 20f); close()
            moveTo(16.45f, 4f); lineTo(17.55f, 4f); lineTo(17.55f, 20f); lineTo(16.45f, 20f); close()
            // 三条横线
            moveTo(4f, 8.45f); lineTo(20f, 8.45f); lineTo(20f, 9.55f); lineTo(4f, 9.55f); close()
            moveTo(4f, 12.45f); lineTo(20f, 12.45f); lineTo(20f, 13.55f); lineTo(4f, 13.55f); close()
            moveTo(4f, 16.45f); lineTo(20f, 16.45f); lineTo(20f, 17.55f); lineTo(4f, 17.55f); close()
        }
    }
}

/** 3. 霍兰德职业兴趣：六边形 + 六个顶点（RIASEC 六型） */
val HollandTestIcon: ImageVector by lazy {
    testIcon("HollandTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4.8f); lineTo(18.24f, 8.4f); lineTo(18.24f, 15.6f); lineTo(12f, 19.2f); lineTo(5.76f, 15.6f); lineTo(5.76f, 8.4f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 六个顶点
            moveTo(12f, 3.5f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 6.1f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 3.5f); close()
            moveTo(18.24f, 7.1f); arcTo(1.3f, 1.3f, 0f, false, false, 18.24f, 9.7f); arcTo(1.3f, 1.3f, 0f, false, false, 18.24f, 7.1f); close()
            moveTo(18.24f, 14.3f); arcTo(1.3f, 1.3f, 0f, false, false, 18.24f, 16.9f); arcTo(1.3f, 1.3f, 0f, false, false, 18.24f, 14.3f); close()
            moveTo(12f, 17.9f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 20.5f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 17.9f); close()
            moveTo(5.76f, 14.3f); arcTo(1.3f, 1.3f, 0f, false, false, 5.76f, 16.9f); arcTo(1.3f, 1.3f, 0f, false, false, 5.76f, 14.3f); close()
            moveTo(5.76f, 7.1f); arcTo(1.3f, 1.3f, 0f, false, false, 5.76f, 9.7f); arcTo(1.3f, 1.3f, 0f, false, false, 5.76f, 7.1f); close()
        }
    }
}

/** 4. 原创文字推理挑战：以抽象矩阵意象表现数列/类比练习。 */
val RavenTestIcon: ImageVector by lazy {
    testIcon("RavenTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 外框（四条边）
            moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 5.3f); lineTo(4f, 5.3f); close()
            moveTo(4f, 18.7f); lineTo(20f, 18.7f); lineTo(20f, 20f); lineTo(4f, 20f); close()
            moveTo(4f, 4f); lineTo(5.3f, 4f); lineTo(5.3f, 20f); lineTo(4f, 20f); close()
            moveTo(18.7f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(18.7f, 20f); close()
            // 两条竖线
            moveTo(8.78f, 4f); lineTo(9.88f, 4f); lineTo(9.88f, 20f); lineTo(8.78f, 20f); close()
            moveTo(14.12f, 4f); lineTo(15.22f, 4f); lineTo(15.22f, 20f); lineTo(14.12f, 20f); close()
            // 两条横线
            moveTo(4f, 8.78f); lineTo(20f, 8.78f); lineTo(20f, 9.88f); lineTo(4f, 9.88f); close()
            moveTo(4f, 14.12f); lineTo(20f, 14.12f); lineTo(20f, 15.22f); lineTo(4f, 15.22f); close()
            // 右下格缺失图形
            moveTo(17.61f, 16.01f); lineTo(19.21f, 17.61f); lineTo(17.61f, 19.21f); lineTo(16.01f, 17.61f); close()
        }
    }
}

/** 5. 大五人格：底部基线上的五个维度条（OCEAN） */
val BigFiveTestIcon: ImageVector by lazy {
    testIcon("BigFiveTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 底部基线
            moveTo(4f, 18f); lineTo(20f, 18f); lineTo(20f, 19.2f); lineTo(4f, 19.2f); close()
            // 五个高矮不一的维度条
            moveTo(4.4f, 9.5f); lineTo(6.8f, 9.5f); lineTo(6.8f, 18f); lineTo(4.4f, 18f); close()
            moveTo(7.6f, 6.5f); lineTo(10f, 6.5f); lineTo(10f, 18f); lineTo(7.6f, 18f); close()
            moveTo(10.8f, 11.5f); lineTo(13.2f, 11.5f); lineTo(13.2f, 18f); lineTo(10.8f, 18f); close()
            moveTo(14f, 7.5f); lineTo(16.4f, 7.5f); lineTo(16.4f, 18f); lineTo(14f, 18f); close()
            moveTo(17.2f, 10f); lineTo(19.6f, 10f); lineTo(19.6f, 18f); lineTo(17.2f, 18f); close()
        }
    }
}

/** 6. MMPI 心理：量表纸 + 长短不一的量表条 */
val MmpiTestIcon: ImageVector by lazy {
    testIcon("MmpiTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 量表纸外框（四条边）
            moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 5.3f); lineTo(5f, 5.3f); close()
            moveTo(5f, 18.7f); lineTo(19f, 18.7f); lineTo(19f, 20f); lineTo(5f, 20f); close()
            moveTo(5f, 4f); lineTo(6.3f, 4f); lineTo(6.3f, 20f); lineTo(5f, 20f); close()
            moveTo(17.7f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(17.7f, 20f); close()
            // 量表条
            moveTo(6.8f, 7f); lineTo(16f, 7f); lineTo(16f, 8.2f); lineTo(6.8f, 8.2f); close()
            moveTo(6.8f, 9.5f); lineTo(14.5f, 9.5f); lineTo(14.5f, 10.7f); lineTo(6.8f, 10.7f); close()
            moveTo(6.8f, 12f); lineTo(17f, 12f); lineTo(17f, 13.2f); lineTo(6.8f, 13.2f); close()
            moveTo(6.8f, 14.5f); lineTo(12.5f, 14.5f); lineTo(12.5f, 15.7f); lineTo(6.8f, 15.7f); close()
            moveTo(6.8f, 17f); lineTo(15f, 17f); lineTo(15f, 18.2f); lineTo(6.8f, 18.2f); close()
        }
    }
}

/** 7. 九型人格：圆周九个点位 + 中心三角（简化九型图） */
val EnneagramTestIcon: ImageVector by lazy {
    testIcon("EnneagramTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 中心三角
            moveTo(12f, 7.5f); lineTo(8.1f, 14.25f); lineTo(15.9f, 14.25f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 圆周九个点位
            moveTo(12f, 3.45f); arcTo(1.05f, 1.05f, 0f, false, false, 12f, 5.55f); arcTo(1.05f, 1.05f, 0f, false, false, 12f, 3.45f); close()
            moveTo(16.82f, 5.2f); arcTo(1.05f, 1.05f, 0f, false, false, 16.82f, 7.3f); arcTo(1.05f, 1.05f, 0f, false, false, 16.82f, 5.2f); close()
            moveTo(19.39f, 9.65f); arcTo(1.05f, 1.05f, 0f, false, false, 19.39f, 11.75f); arcTo(1.05f, 1.05f, 0f, false, false, 19.39f, 9.65f); close()
            moveTo(18.5f, 14.7f); arcTo(1.05f, 1.05f, 0f, false, false, 18.5f, 16.8f); arcTo(1.05f, 1.05f, 0f, false, false, 18.5f, 14.7f); close()
            moveTo(14.57f, 18f); arcTo(1.05f, 1.05f, 0f, false, false, 14.57f, 20.1f); arcTo(1.05f, 1.05f, 0f, false, false, 14.57f, 18f); close()
            moveTo(9.43f, 18f); arcTo(1.05f, 1.05f, 0f, false, false, 9.43f, 20.1f); arcTo(1.05f, 1.05f, 0f, false, false, 9.43f, 18f); close()
            moveTo(5.5f, 14.7f); arcTo(1.05f, 1.05f, 0f, false, false, 5.5f, 16.8f); arcTo(1.05f, 1.05f, 0f, false, false, 5.5f, 14.7f); close()
            moveTo(4.61f, 9.65f); arcTo(1.05f, 1.05f, 0f, false, false, 4.61f, 11.75f); arcTo(1.05f, 1.05f, 0f, false, false, 4.61f, 9.65f); close()
            moveTo(7.18f, 5.2f); arcTo(1.05f, 1.05f, 0f, false, false, 7.18f, 7.3f); arcTo(1.05f, 1.05f, 0f, false, false, 7.18f, 5.2f); close()
        }
    }
}

/** 8. DISC 行为风格：四象限（2×2），右上象限亮点 */
val DiscTestIcon: ImageVector by lazy {
    testIcon("DiscTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 外框（四条边）
            moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 6.3f); lineTo(5f, 6.3f); close()
            moveTo(5f, 17.7f); lineTo(19f, 17.7f); lineTo(19f, 19f); lineTo(5f, 19f); close()
            moveTo(5f, 5f); lineTo(6.3f, 5f); lineTo(6.3f, 19f); lineTo(5f, 19f); close()
            moveTo(17.7f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(17.7f, 19f); close()
            // 十字分隔线
            moveTo(11.45f, 5f); lineTo(12.55f, 5f); lineTo(12.55f, 19f); lineTo(11.45f, 19f); close()
            moveTo(5f, 11.45f); lineTo(19f, 11.45f); lineTo(19f, 12.55f); lineTo(5f, 12.55f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 右上象限亮点
            moveTo(15.8f, 6.4f); arcTo(1.8f, 1.8f, 0f, false, false, 15.8f, 10f); arcTo(1.8f, 1.8f, 0f, false, false, 15.8f, 6.4f); close()
        }
    }
}

/** 9. 性格色彩 FPA：四个色块圆（2×2） */
val ColorTestIcon: ImageVector by lazy {
    testIcon("ColorTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7.5f, 5.9f); arcTo(2.6f, 2.6f, 0f, false, false, 7.5f, 11.1f); arcTo(2.6f, 2.6f, 0f, false, false, 7.5f, 5.9f); close()
            moveTo(16.5f, 5.9f); arcTo(2.6f, 2.6f, 0f, false, false, 16.5f, 11.1f); arcTo(2.6f, 2.6f, 0f, false, false, 16.5f, 5.9f); close()
            moveTo(7.5f, 12.9f); arcTo(2.6f, 2.6f, 0f, false, false, 7.5f, 18.1f); arcTo(2.6f, 2.6f, 0f, false, false, 7.5f, 12.9f); close()
            moveTo(16.5f, 12.9f); arcTo(2.6f, 2.6f, 0f, false, false, 16.5f, 18.1f); arcTo(2.6f, 2.6f, 0f, false, false, 16.5f, 12.9f); close()
        }
    }
}

/** 10. 菲尔人格：简笔人物半身像（头 + 颈 + 肩） */
val PhilTestIcon: ImageVector by lazy {
    testIcon("PhilTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 头部
            moveTo(12f, 4f); arcTo(4f, 4f, 0f, false, false, 12f, 12f); arcTo(4f, 4f, 0f, false, false, 12f, 4f); close()
            // 颈部
            moveTo(10.5f, 12f); lineTo(13.5f, 12f); lineTo(13.5f, 15f); lineTo(10.5f, 15f); close()
            // 肩部
            moveTo(7.5f, 15f); lineTo(16.5f, 15f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        }
    }
}

/** 11. 颜色心理：心理爱心 + 四周色点（色环） */
val ColorPsychTestIcon: ImageVector by lazy {
    testIcon("ColorPsychTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 心形
            moveTo(10.4f, 8.2f); arcTo(1.6f, 1.6f, 0f, false, false, 10.4f, 11.4f); arcTo(1.6f, 1.6f, 0f, false, false, 10.4f, 8.2f); close()
            moveTo(13.6f, 8.2f); arcTo(1.6f, 1.6f, 0f, false, false, 13.6f, 11.4f); arcTo(1.6f, 1.6f, 0f, false, false, 13.6f, 8.2f); close()
            moveTo(12f, 15f); lineTo(8.5f, 10.7f); lineTo(15.5f, 10.7f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 四周色点
            moveTo(12f, 2.5f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 5.1f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 2.5f); close()
            moveTo(18.9f, 10.7f); arcTo(1.3f, 1.3f, 0f, false, false, 18.9f, 13.3f); arcTo(1.3f, 1.3f, 0f, false, false, 18.9f, 10.7f); close()
            moveTo(12f, 18.9f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 21.5f); arcTo(1.3f, 1.3f, 0f, false, false, 12f, 18.9f); close()
            moveTo(5.1f, 10.7f); arcTo(1.3f, 1.3f, 0f, false, false, 5.1f, 13.3f); arcTo(1.3f, 1.3f, 0f, false, false, 5.1f, 10.7f); close()
        }
    }
}

/** 12. 霍格沃茨：分院帽（巫师帽，帽檐 + 尖锥 + 帽带） */
val HogwartsTestIcon: ImageVector by lazy {
    testIcon("HogwartsTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 帽檐
            moveTo(4f, 19.2f); lineTo(20f, 19.2f); lineTo(18.6f, 20.8f); lineTo(5.4f, 20.8f); close()
            // 帽身（尖锥）
            moveTo(7.2f, 19.2f); lineTo(14.8f, 19.2f); lineTo(12.8f, 3.2f); close()
            // 帽带
            moveTo(8.6f, 16.2f); lineTo(13.6f, 16.2f); lineTo(13.6f, 17.4f); lineTo(8.6f, 17.4f); close()
        }
    }
}

/** 13. SBTI 处事风格：躺/卷/说/纠结四类，四个小人轮廓 */
val SbtiTestIcon: ImageVector by lazy {
    testIcon("SbtiTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 四个头
            moveTo(4.8f, 5.4f); arcTo(1.4f, 1.4f, 0f, false, false, 4.8f, 8.2f); arcTo(1.4f, 1.4f, 0f, false, false, 4.8f, 5.4f); close()
            moveTo(9.6f, 5.4f); arcTo(1.4f, 1.4f, 0f, false, false, 9.6f, 8.2f); arcTo(1.4f, 1.4f, 0f, false, false, 9.6f, 5.4f); close()
            moveTo(14.4f, 5.4f); arcTo(1.4f, 1.4f, 0f, false, false, 14.4f, 8.2f); arcTo(1.4f, 1.4f, 0f, false, false, 14.4f, 5.4f); close()
            moveTo(19.2f, 5.4f); arcTo(1.4f, 1.4f, 0f, false, false, 19.2f, 8.2f); arcTo(1.4f, 1.4f, 0f, false, false, 19.2f, 5.4f); close()
            // 四个身体
            moveTo(2.9f, 9.4f); lineTo(6.7f, 9.4f); lineTo(6.1f, 15.2f); lineTo(3.5f, 15.2f); close()
            moveTo(7.7f, 9.4f); lineTo(11.5f, 9.4f); lineTo(10.9f, 15.2f); lineTo(8.3f, 15.2f); close()
            moveTo(12.5f, 9.4f); lineTo(16.3f, 9.4f); lineTo(15.7f, 15.2f); lineTo(13.1f, 15.2f); close()
            moveTo(17.3f, 9.4f); lineTo(21.1f, 9.4f); lineTo(20.5f, 15.2f); lineTo(17.9f, 15.2f); close()
        }
    }
}

/** 14. 牛马浓度：牛头正面（颅顶圆 + 双角 + 双耳 + 鼻口） */
val CowHorseTestIcon: ImageVector by lazy {
    testIcon("CowHorseTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 头部主体（颅顶圆 + 面部方）
            moveTo(12f, 5f); arcTo(4f, 4f, 0f, false, false, 12f, 13f); arcTo(4f, 4f, 0f, false, false, 12f, 5f); close()
            moveTo(8f, 8f); lineTo(16f, 8f); lineTo(16f, 16.5f); lineTo(8f, 16.5f); close()
            // 鼻口
            moveTo(9.5f, 14.2f); lineTo(14.5f, 14.2f); lineTo(14.5f, 17.8f); lineTo(9.5f, 17.8f); close()
            // 双角
            moveTo(8.2f, 8f); lineTo(10f, 8f); lineTo(8.8f, 4f); close()
            moveTo(14f, 8f); lineTo(15.8f, 8f); lineTo(15.2f, 4f); close()
            // 双耳
            moveTo(6.4f, 10.5f); lineTo(8f, 9.2f); lineTo(8f, 12f); close()
            moveTo(17.6f, 10.5f); lineTo(16f, 9.2f); lineTo(16f, 12f); close()
        }
    }
}

/** 15. 恋爱16型：4×4 格子 + 中央爱心 */
val Love16TestIcon: ImageVector by lazy {
    testIcon("Love16TestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 4×4 外框与分隔线
            moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 5.3f); lineTo(4f, 5.3f); close()
            moveTo(4f, 18.7f); lineTo(20f, 18.7f); lineTo(20f, 20f); lineTo(4f, 20f); close()
            moveTo(4f, 4f); lineTo(5.3f, 4f); lineTo(5.3f, 20f); lineTo(4f, 20f); close()
            moveTo(18.7f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(18.7f, 20f); close()
            moveTo(8.45f, 4f); lineTo(9.55f, 4f); lineTo(9.55f, 20f); lineTo(8.45f, 20f); close()
            moveTo(12.45f, 4f); lineTo(13.55f, 4f); lineTo(13.55f, 20f); lineTo(12.45f, 20f); close()
            moveTo(16.45f, 4f); lineTo(17.55f, 4f); lineTo(17.55f, 20f); lineTo(16.45f, 20f); close()
            moveTo(4f, 8.45f); lineTo(20f, 8.45f); lineTo(20f, 9.55f); lineTo(4f, 9.55f); close()
            moveTo(4f, 12.45f); lineTo(20f, 12.45f); lineTo(20f, 13.55f); lineTo(4f, 13.55f); close()
            moveTo(4f, 16.45f); lineTo(20f, 16.45f); lineTo(20f, 17.55f); lineTo(4f, 17.55f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 中央爱心
            moveTo(10.4f, 8.3f); arcTo(1.5f, 1.5f, 0f, false, false, 10.4f, 11.3f); arcTo(1.5f, 1.5f, 0f, false, false, 10.4f, 8.3f); close()
            moveTo(13.6f, 8.3f); arcTo(1.5f, 1.5f, 0f, false, false, 13.6f, 11.3f); arcTo(1.5f, 1.5f, 0f, false, false, 13.6f, 8.3f); close()
            moveTo(12f, 14.5f); lineTo(8.6f, 10.8f); lineTo(15.4f, 10.8f); close()
        }
    }
}

/** 16. 恋爱说明书：上方爱心 + 打开的手册 */
val LoveManualTestIcon: ImageVector by lazy {
    testIcon("LoveManualTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 打开的书页
            moveTo(4.5f, 12f); lineTo(11f, 11.5f); lineTo(11f, 19.5f); lineTo(4.5f, 19f); close()
            moveTo(13f, 11.5f); lineTo(19.5f, 12f); lineTo(19.5f, 19f); lineTo(13f, 19.5f); close()
            // 书脊
            moveTo(11f, 11.6f); lineTo(13f, 11.6f); lineTo(13f, 19.6f); lineTo(11f, 19.6f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 上方爱心
            moveTo(10.4f, 4.1f); arcTo(1.5f, 1.5f, 0f, false, false, 10.4f, 7.1f); arcTo(1.5f, 1.5f, 0f, false, false, 10.4f, 4.1f); close()
            moveTo(13.6f, 4.1f); arcTo(1.5f, 1.5f, 0f, false, false, 13.6f, 7.1f); arcTo(1.5f, 1.5f, 0f, false, false, 13.6f, 4.1f); close()
            moveTo(12f, 10.3f); lineTo(8.6f, 6.6f); lineTo(15.4f, 6.6f); close()
        }
    }
}

/** 17. 动物人格：爪印（大肉垫 + 四趾垫） */
val AnimalTestIcon: ImageVector by lazy {
    testIcon("AnimalTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 四个趾垫
            moveTo(7.8f, 7.8f); arcTo(1.4f, 1.4f, 0f, false, false, 7.8f, 10.6f); arcTo(1.4f, 1.4f, 0f, false, false, 7.8f, 7.8f); close()
            moveTo(16.2f, 7.8f); arcTo(1.4f, 1.4f, 0f, false, false, 16.2f, 10.6f); arcTo(1.4f, 1.4f, 0f, false, false, 16.2f, 7.8f); close()
            moveTo(10.2f, 5.8f); arcTo(1.4f, 1.4f, 0f, false, false, 10.2f, 8.6f); arcTo(1.4f, 1.4f, 0f, false, false, 10.2f, 5.8f); close()
            moveTo(13.8f, 5.8f); arcTo(1.4f, 1.4f, 0f, false, false, 13.8f, 8.6f); arcTo(1.4f, 1.4f, 0f, false, false, 13.8f, 5.8f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 主肉垫（椭圆）
            moveTo(12f, 12.5f); arcTo(4f, 3f, 0f, false, false, 12f, 18.5f); arcTo(4f, 3f, 0f, false, false, 12f, 12.5f); close()
        }
    }
}

/** 18. 美食水果人格：餐叉 + 汤勺 */
val FoodTestIcon: ImageVector by lazy {
    testIcon("FoodTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 餐叉：四齿 + 颈 + 柄
            moveTo(7.6f, 5f); lineTo(8.25f, 5f); lineTo(8.25f, 11f); lineTo(7.6f, 11f); close()
            moveTo(8.55f, 5f); lineTo(9.2f, 5f); lineTo(9.2f, 11f); lineTo(8.55f, 11f); close()
            moveTo(9.5f, 5f); lineTo(10.15f, 5f); lineTo(10.15f, 11f); lineTo(9.5f, 11f); close()
            moveTo(10.45f, 5f); lineTo(11.1f, 5f); lineTo(11.1f, 11f); lineTo(10.45f, 11f); close()
            moveTo(8.9f, 11.2f); lineTo(9.7f, 11.2f); lineTo(9.7f, 13f); lineTo(8.9f, 13f); close()
            moveTo(8.5f, 13f); lineTo(10.1f, 13f); lineTo(10.1f, 20f); lineTo(8.5f, 20f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 汤勺：勺头（椭圆）+ 柄
            moveTo(15f, 5.8f); arcTo(2.5f, 3.4f, 0f, false, false, 15f, 12.6f); arcTo(2.5f, 3.4f, 0f, false, false, 15f, 5.8f); close()
            moveTo(14.3f, 12.4f); lineTo(15.7f, 12.4f); lineTo(15.7f, 13.4f); lineTo(14.3f, 13.4f); close()
            moveTo(14.3f, 13.4f); lineTo(15.7f, 13.4f); lineTo(15.7f, 20f); lineTo(14.3f, 20f); close()
        }
    }
}


/** 影视动漫角色：面具 / 聚光灯 */
val CharacterTestIcon: ImageVector by lazy {
    testIcon("CharacterTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 舞台灯光 + 角色脸
            moveTo(12f, 5f)
            arcTo(3f, 3f, 0f, false, false, 12f, 11f)
            arcTo(3f, 3f, 0f, false, false, 12f, 5f)
            close()
            moveTo(8f, 13f)
            arcTo(4.2f, 4.2f, 0f, false, true, 16f, 13f)
            lineTo(16f, 16f)
            lineTo(8f, 16f)
            close()
            moveTo(4f, 18f)
            lineTo(6f, 20f)
            lineTo(18f, 20f)
            lineTo(20f, 18f)
            close()
            // 头顶光点
            moveTo(11f, 3f)
            lineTo(13f, 3f)
            moveTo(12f, 2f)
            lineTo(12f, 4f)
        }
    }
}

/** FBTI 美食 MBTI：叉勺 + 四字母 */
val FbtiTestIcon: ImageVector by lazy {
    testIcon("FbtiTestIcon") {
        path(fill = SolidColor(Color.Black)) {
            // 餐盘
            moveTo(5f, 5f)
            arcTo(7f, 7f, 0f, false, true, 19f, 5f)
            lineTo(19f, 8f)
            arcTo(7f, 7f, 0f, false, false, 5f, 8f)
            close()
            moveTo(7f, 8f)
            lineTo(17f, 8f)
            lineTo(17f, 9f)
            lineTo(7f, 9f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            // 四字母块
            moveTo(8f, 12f)
            lineTo(16f, 12f)
            lineTo(16f, 15f)
            lineTo(8f, 15f)
            close()
            moveTo(9f, 17f)
            lineTo(12f, 17f)
            lineTo(12f, 19f)
            lineTo(9f, 19f)
            close()
            moveTo(13f, 17f)
            lineTo(15f, 17f)
            lineTo(15f, 19f)
            lineTo(13f, 19f)
            close()
        }
    }
}
