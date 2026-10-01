package pl.szczodrzynski.edziennik.core.manager

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import pl.szczodrzynski.edziennik.App
import timber.log.Timber

class BiometricLockManager(private val app: App) {

    private var isUnlockedForSession: Boolean = false

    val isConfiguredEnabled: Boolean
        get() = app.config.security.biometricLockEnabled

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(app)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isUnlockRequired(): Boolean {
        if (!isConfiguredEnabled) return false
        return !isUnlockedForSession
    }

    fun markUnlocked() {
        isUnlockedForSession = true
    }

    fun markLocked() {
        isUnlockedForSession = false
    }

    fun requestUnlock(
        activity: FragmentActivity,
        title: String = "Odblokuj Vision",
        subtitle: String = "Potwierdź swoją tożsamość, aby uzyskać dostęp",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
    ) {
        if (!isUnlockRequired()) {
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    markUnlocked()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.w("Biometric auth error: $errorCode, $errString")
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onCancel?.invoke()
                    } else {
                        onError?.invoke(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError?.invoke("Nie rozpoznano biometrii")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
