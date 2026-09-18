package pl.szczodrzynski.edziennik.data.api.edziennik.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.Calendar

enum class DemoDayOption(
    val title: String,
    val calendarDay: Int?,
    val emoji: String,
) {
    REAL("Rzeczywisty dzień", null, "⚡"),
    MONDAY("Poniedziałek", Calendar.MONDAY, "📅"),
    TUESDAY("Wtorek", Calendar.TUESDAY, "📅"),
    WEDNESDAY("Środa", Calendar.WEDNESDAY, "📅"),
    THURSDAY("Czwartek", Calendar.THURSDAY, "📅"),
    FRIDAY("Piątek", Calendar.FRIDAY, "📅"),
    SATURDAY("Sobota (weekend)", Calendar.SATURDAY, "🏖️"),
    SUNDAY("Niedziela (weekend)", Calendar.SUNDAY, "🏖️");

    companion object {
        fun fromCalendarDay(day: Int?): DemoDayOption {
            return entries.firstOrNull { it.calendarDay == day } ?: REAL
        }
    }
}

object DemoDayManager {
    private val _currentOption = MutableStateFlow(DemoDayOption.REAL)
    val currentOption = _currentOption.asStateFlow()

    private fun dayToNorm(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    fun setOption(option: DemoDayOption) {
        _currentOption.value = option
        if (option.calendarDay == null) {
            Date.mockToday = null
        } else {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            val currentNorm = dayToNorm(cal.get(Calendar.DAY_OF_WEEK))
            val targetNorm = dayToNorm(option.calendarDay)
            val diff = targetNorm - currentNorm

            cal.add(Calendar.DAY_OF_MONTH, diff)
            Date.mockToday = Date(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    fun getSimulatedNowMillis(): Long {
        return Date.getNowInMillis()
    }
}
