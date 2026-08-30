package com.xuanji.app.ui.components

import android.content.Intent
import android.content.ClipData
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            "year" -> "本年"
            else -> "今日"
        }
        val scoreText = score?.let { " ${it}分" } ?: ""
        return "$APP_NAME · ${periodLabel}${section}${scoreText}"
    }
}

@Composable
fun ShareButton(
    sharedCard: ShareCard,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            scope.launch {
                val uri = withContext(Dispatchers.IO) {
                    val file = ResultShareCardRenderer.shareImage(context, sharedCard)
                    ResultShareCardRenderer.uri(context, file)
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(sharedCard.title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, sharedCard.title))
            }
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
