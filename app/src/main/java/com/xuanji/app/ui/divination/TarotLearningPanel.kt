package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.model.DrawnTarot
import com.xuanji.app.domain.divination.DivinationLearning
import com.xuanji.app.ui.components.*

@Composable
fun TarotLearningPanel(cards: List<DrawnTarot>, dateKey: String, seed: String?) {
    if (cards.isEmpty()) return
    FortuneCard {
        SectionTitle("牌阵关系与复盘")
        Text(DivinationLearning.SCOPE, style = MaterialTheme.typography.bodySmall)
        DivinationLearning.tarotRelations(cards).forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Text("保存后可在安排与记录日历回看原牌面；心得请主动写入当天记录，不会自动生成长期记忆。", style = MaterialTheme.typography.bodySmall)
        SaveReadingSnapshotAction(DivinationLearning.tarotDetail(cards, dateKey, seed))
    }
}
