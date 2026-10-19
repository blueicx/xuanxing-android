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
    private const val MIN_HEIGHT = 880

    fun render(card: ShareCard): Bitmap {
        data class Block(val text: String, val size: Float, val bold: Boolean, val color: Int)
        val blocks = buildList {
            add(Block(card.eyebrow, 24f, false, 0xFFE9D8A6.toInt()))
            add(Block(card.title, 40f, true, 0xFFF2ECFF.toInt()))
            if (card.badge.isNotBlank()) add(Block(card.badge, 24f, true, 0xFF8FE3C2.toInt()))
            if (card.headline.isNotBlank()) add(Block("${card.headline} ${card.headlineUnit}".trim(), 56f, true, card.accent))
            if (card.summary.isNotBlank()) add(Block(card.summary, 26f, false, 0xFFF2ECFF.toInt()))
            card.rows.forEach { (label, value) ->
                add(Block(label, 22f, true, 0xFFC8BEE8.toInt()))
                add(Block(value, 26f, false, 0xFFF2ECFF.toInt()))
            }
        }
        val lines = blocks.map { block -> block to wrap(paint(block.size, block.bold), block.text, 524f) }
        val requiredHeight = 110f + lines.sumOf { (block, wrapped) -> (wrapped.size * (block.size + 14f) + 18f).toDouble() }
        require(requiredHeight <= 12_000) { "share_content_exceeds_image_limit" }
        val height = maxOf(MIN_HEIGHT, kotlin.math.ceil(requiredHeight).toInt())
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), height.toFloat(),
                0xFF241943.toInt(), 0xFF171029.toInt(), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), 44f, 44f, background)
        drawGlow(canvas)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x47E9D8A6.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(18f, 18f, 602f, height - 18f, 36f, 36f, border)
        var cursor = 48f
        lines.forEach { (block, wrapped) ->
            wrapped.forEach { line ->
                cursor += block.size + 14f
                text(canvas, line, 48f, cursor, block.size, block.bold, block.color)
            }
            cursor += 18f
        }
        text(canvas, "玄星 · 本地推算 · 仅供娱乐参考", 48f, height - 38f, size = 22f, color = 0xFFAAA0BA.toInt())
        return bitmap
    }

    fun shareImage(context: Context, card: ShareCard): File {
        val directory = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val output = File(directory, "${java.util.UUID.randomUUID()}.png")
        val bitmap = render(card)
        try {
            output.outputStream().use { stream -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
        } catch (failure: Exception) {
            output.delete() // Only the incomplete file created by this call, never another shared image.
            throw failure
        } finally { bitmap.recycle() }
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
            paragraph.codePoints().toArray().forEach { point ->
                val character = String(Character.toChars(point))
                val next = line + character
                if (paint.measureText(next) > maxWidth && line.isNotEmpty()) {
                    lines.add(line)
                    line = character
                } else {
                    line = next
                }
            }
            if (line.isNotEmpty()) lines.add(line)
        }
        return lines.ifEmpty { listOf("") }
    }
}
