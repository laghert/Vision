package pl.szczodrzynski.edziennik.ui.timetable

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import pl.szczodrzynski.edziennik.ui.today.TodayLessonStatusKind
import pl.szczodrzynski.edziennik.ui.today.TodayUpcomingKind
import pl.szczodrzynski.edziennik.utils.models.Date

@Immutable
data class TimetableUiState(
    val isLoading: Boolean = true,
    val selectedDate: Date = Date.getToday(),
    val weekTitle: String = "",
    val isTodaySelected: Boolean = false,
    val days: PersistentList<TimetableDayUi> = persistentListOf(),
)

@Immutable
data class TimetableDayUi(
    val date: Date,
    val dayOfWeekShort: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasChanges: Boolean = false,
    val hasExams: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val isFreeDay: Boolean = false,
    val currentLessonIndex: Int = -1,
    val items: PersistentList<TimetableItemUi> = persistentListOf(),
    val preview: PersistentList<TimetableLessonPreviewUi> = persistentListOf(),
)

@Immutable
data class TimetableLessonPreviewUi(
    val startTime: String,
    val subject: String,
    val isCancelled: Boolean = false,
    val isChanged: Boolean = false,
    val isCurrent: Boolean = false,
)

sealed interface TimetableItemUi {
    data class Lesson(
        val id: Long,
        val number: Int?,
        val subject: String,
        val originalSubject: String? = null,
        val classroom: String?,
        val teacher: String?,
        val originalTeacher: String? = null,
        val topic: String? = null,
        val changeType: String? = null,
        val changeReason: String? = null,
        val groupName: String? = null,
        val startTime: String,
        val endTime: String,
        val startSeconds: Long,
        val endSeconds: Long,
        val statusKind: TodayLessonStatusKind? = null,
        val statusDetails: String? = null,
        val isCurrent: Boolean = false,
        val progress: Float = 0f,
        val minutesRemaining: Int = 0,
        val colorArgb: Int = 0,
        val notePreview: String? = null,
        val events: List<TimetableLinkedEventUi> = emptyList(),
    ) : TimetableItemUi

    data class Break(
        val durationMinutes: Int,
        val startTime: String,
        val endTime: String,
        val isCurrent: Boolean = false,
    ) : TimetableItemUi
}

@Immutable
data class TimetableLinkedEventUi(
    val id: Long,
    val title: String,
    val kind: TodayUpcomingKind,
)
