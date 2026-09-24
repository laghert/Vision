package pl.szczodrzynski.edziennik.ui.agenda.compose

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

enum class AgendaFilter {
    ALL,
    EXAMS,
    QUIZZES,
    HOMEWORK,
}

@Immutable
data class AgendaDayChipUi(
    val dayOfWeek: String,
    val dayNumber: String,
    val dateString: String,
    val isToday: Boolean,
    val hasEvents: Boolean,
    val eventCount: Int,
)

@Immutable
data class AgendaEventItemUi(
    val id: Long,
    val subjectName: String,
    val topic: String,
    val dateLabel: String,
    val timeLabel: String,
    val countdownLabel: String,
    val typeName: String,
    val typeColor: Int?,
    val isHomework: Boolean,
    val isDone: Boolean,
    val teacherName: String?,
    val isFromLastLogin: Boolean = false,
    val isPast: Boolean = false,
)

@Immutable
data class AgendaUiState(
    val isLoading: Boolean = true,
    val selectedFilter: AgendaFilter = AgendaFilter.ALL,
    val selectedDateString: String? = null,
    val filterFromLastLogin: Boolean = false,
    val newSinceLastLoginCount: Int = 0,
    val isCalendarStripExpanded: Boolean = false,
    val days: PersistentList<AgendaDayChipUi> = persistentListOf(),
    val upcomingEvents: PersistentList<AgendaEventItemUi> = persistentListOf(),
    val pastEvents: PersistentList<AgendaEventItemUi> = persistentListOf(),
    val totalCount: Int = 0,
    val examsCount: Int = 0,
    val quizzesCount: Int = 0,
    val homeworkCount: Int = 0,
)
