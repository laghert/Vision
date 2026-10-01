package pl.szczodrzynski.edziennik.core.telemetry

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * Anonymized, privacy-first telemetry for Vision.
 * Collects zero PII: no student data, no credentials, no grades, no school names.
 * Respects user opt-out in settings immediately.
 */
object VisionTelemetry {
    private const val TAG = "VisionTelemetry"
    private const val ENDPOINT = "https://laghert.pl/api/telemetry/vision"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
    private val gson = Gson()
    private val pendingEvents = ConcurrentLinkedQueue<Map<String, Any>>()

    @Volatile
    private var initialized = false
    private lateinit var appInstance: App

    fun init(app: App) {
        appInstance = app
        initialized = true

        // Ensure anonymous installation ID exists
        if (app.config.telemetryInstallationId.isNullOrBlank()) {
            app.config.telemetryInstallationId = UUID.randomUUID().toString()
        }

        // Send app launch event
        recordEvent("app_launch")
        flush()
    }

    fun isEnabled(): Boolean {
        if (!initialized) return false
        return appInstance.config.telemetryEnabled
    }

    fun recordEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        if (!isEnabled()) return

        val event = mutableMapOf<String, Any>(
            "name" to eventName,
            "timestamp" to System.currentTimeMillis(),
        )
        if (params.isNotEmpty()) {
            event["params"] = params
        }
        pendingEvents.add(event)

        if (pendingEvents.size >= 5) {
            flush()
        }
    }

    fun flush() {
        if (!isEnabled()) {
            pendingEvents.clear()
            return
        }

        scope.launch {
            try {
                if (pendingEvents.isEmpty()) return@launch

                val eventsToSend = mutableListOf<Map<String, Any>>()
                while (true) {
                    val ev = pendingEvents.poll() ?: break
                    eventsToSend.add(ev)
                    if (eventsToSend.size >= 50) break
                }
                if (eventsToSend.isEmpty()) return@launch

                val payload = JsonObject().apply {
                    addProperty("installation_id", appInstance.config.telemetryInstallationId)
                    addProperty("app_version", BuildConfig.VERSION_NAME)
                    addProperty("app_version_code", BuildConfig.VERSION_CODE)
                    addProperty("android_sdk", Build.VERSION.SDK_INT)
                    addProperty("device_brand", Build.BRAND)
                    addProperty("device_model", Build.MODEL)
                    addProperty("timestamp", System.currentTimeMillis())
                    add("events", gson.toJsonTree(eventsToSend))
                }

                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString())
                val request = Request.Builder()
                    .url(ENDPOINT)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Telemetry dispatch HTTP ${response.code()}")
                    }
                }
            } catch (t: Throwable) {
                // Completely silent - telemetry must never affect user experience
                Log.d(TAG, "Telemetry dispatch caught exception: ${t.message}")
            }
        }
    }
}
