package pl.szczodrzynski.edziennik.core.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.NavTarget

object LiveLessonNotificationManager {
    private const val LIVE_NOTIFICATION_ID = 71002

    fun showLiveLessonNotification(
        context: Context,
        subject: String,
        room: String?,
        remainingMinutes: Int,
        progressPercent: Int,
    ) {
        val app = context.applicationContext as? App ?: return
        val channelId = app.notificationManager.data.key

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("fragmentId", NavTarget.TIMETABLE)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LIVE_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomText = if (!room.isNullOrBlank()) " • Sala $room" else ""
        val subtitle = if (remainingMinutes > 0) "Koniec za $remainingMinutes min$roomText" else "Koniec lekcji$roomText"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Trwa lekcja: $subject")
            .setContentText(subtitle)
            .setSubText("Na żywo")
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(LIVE_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(LIVE_NOTIFICATION_ID)
    }
}
