package pl.szczodrzynski.edziennik.ui.attendance.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceViewModel(
    private val app: App,
    private val profileId: Int,
) : ViewModel() {

    val uiState: StateFlow<AttendanceUiState> = app.db.attendanceDao()
        .getAll(profileId)
        .asFlow()
        .map { list -> buildState(list ?: emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AttendanceUiState(),
        )

    private fun buildState(rawList: List<AttendanceFull>): AttendanceUiState {
        var present = 0
        var absent = 0
        var excused = 0
        var late = 0
        var released = 0

        rawList.forEach { item ->
            when (item.baseType) {
                Attendance.TYPE_PRESENT, Attendance.TYPE_PRESENT_CUSTOM -> present++
                Attendance.TYPE_ABSENT -> absent++
                Attendance.TYPE_ABSENT_EXCUSED -> excused++
                Attendance.TYPE_BELATED, Attendance.TYPE_BELATED_EXCUSED -> late++
                Attendance.TYPE_RELEASED -> released++
            }
        }

        val totalCounted = present + absent + excused + late
        val presenceScore = present + late // presence counts present + late
        val percentage = if (totalCounted > 0) {
            (presenceScore.toFloat() / totalCounted.toFloat()) * 100f
        } else {
            100f
        }

        val statusMsg = when {
            percentage >= 95f -> "Wzorowa frekwencja! 🌟"
            percentage >= 85f -> "Dobra frekwencja 👍"
            percentage >= 75f -> "Przeciętna frekwencja"
            percentage >= 50f -> "⚠️ Ryzyko nieklasyfikowania"
            else -> "🚨 Krytycznie niska frekwencja (<50%)"
        }

        val dayFormat = SimpleDateFormat("EEEE", Locale("pl"))
        val history = rawList
            .groupBy { it.date.stringY_m_d }
            .map { (dateKey, items) ->
                val firstDate = items.first().date
                val dayOfWeek = dayFormat.format(firstDate.asCalendar.time).replaceFirstChar { it.uppercase() }

                AttendanceDayUi(
                    dateLabel = firstDate.formattedString,
                    dayOfWeek = dayOfWeek,
                    isToday = Date.isToday(firstDate),
                    lessons = items.map { item ->
                        val baseType = item.baseType
                        val subj = item.subjectLongName ?: item.subjectShortName ?: "Lekcja"
                        AttendanceLessonUi(
                            id = item.id,
                            subjectName = subj,
                            timeLabel = item.startTime?.stringValue ?: "",
                            typeName = item.typeName.ifBlank { "Obecność" },
                            typeColor = item.typeColor,
                            isPresent = baseType == Attendance.TYPE_PRESENT || baseType == Attendance.TYPE_PRESENT_CUSTOM,
                            isExcused = baseType == Attendance.TYPE_ABSENT_EXCUSED,
                            isLate = baseType == Attendance.TYPE_BELATED || baseType == Attendance.TYPE_BELATED_EXCUSED,
                            isAbsent = baseType == Attendance.TYPE_ABSENT,
                        )
                    }.toPersistentList(),
                )
            }
            .toPersistentList()

        return AttendanceUiState(
            isLoading = false,
            overallPercentage = percentage,
            totalLessons = totalCounted + released,
            presentCount = present,
            absentCount = absent,
            excusedCount = excused,
            lateCount = late,
            releasedCount = released,
            streakDays = 5,
            statusMessage = statusMsg,
            historyByDay = history,
        )
    }

    companion object {
        fun factory(app: App, profileId: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AttendanceViewModel(app, profileId) as T
            }
        }
    }
}
