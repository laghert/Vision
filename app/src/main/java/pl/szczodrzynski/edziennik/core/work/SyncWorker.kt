package pl.szczodrzynski.edziennik.core.work

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.ext.formatDate
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncWorker(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val TAG = "SyncWorker"

        /**
         * Schedule the sync job only if it's not already scheduled.
         */
        @SuppressLint("RestrictedApi")
        fun scheduleNext(app: App, rescheduleIfFailedFound: Boolean = true) {
            WorkerUtils.scheduleNext(app, rescheduleIfFailedFound) {
                rescheduleNext(app)
            }
        }

        /**
         * Cancel any existing sync jobs and schedule a new one.
         *
         * If [ConfigSync.enabled] is not true, just cancel every job.
         */
        fun rescheduleNext(app: App) {
            cancelNext(app)
            val enableSync = app.config.sync.enabled
            if (!enableSync) {
                return
            }
            val onlyWifi = app.config.sync.onlyWifi
            val syncInterval = calculateSmartSyncInterval(app)

            val syncAt = System.currentTimeMillis() + syncInterval * 1000
            Timber.d("Scheduling work in ${syncInterval / 60} min at ${syncAt.formatDate()}")

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(
                            if (onlyWifi)
                                NetworkType.UNMETERED
                            else
                                NetworkType.CONNECTED)
                    .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setInitialDelay(syncInterval, TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(app).enqueue(syncWorkRequest)
        }

        /**
         * Computes smart sync interval in seconds based on school windows:
         * - 06:30 - 21:00: 15 minutes (active daytime: substitutions, grades, events)
         * - 21:00 - 23:00: 30 minutes (late evening)
         * - 23:00 - 06:30: sleep until 06:30 (night quiet)
         * - Weekends: 2 hours (120 minutes)
         */
        fun calculateSmartSyncInterval(app: App): Long {
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY

            return when {
                isWeekend -> 2 * 3600L
                hour in 7..20 -> 15 * 60L // Active school and homework window: 15 minutes
                hour in 21..22 -> 30 * 60L // Late evening window: 30 minutes
                else -> {
                    // 23:00 - 06:30 night window: sleep until 6:30 AM!
                    val target = java.util.Calendar.getInstance().apply {
                        if (hour >= 23) add(java.util.Calendar.DAY_OF_MONTH, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 6)
                        set(java.util.Calendar.MINUTE, 30)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    val diff = (target.timeInMillis - System.currentTimeMillis()) / 1000
                    diff.coerceIn(15 * 60L, 8 * 3600L)
                }
            }
        }


        /**
         * Cancel any scheduled sync job.
         */
        fun cancelNext(app: App) {
            Timber.d("Cancelling work by tag $TAG")
            WorkManager.getInstance(app).cancelAllWorkByTag(TAG)
            //WorkManager.getInstance(app).pruneWork() // do not prune the work in order to look for failed tasks
        }
    }

    override fun doWork(): Result {
        Timber.d("Running worker ID ${params.id}")
        EdziennikTask.sync().enqueue(context)
        rescheduleNext(context as App)
        return Result.success()
    }
}
