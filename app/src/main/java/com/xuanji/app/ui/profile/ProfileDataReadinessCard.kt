package com.xuanji.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.BirthProfileReadinessEvaluator
import com.xuanji.app.domain.ProfileInputAvailability

@Composable
fun ProfileDataReadinessCard(profile: UserProfile?) {
    val readiness = BirthProfileReadinessEvaluator.evaluate(profile)
    Card(Modifier.fillMaxWidth().testTag("profile-data-readiness")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("资料完整度与影响", style = MaterialTheme.typography.titleSmall)
            Text(
                "这里只说明已保存输入的覆盖情况，不会改变算法分数或替你推断缺失资料。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            readiness.fields.forEach { field ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${field.label}${if (field.optional) "（可选）" else ""}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        if (field.availability == ProfileInputAvailability.Available) "已录入" else "未录入",
                        modifier = Modifier.testTag("profile-readiness-${field.key}"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (field.availability == ProfileInputAvailability.Available) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(field.impact, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
