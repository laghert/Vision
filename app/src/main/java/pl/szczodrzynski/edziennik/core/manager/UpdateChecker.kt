package pl.szczodrzynski.edziennik.core.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
)

class UpdateChecker(private val app: App) {

    suspend fun checkLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/laghert/Vision/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Vision-App")
                connectTimeout = 8_000
                readTimeout = 8_000
            }

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val tagName = root.optString("tag_name", "")
                val name = root.optString("name", tagName)
                val body = root.optString("body", "")
                val htmlUrl = root.optString("html_url", "https://github.com/laghert/Vision/releases/latest")

                var apkUrl: String? = null
                val assets = root.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", null)
                            break
                        }
                    }
                }

                return@withContext GitHubRelease(
                    tagName = tagName,
                    name = name,
                    body = body,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not check GitHub releases")
        }
        return@withContext null
    }

    fun isNewerVersion(releaseTag: String): Boolean {
        val current = BuildConfig.VERSION_NAME.removePrefix("v").trim()
        val latest = releaseTag.removePrefix("v").trim()
        return latest.isNotBlank() && latest != current
    }

    fun openReleasePage(context: Context, release: GitHubRelease) {
        val targetUrl = release.apkDownloadUrl ?: release.htmlUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
