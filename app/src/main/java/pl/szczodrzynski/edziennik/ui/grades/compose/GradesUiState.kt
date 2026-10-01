package pl.szczodrzynski.edziennik.ui.grades.compose

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

enum class GradesSemesterTab(val title: String) {
    SEMESTER_1("Semestr 1"),
    SEMESTER_2("Semestr 2"),
    YEAR("Roczne"),
}

@Immutable
data class GradesUiState(
    val isLoading: Boolean = true,
    val selectedSemester: GradesSemesterTab = GradesSemesterTab.SEMESTER_1,
    val overallAverage: Float? = null,
    val redStripeProgress: Float = 0f,
    val redStripeDiff: Float? = null,
    val gradeDistribution: Map<Int, Int> = emptyMap(),
    val averageHistoryPoints: List<Float> = emptyList(),
    val subjects: PersistentList<GradeSubjectUi> = persistentListOf(),
    val simulatedGrades: Map<Long, List<SimulatedGrade>> = emptyMap(),
    val filterFromLastLogin: Boolean = false,
    val newSinceLastLoginCount: Int = 0,
)

@Immutable
data class GradeSubjectUi(
    val subjectId: Long,
    val subjectName: String,
    val average: Float?,
    val proposedGrade: String?,
    val finalGrade: String?,
    val colorIndex: Int = 0,
    val grades: PersistentList<GradeItemUi> = persistentListOf(),
)

@Immutable
data class GradeItemUi(
    val id: Long,
    val name: String,
    val value: Float,
    val weight: Float,
    val category: String,
    val comment: String?,
    val teacher: String?,
    val dateString: String,
    val color: Int?,
    val isCounted: Boolean = true,
    val isPinned: Boolean = false,
    val description: String? = null,
    val classAverage: Float? = null,
    val semester: Int = 1,
    val isFromLastLogin: Boolean = false,
)

@Immutable
data class SimulatedGrade(
    val value: Float,
    val weight: Float,
)
