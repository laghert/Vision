package pl.szczodrzynski.edziennik.ui.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.agenda.AgendaFragment
import pl.szczodrzynski.edziennik.ui.announcements.AnnouncementsFragment
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceFragment
import pl.szczodrzynski.edziennik.ui.behaviour.BehaviourFragment
import pl.szczodrzynski.edziennik.ui.debug.DebugFragment
import pl.szczodrzynski.edziennik.ui.debug.LabFragment
import pl.szczodrzynski.edziennik.ui.grades.GradesListFragment
import pl.szczodrzynski.edziennik.ui.grades.editor.GradesEditorFragment
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.ui.homework.HomeworkFragment
import pl.szczodrzynski.edziennik.ui.messages.compose.MessagesComposeFragment
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.ui.messages.single.MessageFragment
import pl.szczodrzynski.edziennik.ui.notes.NotesFragment
import pl.szczodrzynski.edziennik.ui.notifications.NotificationsListFragment
import pl.szczodrzynski.edziennik.ui.settings.ProfileManagerFragment
import pl.szczodrzynski.edziennik.ui.settings.SettingsFragment
import pl.szczodrzynski.edziennik.ui.teachers.TeachersListFragment
import pl.szczodrzynski.edziennik.ui.template.TemplateFragment
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment

/** Keeps Fragment construction explicit and out of navigation metadata. */
internal fun NavTarget.createFragment(arguments: Bundle?): Fragment? {
    val fragment = when (this) {
        NavTarget.HOME -> HomeFragment()
        NavTarget.TIMETABLE -> TimetableFragment()
        NavTarget.AGENDA -> AgendaFragment()
        NavTarget.GRADES -> GradesListFragment()
        NavTarget.MESSAGES -> MessagesFragment()
        NavTarget.HOMEWORK -> HomeworkFragment()
        NavTarget.BEHAVIOUR -> BehaviourFragment()
        NavTarget.ATTENDANCE -> AttendanceFragment()
        NavTarget.ANNOUNCEMENTS -> AnnouncementsFragment()
        NavTarget.NOTES -> NotesFragment()
        NavTarget.TEACHERS -> TeachersListFragment()
        NavTarget.NOTIFICATIONS -> NotificationsListFragment()
        NavTarget.SETTINGS -> SettingsFragment()
        NavTarget.LAB -> LabFragment()
        NavTarget.TEMPLATE -> TemplateFragment()
        NavTarget.PROFILE_MANAGER -> ProfileManagerFragment()
        NavTarget.DEBUG -> DebugFragment()
        NavTarget.GRADES_EDITOR -> GradesEditorFragment()
        NavTarget.MESSAGE -> MessageFragment()
        NavTarget.MESSAGE_COMPOSE -> MessagesComposeFragment()
        NavTarget.PROFILE_ADD,
        NavTarget.PROFILE_MARK_AS_READ,
        NavTarget.PROFILE_SYNC_ALL,
        -> null
    }
    return fragment?.apply { this.arguments = arguments ?: Bundle() }
}
