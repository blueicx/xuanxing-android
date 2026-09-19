package com.xuanji.app.domain

/** Pure dialogue templates. The generator owns routing and safety; this module owns wording only. */
internal object MysticDialogueTemplates {
    internal fun pulse(value: String): Long {
        var hash = 5381L
        for (char in value.trim()) {
            hash = (hash * 33L + char.code) % 2147483647L
        }
        return hash
    }

    /** 闲聊不报分数：先像熟人一样接话，再留一个轻入口。 */
    fun greetingAnswer(scholar: Boolean, styleKey: String, skinId: String, question: String): String {
        val variant = (pulse(question) % 3L).toInt()
        val lateNight = listOf("晚安", "夜里好", "晚好").any { question.contains(it) }
        skinGreetingLine(skinId, lateNight, variant)?.let { return it }
        if (lateNight) {
            return if (scholar) {
                when (styleKey) {
                    "harbor" -> "晚安。灯给你留一盏小的，今晚早点靠岸。"
                    "archive" -> "晚安。今天这页先合上，明天再看也不迟。"
                    else -> "晚安。罗盘也该歇一歇了。"
                }
            } else {
                when (styleKey) {
                    "alley" -> "晚安啊？行，大碗茶给你扣上，回去睡踏实点。"
                    "herald" -> "收锣！今晚不许再折腾自己，明儿再上场。"
                    else -> "晚安签收！实习生闭嘴，仙界静音。"
                }
            }
        }
        return if (scholar) {
            when (styleKey) {
                "archive" -> if (variant == 0) "来了？书页刚翻开，坐。" else "嗯，我在。今天想先说哪一件？"
                "harbor" -> if (variant == 0) "来了就坐下，外头的事先放一放。" else "我在这儿呢。慢慢说，不急。"
                else -> if (variant == 0) "你好。指针停稳了，先聊聊近况也行。" else "你好呀。今天风不大，适合说话。"
            }
        } else {
            when (styleKey) {
                "alley" -> if (variant == 0) "哟，来了？茶还热着，说吧。" else "嘿，稀客。今天什么风把你吹来的？"
                "herald" -> if (variant == 0) "来啦？锣鼓先收着，咱好好说话。" else "哟，人到了就好！坐前排。"
                else -> if (variant == 0) "你好你好！信号接通，本半仙在线。" else "嗨，来了？今天不用装客气。"
            }
        }
    }

    fun farewellAnswer(scholar: Boolean, styleKey: String): String =
        if (scholar) {
            when (styleKey) {
                "archive" -> "好，这一页先替你夹好。回见。"
                "harbor" -> "去吧，别赶太急。泊位一直留着。"
                else -> "好，方向记下了。慢一点走。"
            }
        } else {
            when (styleKey) {
                "alley" -> "成，那先散了。有事回来敲门。"
                "herald" -> "退场不催！回头有戏，我再喊你。"
                else -> "收到，本次会话……咳，咱下次接着唠！"
            }
        }

    fun thanksAnswer(scholar: Boolean, styleKey: String): String =
        if (scholar) {
            when (styleKey) {
                "archive" -> "不必谢。能帮你把纸页理顺，就好。"
                "harbor" -> "不用谢。你能松口气，比什么都值。"
                else -> "举手之劳。路还得你自己走稳。"
            }
        } else {
            when (styleKey) {
                "alley" -> "谢啥？下回带点八卦来抵账。"
                "herald" -> "谢就免了！掌声留下也行。"
                else -> "小场面！五星好评……开玩笑，记得照顾自己就行。"
            }
        }

    fun identityAnswer(scholar: Boolean, styleKey: String, personaLabel: String): String {
        val name = personaLabel
        return if (scholar) {
            when (styleKey) {
                "archive" -> "$name，一个守旧档的人。我不替你判命，只帮你看清手边能做的事。"
                "harbor" -> "我叫$name。夜里守灯，白天听人说话；算盘有几把，但从不吓人。"
                else -> "$name。罗盘在我手里，方向盘仍在你手里。"
            }
        } else {
            when (styleKey) {
                "alley" -> "街口$name。收费随缘，实话管饱。"
                "herald" -> "$name！台前能镇场，幕后会记账；命理这行，也得讲证据。"
                else -> "云端实习$name。天机会看，大话不说。"
            }
        }
    }

    fun smallTalkAnswer(scholar: Boolean, styleKey: String, question: String): String {
        val variant = (pulse(question) % 3L).toInt()
        return if (scholar) {
            when (styleKey) {
                "archive" -> when (variant) {
                    0 -> "刚把一页旧注脚压平。你那边呢，今天过得顺不顺？"
                    1 -> "我在，没忙着算什么。你想闲聊几句也可以。"
                    else -> "书斋里安静得很。有话就说，不用先挑重点。"
                }
                "harbor" -> when (variant) {
                    0 -> "在灯边坐着呢。你来了，水面倒热闹了一点。"
                    1 -> "没做什么大事，等你把今天的琐碎倒出来。"
                    else -> "闲着也是闲着。陪你说两句，不算浪费。"
                }
                else -> when (variant) {
                    0 -> "在呢。罗盘转累了，正好歇口气。"
                    1 -> "听着呢。哪怕只是碎碎念，我也接得住。"
                    else -> "今天不急着看盘，先听听你的动静。"
                }
            }
        } else {
            when (styleKey) {
                "alley" -> when (variant) {
                    0 -> "刚泡上茶，正闲着。你要不要也唠两句？"
                    1 -> "在这儿蹲着呢。别憋着，无聊就往外倒。"
                    else -> "街口风挺舒服。说吧，今天谁又惹你了？"
                }
                "herald" -> when (variant) {
                    0 -> "候场中！闲聊也是正经戏，开嗓吧。"
                    1 -> "在后台擦锣呢。你来啦，刚好解闷。"
                    else -> "本台节目随时开播，今天先播你的日常。"
                }
                else -> when (variant) {
                    0 -> "在线在线！云朵刚充完电。"
                    1 -> "等着呢。工单栏空着，聊天栏开着。"
                    else -> "本半仙不忙。你要是想找人说话，就算找对地方了。"
                }
            }
        }
    }

    /** 自由话题先被当作“人说话”，不硬塞分数；只有明确问势时才回到盘面。 */
    fun dailyChatAnswer(scholar: Boolean, styleKey: String, skinId: String, question: String): String =
        conversationalAnswer(scholar, styleKey, skinId, "daily", question)

    fun chatAnswer(scholar: Boolean, styleKey: String, skinId: String, question: String): String =
        conversationalAnswer(scholar, styleKey, skinId, "chat", question)

    private fun conversationalAnswer(
        scholar: Boolean,
        styleKey: String,
        skinId: String,
        kind: String,
        question: String
    ): String {
        val variant = (pulse(question) % 3L).toInt()
        val key = "$kind-$variant"
        when (skinId) {
            "jiangnan-robe" -> return when (key) {
                "daily-0" -> "饿了就先吃，账册不会跑。想吃热的，还是想吃一口甜的？"
                "daily-1" -> "雨声里最适合把饭吃慢一点。你今天嘴里想吃什么？"
                "daily-2" -> "身体先顾好。吃完这口，我们再说别的。"
                "chat-0" -> "这句我先放在茶边。你是随口一说，还是心里已经有答案了？"
                "chat-1" -> "嗯，我听着。这件事最硌你的地方在哪？"
                else -> "砚台磨好了，先不说结论。你想从哪一句说起？"
            }
            "academy-gown" -> return when (key) {
                "daily-0" -> "先把饭安排上。观察可以等，胃不能等。"
                "daily-1" -> "这是今天的第一个信号：你需要休息和补给。打算吃什么？"
                "daily-2" -> "记录暂停，优先照顾身体。吃饱再回来核对。"
                "chat-0" -> "我把这句话当成一个观察。它让你想到的最直接结果是什么？"
                "chat-1" -> "先别急着定性。这里面有事实，也有情绪，你想拆哪一层？"
                else -> "我在听。说具体一点，我们就能少绕一步。"
            }
            "silkroad-robe" -> return when (key) {
                "daily-0" -> "远行的人先补水进食。今天想吃热汤，还是清爽的？"
                "daily-1" -> "驿站灯亮着，饭比赶路要紧。慢慢来。"
                "daily-2" -> "风大就更别空着肚子。先吃，再谈下一程。"
                "chat-0" -> "这话像半路捡到的石头。先掂掂分量，再看要不要带走。"
                "chat-1" -> "嗯，路还长。你最想先弄清的是人，还是事？"
                else -> "我记下了。你说完，我们再决定哪段先走。"
            }
            "northland-mantle" -> return when (key) {
                "daily-0" -> "饿了就靠近火边。热乎的先下肚，别的都好商量。"
                "daily-1" -> "天冷，身子比道理实在。先吃口热的。"
                "daily-2" -> "帐篷里有位子，吃完再说话也不迟。"
                "chat-0" -> "这句话先埋进雪里焐一焐。你觉得它是麻烦，还是机会？"
                "chat-1" -> "我听见了。边界在哪里，你心里有没有数？"
                else -> "火不灭，话不急。慢慢说。"
            }
            "cloud-daoist" -> return when (key) {
                "daily-0" -> "饿是身体来讨债，赖不掉。先吃，账我再替你看着。"
                "daily-1" -> "云缝里飘来一股烟火气。去吃吧，回来再谈正事。"
                "daily-2" -> "这单便宜：先喂饱自己，利息全免。"
                "chat-0" -> "有意思。你说这话时，是想让我点头，还是想让我拆台？"
                "chat-1" -> "先别亮全部底牌。我只问一句：这事谁最受益？"
                else -> "袖里这笔我先记着。继续说，看能不能换出个更有用的东西。"
            }
            "street-jacket" -> return when (key) {
                "daily-0" -> "饿就说饿，装什么仙。巷口那家热的先安排上。"
                "daily-1" -> "空肚子算不准事。先吃，碗我给你扣着。"
                "daily-2" -> "吃饭不算浪费时间，硬撑才费命。去吧。"
                "chat-0" -> "哟，话里有钩子。你想聊闲篇，还是想让我帮你看门道？"
                "chat-1" -> "行，这茬我不接虚的。说说谁沾了这事，谁又躲开了？"
                else -> "茶续上了。别绕，把你最不想说的那句放桌面上。"
            }
            "desert-traveler" -> return when (key) {
                "daily-0" -> "饿是沙丘给的第一道路标。先吃，别跟自己讨价还价。"
                "daily-1" -> "水囊旁边留了半个位置给饭。吃完再走。"
                "daily-2" -> "日头毒，空腹走远路是傻事。先歇嘴，再歇脚。"
                "chat-0" -> "脚印很新，话也很新。你想让我听，还是让我帮你辨真假？"
                "chat-1" -> "沙子里藏不住太重的心事。哪一块最先压着你？"
                else -> "坐。火堆小，但够照见你脸上那点犹豫。"
            }
            "festival-costume" -> return when (key) {
                "daily-0" -> "角儿也得吃饭。后台的热食先端上来，戏压一折。"
                "daily-1" -> "锣先停。吃饱了才有力气翻下一折。"
                "daily-2" -> "台面可以等，嗓子不能空着。去吃。"
                "chat-0" -> "这句词有点意思。你想让我喝彩，还是想让我挑错？"
                "chat-1" -> "后台只说实话。这一折，你自己最心虚的是哪句？"
                else -> "锣槌放下，你说。我看看这场戏值不值得接。"
            }
        }
        return if (scholar) {
            when (styleKey) {
                "archive" -> if (kind == "daily") "先处理身体这件小事；档案页不会跑。" else "这句话先归到待核那一栏。你想补充什么细节？"
                "harbor" -> if (kind == "daily") "先让自己舒服一点，水面稳了再谈别的。" else "我听见你了。不用急着把它变成问题。"
                else -> if (kind == "daily") "先把眼前的小事安顿好，方向随后再校。" else "范围先放宽一点说，我再帮你收窄。"
            }
        } else {
            when (styleKey) {
                "herald" -> if (kind == "daily") "锣鼓暂停！先把这点小事办体面！" else "这句台词有戏。你想让我捧场，还是挑刺？"
                "alley" -> if (kind == "daily") "别硬撑，先把眼前的亏空补上。" else "行，这话我收了。可它背后那点心思呢？"
                else -> if (kind == "daily") "系统提示：先补给，后运转。这条建议免费。" else "底牌先别掀。我只问，你最怕别人看出什么？"
            }
        }
    }

    /** 问候先落在当前文化身份上，再留一个不催促的入口。 */
    private fun skinGreetingLine(skinId: String, lateNight: Boolean, variant: Int): String? = when (skinId) {
        "jiangnan-robe" -> when {
            lateNight -> "晚安。灯芯压低些，明天的事让雨先听着。"
            variant == 0 -> "来了？茶刚醒，先坐，不急。"
            variant == 1 -> "你好。檐下风软，说点近况正好。"
            else -> "嗯，我在。今天不必先挑要紧事。"
        }
        "academy-gown" -> when {
            lateNight -> "晚安。今天这一组观察先归档。"
            variant == 0 -> "你好。记录本空着，先聊两句也行。"
            variant == 1 -> "来了？不急着给结论，先说说今天。"
            else -> "嗯，我在听。你想从哪句开始都可以。"
        }
        "silkroad-robe" -> when {
            lateNight -> "晚安。驿站灯亮着，行李明天再理。"
            variant == 0 -> "来了？先坐，沙子拍干净再说话。"
            variant == 1 -> "你好。驼铃歇了，正好听你讲讲路上事。"
            else -> "我在。远路不催，闲话也能落脚。"
        }
        "northland-mantle" -> when {
            lateNight -> "晚安。炉火封好了，风声交给帐篷。"
            variant == 0 -> "来了？火边有位子，先暖手。"
            variant == 1 -> "你好。雪停了一会儿，慢慢说。"
            else -> "嗯，我在这儿。今天不用赶着交代什么。"
        }
        "cloud-daoist" -> when {
            lateNight -> "收云，闭账。晚安，梦里别签契。"
            variant == 0 -> "哟，云缝里掉下来一单生意。坐，先说清楚：天机可以聊，保票不卖。"
            variant == 1 -> "你好啊。今天天机打烊，人话倒还剩几句。"
            else -> "来了？本半仙刚好有空。便宜话管够，真话另算。"
        }
        "street-jacket" -> when {
            lateNight -> "这么晚？茶扣上，回去睡，明儿再折腾。"
            variant == 0 -> "哟，来了？茶还热着。想问路还是想问人？"
            variant == 1 -> "嘿，稀客。巷口风大，先把来意放桌上。"
            else -> "别装客气。我这儿只有热茶、冷话和几条近道。"
        }
        "desert-traveler" -> when {
            lateNight -> "夜风凉了，晚安。水囊我先收好。"
            variant == 0 -> "风把你送来了？拍拍沙。过路费是一句真话。"
            variant == 1 -> "火堆小，话能烤热。想借光，就别带假货。"
            else -> "脚印很新。坐下前先想好：你要问水，还是问人心？"
        }
        "festival-costume" -> when {
            lateNight -> "锣入箱，灯落幕。晚安，别再赶场。"
            variant == 0 -> "哟，角儿来了？锣先捂住，后台只说人话。"
            variant == 1 -> "你好呀。茶在箱上，戏先不点，底牌也别亮。"
            else -> "来得巧。我正缺个搭戏的，可惜更缺一句实话。"
        }
        else -> null
    }
}
