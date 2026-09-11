/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.data.config

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.ORDER_BY_DATE_DESC
import pl.szczodrzynski.edziennik.data.config.migration.ConfigMigration11
import pl.szczodrzynski.edziennik.data.config.migration.ConfigMigration14
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.ext.HOUR
import pl.szczodrzynski.edziennik.utils.models.Time

class Config(app: App) : BaseConfig<Config>(app, profileId = null) {

    override val dataVersion = 14
    override val migrations
        get() = mapOf(
            11 to ConfigMigration11(),
            14 to ConfigMigration14(),
        )

    private val profileConfigs: HashMap<Int, ProfileConfig> = hashMapOf()

    operator fun get(profileId: Int): ProfileConfig {
        var config = profileConfigs[profileId]
        if (config == null) {
            config = ProfileConfig(app, profileId, entries)
            profileConfigs[profileId] = config
        }
        config.migrate()
        return config
    }

    val ui by lazy { UI() }
    val sync by lazy { Sync() }
    val timetable by lazy { Timetable() }
    val grades by lazy { Grades() }

    var lastProfileId: Int by config<Int>(0)
    var loginFinished: Boolean by config<Boolean>(false)

    var devMode: Boolean? by config<Boolean?>("debugMode", null)
    var devModePassword: String? by config<String?>(null)
    var enableChucker: Boolean? by config<Boolean?>(null)

    var appInstalledTime: Long by config<Long>(0L)
    var appRateSnackbarTime: Long by config<Long>(0L)
    var lastLogCleanupTime: Long by config<Long>(0L)
    var appVersion: Int by config<Int>(BuildConfig.VERSION_CODE)
    var appVersionCore: Int by config<Int>(0)

    var archiverEnabled: Boolean by config<Boolean>(true)
    var runSync: Boolean by config<Boolean>(false)
    var widgetConfigs: JsonObject by config<JsonObject> { JsonObject() }

    inner class UI {
        var themeColor: Theme by config<Theme>(Theme.DEFAULT)
        var themeType: Theme.Type by config<Theme.Type>(Theme.Type.M3)
        var themeMode: Theme.Mode by config<Theme.Mode>(Theme.Mode.DAYNIGHT)
        var themeNightMode: Boolean? by config<Boolean?>(null)
        var themeBlackMode: Boolean by config<Boolean>(false)

        var language: String? by config<String?>(null)

        var appBackground: String? by config<String?>("appBg", null)
        var headerBackground: String? by config<String?>("headerBg", null)

        var miniMenuVisible: Boolean by config<Boolean>(false)
        var miniMenuButtons: Set<NavTarget> by config(NavTarget.Companion::getDefaultConfig)
        var openDrawerOnBackPressed: Boolean by config<Boolean>(false)

        var bottomSheetOpened: Boolean by config<Boolean>(false)
        var snowfall: Boolean by config<Boolean>(false)
        var eggfall: Boolean by config<Boolean>(false)
    }

    inner class Sync {
        var enabled: Boolean by config<Boolean>("syncEnabled", true)
        var interval: Int by config<Int>("syncInterval", 1 * HOUR.toInt())
        var onlyWifi: Boolean by config<Boolean>("syncOnlyWifi", false)

        var dontShowAppManagerDialog: Boolean by config<Boolean>(false)

        // Quiet Hours
        var quietHoursEnabled: Boolean by config<Boolean>(false)
        var quietHoursStart: Time? by config<Time?>(null)
        var quietHoursEnd: Time? by config<Time?>(null)
        var quietDuringLessons: Boolean by config<Boolean>(false)

        // Provider FCM tokens
        var tokenMobidziennik: String? by config<String?>(null)
        var tokenLibrus: String? by config<String?>(null)
        var tokenVulcan: String? by config<String?>(null)
        var tokenVulcanHebe: String? by config<String?>(null)

        var tokenMobidziennikList: List<Int> by config<List<Int>> { listOf() }
        var tokenLibrusList: List<Int> by config<List<Int>> { listOf() }
        var tokenVulcanList: List<Int> by config<List<Int>> { listOf() }
        var tokenVulcanHebeList: List<Int> by config<List<Int>> { listOf() }
    }

    inner class Timetable {
        var bellSyncMultiplier: Int by config<Int>(0)
        var bellSyncDiff: Time? by config<Time?>(null)
        var countInSeconds: Boolean by config<Boolean>(false)
    }

    inner class Grades {
        var orderBy: Int by config<Int>("gradesOrderBy", ORDER_BY_DATE_DESC)
    }
}
