/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.data.config

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration2
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration3
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration4
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration5
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration6
import pl.szczodrzynski.edziennik.data.db.entity.ConfigEntry
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.UNIVERSITY_AVERAGE_MODE_ECTS
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.YEAR_ALL_GRADES

class ProfileConfig(
    app: App,
    profileId: Int,
    entries: List<ConfigEntry>?,
) : BaseConfig<ProfileConfig>(app, profileId, entries) {

    override val dataVersion = 6
    override val migrations
        get() = mapOf(
            2 to ProfileConfigMigration2(),
            3 to ProfileConfigMigration3(),
            4 to ProfileConfigMigration4(),
            5 to ProfileConfigMigration5(),
            6 to ProfileConfigMigration6(),
        )

    val grades by lazy { Grades() }
    val ui by lazy { UI() }
    val sync by lazy { Sync() }
    val attendance by lazy { Attendance() }

    inner class Grades {
        var averageWithoutWeight: Boolean by config<Boolean>(true)
        var colorMode: Int by config<Int>(COLOR_MODE_WEIGHTED)
        var dontCountEnabled: Boolean by config<Boolean>(false)
        var dontCountGrades: List<String> by config<List<String>> { listOf() }
        var hideImproved: Boolean by config<Boolean>(false)
        var hideNoGrade: Boolean by config<Boolean>(false)
        var hideSticksFromOld: Boolean by config<Boolean>(false)
        var minusValue: Float? by config<Float?>(null)
        var plusValue: Float? by config<Float?>(null)
        var yearAverageMode: Int by config<Int>(YEAR_ALL_GRADES)
        var universityAverageMode: Int by config<Int>(UNIVERSITY_AVERAGE_MODE_ECTS)
        var countEctsInProgress: Boolean by config<Boolean>(false)
    }

    inner class UI {
        var agendaViewType: Int by config<Int>(AGENDA_DEFAULT)
        var agendaCompactMode: Boolean by config<Boolean>(false)
        var agendaGroupByType: Boolean by config<Boolean>(false)
        var agendaLessonChanges: Boolean by config<Boolean>(true)
        var agendaTeacherAbsence: Boolean by config<Boolean>(true)
        var agendaSubjectImportant: Boolean by config<Boolean>(false)
        var agendaElearningMark: Boolean by config<Boolean>(false)
        var agendaElearningGroup: Boolean by config<Boolean>(true)

        var homeCards: List<HomeCardModel> by config<List<HomeCardModel>> { listOf() }

        var messagesGreetingOnCompose: Boolean by config<Boolean>(true)
        var messagesGreetingOnReply: Boolean by config<Boolean>(true)
        var messagesGreetingOnForward: Boolean by config<Boolean>(false)
        var messagesGreetingText: String? by config<String?>(null)

        var timetableShowAttendance: Boolean by config<Boolean>(true)
        var timetableShowEvents: Boolean by config<Boolean>(true)
        var timetableTrimHourRange: Boolean by config<Boolean>(false)
        var timetableColorSubjectName: Boolean by config<Boolean>(false)
    }

    inner class Sync {
        var notificationFilter: Set<NotificationType> by config(NotificationType.Companion::getDefaultConfig)
    }

    inner class Attendance {
        var attendancePageSelection: Int by config<Int>(1)
        var groupConsecutiveDays: Boolean by config<Boolean>(true)
        var showPresenceInMonth: Boolean by config<Boolean>(false)
        var useSymbols: Boolean by config<Boolean>(false)
    }
}
