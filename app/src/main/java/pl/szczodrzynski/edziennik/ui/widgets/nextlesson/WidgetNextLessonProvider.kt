package pl.szczodrzynski.edziennik.ui.widgets.nextlesson

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.pendingIntentFlag
import pl.szczodrzynski.edziennik.ext.putExtras
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.math.ceil

class WidgetNextLessonProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        for (appWidgetId in appWidgetIds) {
            val config = widgetConfigs.getJsonObject(appWidgetId.toString())
                ?.let { app.gson.fromJson(it, WidgetConfig::class.java) } ?: continue
            val views = RemoteViews(
                app.packageName,
                if (config.darkTheme) R.layout.widget_next_lesson_dark else R.layout.widget_next_lesson,
            )
            val snapshot = resolveNextLesson(app, config.profileId)
            if (snapshot == null) {
                views.setTextViewText(R.id.widgetNextLessonLabel, "Plan")
                views.setTextViewText(R.id.widgetNextLessonSubject, "Brak lekcji")
                views.setTextViewText(R.id.widgetNextLessonMeta, "Sprawdź plan w aplikacji")
            } else {
                views.setTextViewText(R.id.widgetNextLessonLabel, snapshot.label)
                views.setTextViewText(R.id.widgetNextLessonSubject, snapshot.subject)
                views.setTextViewText(R.id.widgetNextLessonMeta, snapshot.meta)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtras("fragmentId" to NavTarget.TIMETABLE)
            }
            views.setOnClickPendingIntent(
                R.id.widgetNextLessonRoot,
                PendingIntent.getActivity(context, appWidgetId, openIntent, pendingIntentFlag()),
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        appWidgetIds.forEach { widgetConfigs.remove(it.toString()) }
        app.config.widgetConfigs = widgetConfigs
    }

    private data class Snapshot(val label: String, val subject: String, val meta: String)

    private fun resolveNextLesson(app: App, profileId: Int): Snapshot? {
        val today = Date.getToday()
        val tomorrow = today.clone().stepForward(0, 0, 1)
        val lessons = app.db.timetableDao().getBetweenDatesNow(today, tomorrow)
            .filter { it.profileId == profileId }
            .filter { it.type != Lesson.TYPE_NO_LESSONS && !it.isCancelled }
            .filter { it.displayStartTime != null && it.displayEndTime != null }
            .sortedWith(compareBy({ it.displayDate?.value }, { it.displayStartTime?.value }))
        if (lessons.isEmpty()) return null
        val nowSeconds = Time.fromMillis(Date.getNowInMillis()).inSeconds
        val todayLessons = lessons.filter { it.displayDate?.value == today.value }
        val current = todayLessons.firstOrNull {
            val start = it.displayStartTime!!.inSeconds
            val end = it.displayEndTime!!.inSeconds
            nowSeconds in start until end
        }
        if (current != null) {
            val left = ceil((current.displayEndTime!!.inSeconds - nowSeconds).coerceAtLeast(0) / 60.0).toInt()
            return Snapshot(
                label = "Trwa • $left min",
                subject = current.displaySubjectName.orEmpty().ifBlank { "Lekcja" },
                meta = listOfNotNull(
                    current.displayClassroom?.let { "Sala $it" },
                    "${current.displayStartTime?.stringHM}–${current.displayEndTime?.stringHM}",
                ).joinToString(" • "),
            )
        }
        val upcomingToday = todayLessons.firstOrNull { it.displayStartTime!!.inSeconds > nowSeconds }
        val next = upcomingToday ?: lessons.firstOrNull { it.displayDate?.value == tomorrow.value }
        next ?: return null
        val isTomorrow = next.displayDate?.value == tomorrow.value
        val minutes = if (isTomorrow) {
            null
        } else {
            ceil((next.displayStartTime!!.inSeconds - nowSeconds).coerceAtLeast(0) / 60.0).toInt()
        }
        return Snapshot(
            label = when {
                minutes == null -> "Jutro"
                minutes <= 0 -> "Teraz"
                else -> "Za $minutes min"
            },
            subject = next.displaySubjectName.orEmpty().ifBlank { "Lekcja" },
            meta = listOfNotNull(
                next.displayClassroom?.let { "Sala $it" },
                "${next.displayStartTime?.stringHM}–${next.displayEndTime?.stringHM}",
            ).joinToString(" • "),
        )
    }
}
