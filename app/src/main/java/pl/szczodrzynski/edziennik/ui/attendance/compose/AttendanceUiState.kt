package pl.szczodrzynski.edziennik.ui.attendance.compose

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AttendanceLessonUi(
    val id: Long,
    val subjectName: String,
    val timeLabel: String,
    val typeName: String,
    val typeColor: Int?,
    val isPresent: Boolean,
    val isExcused: Boolean,
    val isLate: Boolean,
    val isAbsent: Boolean,
)

@Immutable
data class AttendanceDayUi(
    val dateLabel: String,
    val dayOfWeek: String,
    val isToday: Boolean,
    val lessons: PersistentList<AttendanceLessonUi>,
)

@Immutable
data class AttendanceUiState(
    val isLoading: Boolean = true,
    val overallPercentage: Float = 100f,
    val totalLessons: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val excusedCount: Int = 0,
    val lateCount: Int = 0,
    val releasedCount: Int = 0,
    val streakDays: Int = 0,
    val statusMessage: String = "",
    val historyByDay: PersistentList<AttendanceDayUi> = persistentListOf(),
)
