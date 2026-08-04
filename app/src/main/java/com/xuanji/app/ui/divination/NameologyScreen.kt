package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.Nameology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun NameologyScreen() {
    var surname by remember { mutableStateOf("") }
    var givenName by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("姓名学（五格剖象）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "输入姓氏与名字，按五格剖象法计算天格、人格、地格、外格、总格数理及吉凶。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("nameology")

        FortuneCard {
            SectionTitle("姓名输入")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("姓氏，如：张") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = givenName,
                onValueChange = { givenName = it },
                label = { Text("名字，如：伟明") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (surname.trim().isNotEmpty() && givenName.trim().isNotEmpty()) {
            val res = Nameology.analyze(surname, givenName)
            FortuneCard {
                SectionTitle("五格数理 · ${res.surname}${res.givenName}")
                Spacer(Modifier.height(8.dp))
                res.geList.forEach { ge ->
                    InfoRow("${ge.name}（${ge.element}）·${ge.luck}", "${ge.number}")
                    Text(ge.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }
            FortuneCard {
                SectionTitle("综合解读")
                Spacer(Modifier.height(8.dp))
                Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
            }
            FortuneCard {
                SectionTitle("综合建议")
                Spacer(Modifier.height(8.dp))
                Text(res.note, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text("请输入姓氏和名字以查看五格分析。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
