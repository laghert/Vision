package pl.szczodrzynski.edziennik.ui.agenda.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendaViewModel(
    private val app: App,
    private val profileId: Int,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(AgendaFilter.ALL)
    private val selectedDate = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AgendaUiState> = combine(
        app.db.eventDao().getAll(profileId).asFlow(),
        selectedFilter,
        selectedDate,
    ) { rawEvents, filter, chosenDate ->
        buildState(rawEvents ?: emptyList(), filter, chosenDate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgendaUiState(),
    )

    fun setFilter(filter: AgendaFilter) {
        selectedFilter.value = filter
        // If a specific filter is chosen, clear selected day so the user sees all matching events
        if (filter != AgendaFilter.ALL) {
            selectedDate.value = null
        }
    }

    fun selectDate(dateString: String?) {
        selectedDate.value = if (selectedDate.value == dateString) null else dateString
    }

    fun toggleEventDone(eventId: Long, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = app.db.eventDao().getByIdNow(profileId, eventId) ?: return@launch
            event.isDone = isDone
            app.db.eventDao().update(event)
        }
    }

    private fun EventFull.isExam(): Boolean =
        type == Event.TYPE_EXAM ||
        typeName?.contains("sprawdz", ignoreCase = true) == true ||
        typeName?.contains("praca klas", ignoreCase = true) == true ||
        topic.contains("sprawdzian", ignoreCase = true) ||
        topic.contains("praca klasowa", ignoreCase = true)

    private fun EventFull.isQuiz(): Boolean =
        type == Event.TYPE_SHORT_QUIZ ||
        typeName?.contains("kartkówk", ignoreCase = true) == true ||
        topic.contains("kartkówka", ignoreCase = true) ||
        topic.contains("kartkówk", ignoreCase = true)

    private fun EventFull.isHomeworkEvent(): Boolean =
        isHomework ||
        type == Event.TYPE_HOMEWORK ||
        typeName?.contains("zadani", ignoreCase = true) == true ||
        typeName?.contains("domow", ignoreCase = true) == true ||
        topic.contains("zadanie", ignoreCase = true)

    private fun buildState(
        rawList: List<EventFull>,
        filter: AgendaFilter,
        chosenDate: String?,
    ): AgendaUiState {
        val today = Date.getToday()
        val dayOfWeekFormat = SimpleDateFormat("EE", Locale("pl"))
        val dayNumberFormat = SimpleDateFormat("d", Locale("pl"))
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        // Generate 14-day strip anchored around today (supports simulated dates)
        val cal = Calendar.getInstance().apply {
            timeInMillis = Date.getNowInMillis()
            set(today.year, today.month - 1, today.day, 0, 0, 0)
        }
        val daysList = (0..13).map { offset ->
            val curCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dateKey = fullDateFormat.format(curCal.time)
            val count = rawList.count { it.date.stringY_m_d == dateKey }
            val dayName = dayOfWeekFormat.format(curCal.time).replace(".", "").replaceFirstChar { it.uppercase() }
            val dayNum = dayNumberFormat.format(curCal.time)

            AgendaDayChipUi(
                dayOfWeek = dayName,
                dayNumber = dayNum,
                dateString = dateKey,
                isToday = offset == 0,
                hasEvents = count > 0,
                eventCount = count,
            )
        }.toPersistentList()

        val examsCount = rawList.count { it.isExam() }
        val quizzesCount = rawList.count { it.isQuiz() }
        val homeworkCount = rawList.count { it.isHomeworkEvent() }

        val filteredList = rawList.filter { event ->
            val matchesType = when (filter) {
                AgendaFilter.ALL -> true
                AgendaFilter.EXAMS -> event.isExam()
                AgendaFilter.QUIZZES -> event.isQuiz()
                AgendaFilter.HOMEWORK -> event.isHomeworkEvent()
            }
            val matchesDate = if (chosenDate != null) {
                event.date.stringY_m_d == chosenDate
            } else {
                true
            }
            matchesType && matchesDate
        }

        val mappedEvents = filteredList.map { event ->
            val eventDate = event.date
            val daysDiff = Date.diffDays(eventDate, today)
            val countdown = when {
                daysDiff < 0 -> "Przeszłe"
                daysDiff == 0 -> "Dzisiaj"
                daysDiff == 1 -> "Jutro"
                daysDiff == 2 -> "Pojutrze"
                daysDiff in 3..7 -> "Za $daysDiff dni"
                else -> eventDate.formattedString
            }

            val typeTitle = when {
                event.isQuiz() -> "Kartkówka"
                event.isExam() -> "Sprawdzian"
                event.isHomeworkEvent() -> "Zadanie domowe"
                event.type == Event.TYPE_ESSAY -> "Wypracowanie"
                event.type == Event.TYPE_PROJECT -> "Projekt"
                else -> event.typeName ?: "Wydarzenie"
            }

            AgendaEventItemUi(
                id = event.id,
                subjectName = event.subjectLongName ?: event.subjectShortName ?: "Inne",
                topic = event.topic.ifBlank { typeTitle },
                dateLabel = eventDate.formattedString,
                timeLabel = event.time?.stringValue ?: "",
                countdownLabel = countdown,
                typeName = typeTitle,
                typeColor = event.typeColor ?: event.color,
                isHomework = event.isHomeworkEvent(),
                isDone = event.isDone,
                teacherName = event.teacherName,
            )
        }.toPersistentList()

        return AgendaUiState(
            isLoading = false,
            selectedFilter = filter,
            selectedDateString = chosenDate,
            days = daysList,
            events = mappedEvents,
            totalCount = rawList.size,
            examsCount = examsCount,
            quizzesCount = quizzesCount,
            homeworkCount = homeworkCount,
        )
    }

    companion object {
        fun factory(app: App, profileId: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AgendaViewModel(app, profileId) as T
            }
        }
    }
}
