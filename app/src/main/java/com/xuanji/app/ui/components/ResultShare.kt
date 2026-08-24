package com.xuanji.app.ui.components

import android.content.Intent
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
            else -> "今日"
        }
        val scoreText = score?.let { " ${it}分" } ?: ""
        return "$APP_NAME · ${periodLabel}${section}${scoreText}"
    }
}

@Composable
fun ShareButton(
    sharedCard: ShareCard? = null,
    sharedText: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            val card = sharedCard
            if (card == null) {
                startTextShare(context, sharedText)
            } else {
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        val file = ResultShareCardRenderer.shareImage(context, card)
                        ResultShareCardRenderer.uri(context, file)
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "${card.title}\n${card.summary}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
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

private fun startTextShare(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
