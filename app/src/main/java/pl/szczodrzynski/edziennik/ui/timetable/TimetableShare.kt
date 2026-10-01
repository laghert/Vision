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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    fun exportIcs(context: Context, state: TimetableUiState) {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Vision//Plan Lekcji//PL\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")

        val nowStr = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        state.days.forEach { day ->
            val dateStr = String.format(Locale.US, "%04d%02d%02d", day.date.year, day.date.month, day.date.day)
            day.items.filterIsInstance<TimetableItemUi.Lesson>().forEach { lesson ->
                val startParts = lesson.startTime.split(":")
                val endParts = lesson.endTime.split(":")
                if (startParts.size >= 2 && endParts.size >= 2) {
                    val dtStart = String.format(
                        Locale.US,
                        "%sT%02d%02d00",
                        dateStr,
                        startParts[0].toIntOrNull() ?: 8,
                        startParts[1].toIntOrNull() ?: 0
                    )
                    val dtEnd = String.format(
                        Locale.US,
                        "%sT%02d%02d00",
                        dateStr,
                        endParts[0].toIntOrNull() ?: 8,
                        endParts[1].toIntOrNull() ?: 45
                    )
                    sb.append("BEGIN:VEVENT\r\n")
                    sb.append("UID:vision-${lesson.id}-${dateStr}@vision.app\r\n")
                    sb.append("DTSTAMP:").append(nowStr).append("\r\n")
                    sb.append("DTSTART:").append(dtStart).append("\r\n")
                    sb.append("DTEND:").append(dtEnd).append("\r\n")
                    sb.append("SUMMARY:").append(lesson.subject.replace(",", "\\,")).append("\r\n")
                    lesson.classroom?.let {
                        sb.append("LOCATION:Sala ").append(it.replace(",", "\\,")).append("\r\n")
                    }
                    val desc = buildString {
                        lesson.teacher?.let { append("Nauczyciel: ").append(it).append("\\n") }
                        lesson.topic?.let { append("Temat: ").append(it).append("\\n") }
                    }
                    if (desc.isNotEmpty()) {
                        sb.append("DESCRIPTION:").append(desc).append("\r\n")
                    }
                    sb.append("END:VEVENT\r\n")
                }
            }
        }
        sb.append("END:VCALENDAR\r\n")

        val file = File(context.cacheDir, "plan-lekcji.ics")
        file.writeText(sb.toString(), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Plan lekcji Vision (.ics)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Eksportuj plan do kalendarza (.ics)"))
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
