/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Announcement
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector
import pl.szczodrzynski.edziennik.R
import java.util.Locale

/**
 * Stable navigation metadata shared by intents, shortcuts and the Compose shell.
 * Fragment construction deliberately lives in LegacyFragmentFactory.
 */
enum class NavTarget(
    val id: Int,
    val location: NavTargetLocation = NavTargetLocation.NOWHERE,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int? = null,
    @StringRes val titleRes: Int? = null,
    val icon: ImageVector? = null,
    val popTo: NavTarget? = null,
    val badgeType: MetadataType? = null,
    val featureType: FeatureType? = null,
    val devModeOnly: Boolean = false,
) {
    HOME(
        id = 1,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_home_page,
        titleRes = R.string.app_name,
        icon = Icons.Outlined.Home,
    ),
    TIMETABLE(
        id = 11,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_timetable,
        icon = Icons.Outlined.CalendarMonth,
        popTo = HOME,
        badgeType = MetadataType.LESSON_CHANGE,
        featureType = FeatureType.TIMETABLE,
    ),
    AGENDA(
        id = 12,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_agenda,
        icon = Icons.Outlined.ViewAgenda,
        popTo = HOME,
        badgeType = MetadataType.EVENT,
        featureType = FeatureType.AGENDA,
    ),
    GRADES(
        id = 13,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_grades,
        icon = Icons.Outlined.Grade,
        popTo = HOME,
        badgeType = MetadataType.GRADE,
        featureType = FeatureType.GRADES,
    ),
    MESSAGES(
        id = 17,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_messages,
        icon = Icons.Outlined.MailOutline,
        popTo = HOME,
        badgeType = MetadataType.MESSAGE,
        featureType = FeatureType.MESSAGES_INBOX,
    ),
    HOMEWORK(
        id = 14,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_homework,
        icon = Icons.Outlined.Assignment,
        popTo = HOME,
        badgeType = MetadataType.HOMEWORK,
        featureType = FeatureType.HOMEWORK,
    ),
    BEHAVIOUR(
        id = 15,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_notices,
        icon = Icons.Outlined.SentimentSatisfied,
        popTo = HOME,
        badgeType = MetadataType.NOTICE,
        featureType = FeatureType.BEHAVIOUR,
    ),
    ATTENDANCE(
        id = 16,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_attendance,
        icon = Icons.Outlined.EventBusy,
        popTo = HOME,
        badgeType = MetadataType.ATTENDANCE,
        featureType = FeatureType.ATTENDANCE,
    ),
    ANNOUNCEMENTS(
        id = 18,
        location = NavTargetLocation.DRAWER,
        nameRes = R.string.menu_announcements,
        icon = Icons.Outlined.Announcement,
        popTo = HOME,
        badgeType = MetadataType.ANNOUNCEMENT,
        featureType = FeatureType.ANNOUNCEMENTS,
    ),
    NOTES(
        id = 23,
        location = NavTargetLocation.DRAWER_MORE,
        nameRes = R.string.menu_notes,
        icon = Icons.Outlined.EditNote,
    ),
    TEACHERS(
        id = 22,
        location = NavTargetLocation.DRAWER_MORE,
        nameRes = R.string.menu_teachers,
        icon = Icons.Outlined.School,
    ),
    NOTIFICATIONS(
        id = 20,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_notifications,
        icon = Icons.Outlined.Notifications,
        popTo = HOME,
    ),
    SETTINGS(
        id = 101,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_settings,
        icon = Icons.Outlined.Settings,
    ),
    LAB(
        id = 1000,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_lab,
        icon = Icons.Outlined.Science,
        popTo = HOME,
        devModeOnly = true,
    ),
    TEMPLATE(
        id = 1001,
        location = NavTargetLocation.DRAWER_BOTTOM,
        nameRes = R.string.menu_template,
        icon = Icons.Outlined.Code,
        popTo = HOME,
        devModeOnly = true,
    ),
    PROFILE_ADD(
        id = 200,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_add_new_profile,
        descriptionRes = R.string.drawer_add_new_profile_desc,
        icon = Icons.Outlined.PersonAdd,
    ),
    PROFILE_MANAGER(
        id = 203,
        nameRes = R.string.menu_manage_profiles,
        titleRes = R.string.title_profile_manager,
        descriptionRes = R.string.drawer_manage_profiles_desc,
        icon = Icons.Outlined.ManageAccounts,
    ),
    PROFILE_MARK_AS_READ(
        id = 204,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_mark_everything_as_read,
        icon = Icons.Outlined.DoneAll,
    ),
    PROFILE_SYNC_ALL(
        id = 201,
        location = NavTargetLocation.PROFILE_LIST,
        nameRes = R.string.menu_sync_all,
        icon = Icons.Outlined.Sync,
    ),
    DEBUG(
        id = 102,
        location = NavTargetLocation.BOTTOM_SHEET,
        nameRes = R.string.menu_debug,
        icon = Icons.Outlined.BugReport,
        devModeOnly = true,
    ),
    GRADES_EDITOR(
        id = 501,
        nameRes = R.string.menu_grades_editor,
        icon = Icons.Outlined.EditNote,
    ),
    MESSAGE(
        id = 503,
        nameRes = R.string.menu_message,
        icon = Icons.Outlined.MailOutline,
        popTo = MESSAGES,
    ),
    MESSAGE_COMPOSE(
        id = 504,
        nameRes = R.string.menu_message_compose,
        icon = Icons.Outlined.Add,
    );

    val route: String
        get() = "target/${name.lowercase(Locale.ROOT)}"

    companion object {
        fun getDefaultConfig() = setOf(
            HOME,
            TIMETABLE,
            AGENDA,
            GRADES,
            MESSAGES,
            HOMEWORK,
            SETTINGS,
        )

        fun getById(id: Int) = entries.first { it.id == id }
    }
}
