package com.xuanji.app.domain

import androidx.compose.ui.graphics.Color
import com.xuanji.app.data.model.Element

/** 五行中文名 */
fun elementName(e: Element): String = when (e) {
    Element.WOOD -> "木"
    Element.FIRE -> "火"
    Element.EARTH -> "土"
    Element.METAL -> "金"
    Element.WATER -> "水"
}

/** 五行生（a 生 b）: 木生火, 火生土, 土生金, 金生水, 水生木 */
fun produces(a: Element, b: Element): Boolean = when (a to b) {
    Element.WOOD to Element.FIRE,
    Element.FIRE to Element.EARTH,
    Element.EARTH to Element.METAL,
    Element.METAL to Element.WATER,
    Element.WATER to Element.WOOD -> true
    else -> false
}

/** 五行克（a 克 b）: 木克土, 土克水, 水克火, 火克金, 金克木 */
fun controls(a: Element, b: Element): Boolean = when (a to b) {
    Element.WOOD to Element.EARTH,
    Element.EARTH to Element.WATER,
    Element.WATER to Element.FIRE,
    Element.FIRE to Element.METAL,
    Element.METAL to Element.WOOD -> true
    else -> false
}

/** 五行幸运色（中文） */
fun elementColor(e: Element): String = when (e) {
    Element.WOOD -> "青碧色"
    Element.FIRE -> "朱红色"
    Element.EARTH -> "明黄色"
    Element.METAL -> "金银白"
    Element.WATER -> "玄黑色"
}

/** 五行吉利方位 */
fun elementDirection(e: Element): String = when (e) {
    Element.WOOD -> "东方"
    Element.FIRE -> "南方"
    Element.EARTH -> "中央"
    Element.METAL -> "西方"
    Element.WATER -> "北方"
}

/** 五行对应的 Compose 颜色（用于 UI 展示） */
fun elementColorCompose(e: Element): Color = when (e) {
    Element.WOOD -> Color(0xFF5FB87A)
    Element.FIRE -> Color(0xFFE0594E)
    Element.EARTH -> Color(0xFFC9A227)
    Element.METAL -> Color(0xFFCFCFCF)
    Element.WATER -> Color(0xFF4A90D9)
}
