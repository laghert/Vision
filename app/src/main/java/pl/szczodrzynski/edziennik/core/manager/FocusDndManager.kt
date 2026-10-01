package pl.szczodrzynski.edziennik.core.manager

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import pl.szczodrzynski.edziennik.App
import timber.log.Timber

class FocusDndManager(private val app: App) {

    private val notificationManager by lazy { app.getSystemService<NotificationManager>() }
    private val audioManager by lazy { app.getSystemService<AudioManager>() }

    fun hasDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun applyLessonSilence(isLessonActive: Boolean) {
        if (!app.config.security.dndDuringLessonsEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted == true) {
                if (isLessonActive) {
                    notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    Timber.d("DND activated for ongoing lesson")
                } else {
                    notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    Timber.d("DND restored after lesson")
                }
            } else {
                // Fallback to ringer mode vibrate / normal
                audioManager?.let { audio ->
                    if (isLessonActive && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply DND / silence mode")
        }
    }
}
