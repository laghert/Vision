package pl.szczodrzynski.edziennik.ui.timetable

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import pl.szczodrzynski.edziennik.R
import java.io.File

object TimetableShare {
    fun shareWeek(context: Context, state: TimetableUiState) {
        val bitmap = renderWeek(state)
        val file = File(context.cacheDir, "plan-lekcji.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_intent)))
    }

    private fun renderWeek(state: TimetableUiState): Bitmap {
        val days = state.days
        val colWidth = 220f
        val headerHeight = 160f
        val rowHeight = 72f
        val padding = 36f
        val maxLessons = days.maxOfOrNull { it.preview.size }?.coerceAtLeast(1) ?: 1
        val width = (padding * 2 + days.size * colWidth).toInt().coerceAtLeast(720)
        val height = (padding * 2 + headerHeight + maxLessons * rowHeight + 48f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF7F4EE.toInt() }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 0f, 0f, bg)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1B16.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5F5E5A.toInt()
            textSize = 26f
        }
        canvas.drawText("Plan lekcji", padding, padding + 44f, titlePaint)
        canvas.drawText(state.weekTitle.ifBlank { "Ten tydzień" }, padding, padding + 86f, subtitlePaint)

        days.forEachIndexed { index, day ->
            val left = padding + index * colWidth
            val top = padding + headerHeight
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (day.isToday) 0xFF2F6FED.toInt() else 0xFF1C1B16.toInt()
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(day.dayOfWeekShort, left, top - 28f, headerPaint)
            canvas.drawText(day.dayOfMonth.toString(), left, top - 2f, subtitlePaint)

            if (day.preview.isEmpty()) {
                canvas.drawText("Wolne", left, top + 40f, subtitlePaint)
            } else {
                day.preview.forEachIndexed { lessonIndex, lesson ->
                    val y = top + 16f + lessonIndex * rowHeight
                    val card = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = when {
                            lesson.isCancelled -> 0x33B3261E
                            lesson.isChanged -> 0x337D5700
                            lesson.isCurrent -> 0x332F6FED
                            else -> 0xFFFFFFFF.toInt()
                        }
                    }
                    canvas.drawRoundRect(RectF(left, y, left + colWidth - 16f, y + rowHeight - 10f), 16f, 16f, card)
                    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF5F5E5A.toInt()
                        textSize = 18f
                    }
                    val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (lesson.isCancelled) 0xFFB3261E.toInt() else 0xFF1C1B16.toInt()
                        textSize = 22f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isStrikeThruText = lesson.isCancelled
                    }
                    canvas.drawText(lesson.startTime, left + 12f, y + 24f, timePaint)
                    canvas.drawText(lesson.subject.take(16), left + 12f, y + 50f, subjectPaint)
                }
            }
        }
        return bitmap
    }
}
