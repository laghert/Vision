package pl.szczodrzynski.edziennik.ui.grades.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.utils.models.Date

class GradesViewModel(
    private val app: App,
    private val profileId: Int,
) : ViewModel() {

    private val selectedSemester = MutableStateFlow(GradesSemesterTab.SEMESTER_1)
    private val simulatedGrades = MutableStateFlow<Map<Long, List<SimulatedGrade>>>(emptyMap())
    private val pinTrigger = MutableStateFlow(0)
    private val filterFromLastLogin = MutableStateFlow(false)

    val uiState = combine(
        app.db.gradeDao().getAll(profileId).asFlow(),
        selectedSemester,
        simulatedGrades,
        pinTrigger,
        filterFromLastLogin,
    ) { allGrades, semesterTab, simulated, _, fromLastLogin ->
        val pinned = app.config[profileId].ui.pinnedGradeIds
        val previousLoginTime = app.config[profileId].ui.previousLoginTime

        val gradesForSemester = allGrades.filter { grade ->
            when (semesterTab) {
                GradesSemesterTab.SEMESTER_1 -> grade.semester == 1
                GradesSemesterTab.SEMESTER_2 -> grade.semester == 2
                GradesSemesterTab.YEAR -> true
            }
        }

        val newSinceLastLoginCount = gradesForSemester.count {
            !it.seen || (previousLoginTime > 0L && it.addedDate >= previousLoginTime)
        }

        // Group by subject
        val subjectsGrouped = allGrades.groupBy { it.subjectId }
        val subjectsList = mutableListOf<GradeSubjectUi>()
        var colorIdx = 0

        subjectsGrouped.forEach { (subjectId, subjectAllGrades) ->
            val firstGrade = subjectAllGrades.firstOrNull() ?: return@forEach
            val subjectName = firstGrade.subjectLongName ?: firstGrade.subjectShortName ?: "Inny przedmiot"

            val relevantGrades = when (semesterTab) {
                GradesSemesterTab.SEMESTER_1 -> subjectAllGrades.filter { it.semester == 1 }
                GradesSemesterTab.SEMESTER_2 -> subjectAllGrades.filter { it.semester == 2 }
                GradesSemesterTab.YEAR -> subjectAllGrades
            }

            // Normal grades for calculation
            val normalGrades = relevantGrades.filter { it.type == Grade.TYPE_NORMAL && it.value > 0f }
            val extraSimulated = simulated[subjectId] ?: emptyList()

            var totalWeightedSum = 0f
            var totalWeight = 0f

            normalGrades.forEach { g ->
                val weight = if (g.weight > 0f) g.weight else 1f
                totalWeightedSum += g.value * weight
                totalWeight += weight
            }

            extraSimulated.forEach { sim ->
                totalWeightedSum += sim.value * sim.weight
                totalWeight += sim.weight
            }

            val subjectAverage = if (totalWeight > 0f) totalWeightedSum / totalWeight else null

            // Proposed & Final grades
            val proposed = when (semesterTab) {
                GradesSemesterTab.SEMESTER_1 -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_SEMESTER1_PROPOSED }?.name
                GradesSemesterTab.SEMESTER_2 -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_SEMESTER2_PROPOSED }?.name
                GradesSemesterTab.YEAR -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_YEAR_PROPOSED }?.name
            }

            val final = when (semesterTab) {
                GradesSemesterTab.SEMESTER_1 -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_SEMESTER1_FINAL }?.name
                GradesSemesterTab.SEMESTER_2 -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_SEMESTER2_FINAL }?.name
                GradesSemesterTab.YEAR -> subjectAllGrades.firstOrNull { it.type == Grade.TYPE_YEAR_FINAL }?.name
            }

            val itemsUi = relevantGrades
                .sortedWith(compareByDescending<GradeFull> { it.id in pinned }.thenByDescending { it.addedDate })
                .map { g ->
                    val dateStr = if (g.addedDate > 0) Date.fromMillis(g.addedDate).formattedString else ""
                    val isFromLastLogin = !g.seen || (previousLoginTime > 0L && g.addedDate >= previousLoginTime)
                    GradeItemUi(
                        id = g.id,
                        name = g.name.ifBlank { "•" },
                        value = g.value,
                        weight = g.weight,
                        category = g.category ?: g.comment ?: "Ocena",
                        comment = g.comment,
                        teacher = g.teacherName,
                        dateString = dateStr,
                        color = g.color,
                        isCounted = g.type == Grade.TYPE_NORMAL && g.value > 0f,
                        isPinned = g.id in pinned,
                        description = g.description,
                        classAverage = if (g.classAverage != null && g.classAverage != -1f) g.classAverage else null,
                        semester = g.semester,
                        isFromLastLogin = isFromLastLogin,
                    )
                }
                .toPersistentList()

            subjectsList.add(
                GradeSubjectUi(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    average = subjectAverage,
                    proposedGrade = proposed,
                    finalGrade = final,
                    colorIndex = colorIdx++,
                    grades = itemsUi,
                ),
            )
        }

        // Sort subjects by name
        val sortedSubjects = subjectsList.sortedBy { it.subjectName }

        // Filter by last login if selected
        val displaySubjects = if (fromLastLogin) {
            sortedSubjects
                .filter { it.grades.any { g -> g.isFromLastLogin } }
                .map { sub -> sub.copy(grades = sub.grades.filter { it.isFromLastLogin }.toPersistentList()) }
                .toPersistentList()
        } else {
            sortedSubjects.toPersistentList()
        }

        // Overall GPA
        val subjectAverages = sortedSubjects.mapNotNull { it.average }
        val overallAvg = if (subjectAverages.isNotEmpty()) subjectAverages.average().toFloat() else null

        // Red stripe (pasek 4.75)
        val redStripeProgress = if (overallAvg != null) (overallAvg / 4.75f).coerceIn(0f, 1f) else 0f
        val redStripeDiff = if (overallAvg != null) overallAvg - 4.75f else null

        // Grade count distribution (6, 5, 4, 3, 2, 1)
        val normalGradesAll = gradesForSemester.filter { it.type == Grade.TYPE_NORMAL && it.value > 0f }
        val distribution = (1..6).associateWith { gradeNum ->
            normalGradesAll.count { it.value.toInt() == gradeNum }
        }

        GradesUiState(
            isLoading = false,
            selectedSemester = semesterTab,
            overallAverage = overallAvg,
            redStripeProgress = redStripeProgress,
            redStripeDiff = redStripeDiff,
            gradeDistribution = distribution,
            subjects = displaySubjects,
            simulatedGrades = simulated,
            filterFromLastLogin = fromLastLogin,
            newSinceLastLoginCount = newSinceLastLoginCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GradesUiState(),
    )

    fun selectSemester(tab: GradesSemesterTab) {
        selectedSemester.value = tab
    }

    fun toggleFilterFromLastLogin() {
        filterFromLastLogin.value = !filterFromLastLogin.value
    }

    fun addSimulatedGrade(subjectId: Long, value: Float, weight: Float) {
        val current = simulatedGrades.value.toMutableMap()
        val list = current[subjectId]?.toMutableList() ?: mutableListOf()
        list.add(SimulatedGrade(value, weight))
        current[subjectId] = list
        simulatedGrades.value = current
    }

    fun clearSimulatedGrades(subjectId: Long) {
        val current = simulatedGrades.value.toMutableMap()
        current.remove(subjectId)
        simulatedGrades.value = current
    }

    fun togglePinnedGrade(gradeId: Long) {
        val ui = app.config[profileId].ui
        ui.pinnedGradeIds = if (gradeId in ui.pinnedGradeIds) ui.pinnedGradeIds - gradeId else ui.pinnedGradeIds + gradeId
        pinTrigger.value++
    }

    fun markGradeSeen(gradeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val grade = app.db.gradeDao().getByIdNow(profileId, gradeId) ?: return@launch
            app.gradesManager.markAsSeen(grade)
        }
    }

    companion object {
        fun factory(app: App, profileId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GradesViewModel(app, profileId) as T
            }
        }
    }
}
