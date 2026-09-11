/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.core.manager

import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.MS
import pl.szczodrzynski.edziennik.ext.asBoldSpannable
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Local build metadata without certificate or remote-repository validation. */
class BuildManager(private val app: App) {
    val buildFlavor = BuildConfig.FLAVOR
    val buildType = BuildConfig.BUILD_TYPE
    val isRelease = !BuildConfig.DEBUG
    val isDebug = BuildConfig.DEBUG
    val isNightly = BuildConfig.VERSION_NAME.contains("nightly")
    val isDaily = BuildConfig.VERSION_NAME.contains("daily")

    val buildTimestamp: Long
        get() {
            val info = app.packageManager.getApplicationInfo(
                app.packageName,
                PackageManager.GET_META_DATA,
            )
            return info.metaData?.getString("buildTimestamp")?.toLongOrNull() ?: 0L
        }

    val gitHash = BuildConfig.GIT_INFO["hash"]
    val gitVersion = BuildConfig.GIT_INFO["version"]
    val gitBranch = BuildConfig.GIT_INFO["branch"]
    val gitUnstaged = BuildConfig.GIT_INFO["unstaged"]?.split("; ")
    val gitRevCount = BuildConfig.GIT_INFO["revCount"]
    val gitTag = BuildConfig.GIT_INFO["tag"]
    val gitRemotes = BuildConfig.GIT_INFO["remotes"]?.split("; ")

    val versionName = BuildConfig.VERSION_NAME

    val versionBadge = when {
        isNightly -> "Nightly\n${BuildConfig.VERSION_NAME.substringAfterLast('.')}"
        isDaily -> "Daily\n${BuildConfig.VERSION_NAME.substringAfterLast('.')}"
        isDebug -> "Debug\n${BuildConfig.VERSION_BASE}"
        else -> null
    }

    val devModeEasy = (isNightly || isDebug) && !App.devMode

    fun fetchInstalledTime() {
        if (app.config.appInstalledTime != 0L) return
        try {
            app.config.appInstalledTime =
                app.packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
            app.config.appRateSnackbarTime = app.config.appInstalledTime + 7 * DAY * MS
        } catch (exception: PackageManager.NameNotFoundException) {
            Timber.e(exception)
        }
    }

    fun showVersionDialog(activity: AppCompatActivity) {
        val colorOnBackground = R.attr.colorOnBackground.resolveAttr(activity)
        val fields = mapOf(
            R.string.build_version to BuildConfig.VERSION_BASE,
            R.string.build_platform to buildFlavor,
            R.string.build_date to ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(buildTimestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.RFC_1123_DATE_TIME),
            R.string.build_branch to gitBranch,
            R.string.build_commit to gitHash?.take(8),
            R.string.build_dirty to (gitUnstaged?.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "-"),
            R.string.build_tag to gitTag,
            R.string.build_rev_count to gitRevCount,
            R.string.build_remote to gitRemotes?.joinToString("\n"),
        )

        val message = fields.map { (key, value) ->
            TextUtils.concat(
                activity.getString(key).asBoldSpannable().asColoredSpannable(colorOnBackground),
                ":\n",
                value,
            )
        }.concat("\n\n")

        SimpleDialog<Unit>(activity) {
            title(R.string.build_details)
            message(message)
            positive(R.string.ok, null)
            show()
        }
    }

    /** Certificate/backend validation was removed for the standalone build. */
    fun validateBuild(@Suppress("UNUSED_PARAMETER") activity: AppCompatActivity) = Unit
}
