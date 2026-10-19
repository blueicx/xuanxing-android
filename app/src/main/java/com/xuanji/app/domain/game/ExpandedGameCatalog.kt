package com.xuanji.app.domain.game

internal object ExpandedGameCatalog {
    val poetry = """
        雨后青苔碧|an|桥边白鹭闲|门前旧树寒|一叶过溪中
        山窗听夜雨|ong|灯火照江中|孤帆入远空|竹影到阶前
        春水绕城去|ing|微光映晚星|归舟近小城|远岭入云间
        一卷留香墨|an|疏花倚石栏|新月照溪湾|晨露挂梧桐
        竹院清风起|ong|茶烟入暮空|灯影伴书童|白云过远山
        荷叶翻晴色|ing|水面映繁星|晚钟到古城|轻舟泊柳岸
        石径留秋色|an|松声入夜寒|云影过山峦|暮雨洗梧桐
        小院听泉响|ong|归雁过长空|明月照江中|晨光临竹案
        草木迎春雨|ing|湖光映晓星|柳色绕山城|清茶待客来
        纸上藏山水|an|窗前起暮岚|庭外听鸣蝉|风轻入竹丛
        书屋灯初亮|ong|一盏伴书童|遥山隐暮空|茶香过短栏
        旧桥新雨后|ing|一水绕青城|两岸映疏星|红叶落阶前
        窗外梅初绽|an|炉边夜未寒|月下倚朱栏|山风入古松
        小舟寻远渡|ong|白浪接晴空|清影落江中|霜叶过山前
        林间听鸟语|ing|溪畔望疏星|花下忆山城|一灯伴砚台
        远寺钟声静|an|晚霞落石栏|清露润幽兰|书声入夜风
        江岸灯初上|ong|舟影入波中|烟云散暮空|石阶留古韵
        细雨敲茶案|ing|轻云绕古城|疏影映寒星|夜色到桥边
        春山开画卷|an|绿水过溪湾|白鹭立沙滩|一笔写清风
    """.trimIndent().lines().mapIndexed { index, line ->
        val f = line.trim().split('|')
        PoetryPrompt("p${index + 6}", f[0], f[1], f.drop(2), "按句尾简化韵脚 ${f[1]} 判断；文字来自玄星自有短句库。")
    }
    val stars = (6..24).map { level ->
        val size = 4 + level % 4
        val names = (0 until size).map { "图$level·星点${it + 1}" }
        val edges = names.associateWith { linkedSetOf<String>() }
        fun link(a: Int, b: Int) { edges.getValue(names[a]).add(names[b]); edges.getValue(names[b]).add(names[a]) }
        (0 until size - 1).forEach { link(it, it + 1) }
        if (level % 3 != 0) link(0, 2)
        if (level % 3 == 2) link(1, size - 1)
        val solution = when (level % 3) { 0 -> names; 1 -> listOf(names[0]) + names.drop(2); else -> listOf(names[0], names[1], names.last()) }
        StarMapPuzzle("s$level", names.first(), names.last(), edges.mapValues { it.value.toSet() }, solution,
            "可行路线：${solution.joinToString(" → ")}；这是自编连线逻辑图。")
    }
}
