package pl.szczodrzynski.edziennik.core.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import java.util.concurrent.TimeUnit

class LessonReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    companion object {
        const val TAG = "LessonReminderWorker"
        private const val NOTIFICATION_ID = 71001

        fun schedule(app: App, startMillis: Long, subject: String, room: String?) {
            val delay = startMillis - 5 * 60 * 1000L - System.currentTimeMillis()
            if (delay < 20_000L) {
                cancel(app)
                return
            }
            val request = OneTimeWorkRequestBuilder<LessonReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "subject" to subject,
                        "room" to (room ?: ""),
                    ),
                )
                .addTag(TAG)
                .build()
            WorkManager.getInstance(app).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(app: App) {
            WorkManager.getInstance(app).cancelUniqueWork(TAG)
        }
    }

    override fun doWork(): Result {
        val subject = inputData.getString("subject").orEmpty()
        if (subject.isBlank()) return Result.success()
        val room = inputData.getString("room").orEmpty()
        val text = if (room.isNotBlank()) "Za 5 min: $subject • sala $room" else "Za 5 min: $subject"
        val app = applicationContext as? App ?: return Result.success()
        val notification = NotificationCompat.Builder(app, app.notificationManager.data.key)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zaraz lekcja")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }
}
