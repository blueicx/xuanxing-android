package com.xuanji.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import androidx.core.content.FileProvider
import java.io.File

object ResultShareCardRenderer {
    private const val WIDTH = 620
    private const val HEIGHT = 880

    fun render(card: ShareCard): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                0xFF241943.toInt(), 0xFF171029.toInt(), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), 44f, 44f, background)
        drawGlow(canvas)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x47E9D8A6.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(18f, 18f, 602f, 862f, 36f, 36f, border)

        text(canvas, card.eyebrow, 48f, 78f, size = 44f, bold = true, color = 0xFFE9D8A6.toInt())
        text(canvas, card.title.take(10), 48f, 132f, size = 84f, bold = true, color = 0xFFF2ECFF.toInt())
        if (card.badge.isNotBlank()) drawBadge(canvas, card.badge)

        var cursor = 218f
        if (card.headline.isNotBlank()) {
            text(canvas, card.headline, 48f, 300f, size = 184f, bold = true, color = card.accent)
            if (card.headlineUnit.isNotBlank()) {
                val headlineWidth = paint(184f, bold = true).measureText(card.headline)
                text(canvas, card.headlineUnit, 62f + headlineWidth, 296f, size = 60f, color = 0xFFC8BEE8.toInt())
            }
            cursor = 356f
        }

        val body = paint(54f)
        cursor += 20f
        wrap(body, card.summary, 524f).take(3).forEach { line ->
            text(canvas, line, 48f, cursor, size = 54f, color = 0xFFF2ECFF.toInt())
            cursor += 88f
        }

        card.rows.forEach { (label, value) ->
            if (cursor > 740f) return@forEach
            cursor += 32f
            text(canvas, label, 48f, cursor + 26f, size = 48f, bold = true, color = 0xFFC8BEE8.toInt())
            val valuePaint = paint(50f)
            wrap(valuePaint, value, 350f).forEachIndexed { index, line ->
                text(canvas, line, 168f, cursor + 26f + index * 66f, size = 50f, color = 0xFFF2ECFF.toInt())
            }
            cursor += maxOf(104f, 66 * wrap(valuePaint, value, 350f).size + 38f)
        }

        text(canvas, "玄星 · 本地推算 · 仅供娱乐参考", 48f, 826f, size = 42f, color = 0xFF7A7A8C.toInt())
        return bitmap
    }

    fun shareImage(context: Context, card: ShareCard): File {
        val directory = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val output = File(directory, "${System.currentTimeMillis()}.png")
        render(card).compress(Bitmap.CompressFormat.PNG, 100, output.outputStream())
        return output
    }

    fun uri(context: Context, file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    private fun drawGlow(canvas: Canvas) {
        val glow = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(0x29B69CFF, 0x00B69CFF)
        }
        glow.setBounds(350, -20, 690, 240)
        glow.draw(canvas)
    }

    private fun drawBadge(canvas: Canvas, badge: String) {
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 44f
            isFakeBoldText = true
        }
        val width = badgePaint.measureText(badge) + 64f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8FE3C2.toInt() }
        canvas.drawRoundRect(452f - width, 100f, 452f, 144f, 22f, 22f, fill)
        text(canvas, badge, 468f - width, 129f, size = 44f, bold = true, color = 0xFF12251F.toInt())
    }

    private fun paint(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        isFakeBoldText = bold
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        size: Float,
        bold: Boolean = false,
        color: Int
    ) {
        val paint = paint(size, bold).apply { this.color = color }
        canvas.drawText(value, x, y, paint)
    }

    private fun wrap(paint: Paint, value: String, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        value.split('\n').forEach { paragraph ->
            var line = ""
            paragraph.forEach { char ->
                val next = line + char
                if (paint.measureText(next) > maxWidth && line.isNotEmpty()) {
                    lines.add(line)
                    line = char.toString()
                } else {
                    line = next
                }
            }
            if (line.isNotEmpty()) lines.add(line)
        }
        return lines.ifEmpty { listOf("") }
    }
}
