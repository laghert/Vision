package pl.szczodrzynski.edziennik.ui.today

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TodayUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val profileIncomplete: Boolean = false,
    val userName: String = "",
    val greetingTitle: String = "",
    val heroDateLabel: String = "",
    val dayOfWeekLabel: String = "",
    val dateLabel: String = "",
    val lastSyncMinutes: Long? = null,
    val hero: TodayHeroUi = TodayHeroUi(),
    val nextLessons: PersistentList<TodayLessonUi> = persistentListOf(),
    val attention: TodayAttentionUi = TodayAttentionUi(),
    val upcoming: PersistentList<TodayUpcomingUi> = persistentListOf(),
    val luckyNumber: Int? = null,
    val isUserLuckyNumber: Boolean = false,
    val changesCount: Int = 0,
    val recentGrades: PersistentList<TodayRecentGradeUi> = persistentListOf(),
    val tomorrowPreview: TodayTomorrowPreviewUi? = null,
    val tomorrowLessons: PersistentList<TodayLessonUi> = persistentListOf(),
    val homeworkList: PersistentList<TodayHomeworkUi> = persistentListOf(),
    val isDemoProfile: Boolean = false,
    val simulatedDayTitle: String? = null,
    val bellBar: TodayBellBarUi? = null,
    val fridayFinish: Boolean = false,
    val cardOrder: List<String> = listOf("homework", "upcoming", "grades"),
)

@Immutable
data class TodayHomeworkUi(
    val id: Long,
    val subjectName: String,
    val topic: String,
    val dueDateLabel: String,
    val isDone: Boolean = false,
)

@Immutable
data class TodayRecentGradeUi(
    val id: Long,
    val gradeValue: String,
    val subjectName: String,
    val categoryName: String,
    val weight: Float,
    val color: Int?,
    val dateString: String,
)

@Immutable
data class TodayTomorrowPreviewUi(
    val dayOfWeek: String,
    val dateLabel: String,
    val lessonCount: Int,
    val firstLessonSubject: String,
    val firstLessonTime: String,
    val firstLessonRoom: String?,
    val examCount: Int = 0,
    val endsAt: String? = null,
    val shareText: String = "",
)

@Immutable
data class TodayBellBarUi(
    val subject: String,
    val minutesRemaining: Int,
    val room: String?,
    val isBreak: Boolean,
)

@Immutable
data class TodayHeroUi(
    val phase: TodayPhase = TodayPhase.FREE_DAY,
    val lesson: TodayLessonUi? = null,
    val minutesRemaining: Int = 0,
    val progress: Float = 0f,
    val scheduleDayOfWeekLabel: String? = null,
    val scheduleDateLabel: String? = null,
    val lessonCount: Int = 0,
    val dayStartsAt: String? = null,
    val dayEndsAt: String? = null,
)

@Immutable
data class TodayLessonUi(
    val id: Long,
    val subject: String,
    val room: String?,
    val teacher: String?,
    val startsAt: String,
    val endsAt: String,
    val status: TodayLessonStatusUi? = null,
)

@Immutable
data class TodayLessonStatusUi(
    val kind: TodayLessonStatusKind,
    val details: String? = null,
)

@Immutable
data class TodayAttentionUi(
    val newGrades: Int = 0,
    val unreadMessages: Int = 0,
    val newAnnouncements: Int = 0,
    val newNotices: Int = 0,
    val homeworkTomorrow: Int = 0,
    val enabledGrades: Boolean = true,
    val enabledMessages: Boolean = true,
    val enabledAnnouncements: Boolean = true,
    val enabledNotices: Boolean = true,
    val enabledHomework: Boolean = true,
) {
    val total: Int
        get() = (if (enabledGrades) newGrades else 0) +
            (if (enabledMessages) unreadMessages else 0) +
            (if (enabledAnnouncements) newAnnouncements else 0) +
            (if (enabledNotices) newNotices else 0) +
            (if (enabledHomework) homeworkTomorrow else 0)

    val hasItems: Boolean
        get() = total > 0
}

@Immutable
data class TodayUpcomingUi(
    val id: Long,
    val title: String,
    val supportingText: String?,
    val dateLabel: String,
    val relativeDateLabel: String = "",
    val kind: TodayUpcomingKind,
)

enum class TodayPhase {
    BEFORE_CLASSES,
    IN_CLASS,
    BREAK,
    AFTER_CLASSES,
    NEXT_CLASSES,
    FREE_DAY,
}

enum class TodayLessonStatusKind {
    CANCELLED,
    CHANGED,
    SHIFTED,
}

enum class TodayUpcomingKind {
    EXAM,
    QUIZ,
    PROJECT,
    DEADLINE,
}
