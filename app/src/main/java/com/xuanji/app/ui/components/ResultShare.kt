package com.xuanji.app.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object ResultShare {
    private const val APP_NAME = "玄星"

    fun fortuneTitle(section: String, period: String = "day", score: Int? = null): String {
        val periodLabel = when (period) {
            "week" -> "本周"
            "month" -> "本月"
            else -> "今日"
        }
        val scoreText = score?.let { " ${it}分" } ?: ""
        return "$APP_NAME · ${periodLabel}${section}${scoreText}"
    }
}

@Composable
fun ShareButton(sharedText: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    IconButton(
        onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sharedText)
            }
            context.startActivity(Intent.createChooser(intent, null))
        },
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = "分享",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
    }
}
