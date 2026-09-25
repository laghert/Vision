package pl.szczodrzynski.edziennik.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.ui.today.TodayLessonStatusKind
import pl.szczodrzynski.edziennik.ui.today.TodayUpcomingKind
import pl.szczodrzynski.edziennik.ui.navigation.NowLessonStore
import pl.szczodrzynski.edziennik.ui.navigation.NowLessonUi
import pl.szczodrzynski.edziennik.ui.notes.NoteListDialog
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskFinishedEvent
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.ext.JsonObject
import java.util.Collections

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TimetableViewModel private constructor(
    private val app: App,
    private val profileId: Int,
    private val bellCorrectionMillis: Long,
    private val locale: Locale,
) : ViewModel() {

    private val db: AppDb = app.db

    val selectedDate = MutableStateFlow(Date.getToday())
    private val syncingWeeks = MutableStateFlow<Set<String>>(emptySet())
    private val syncedWeeks = Collections.synchronizedSet(mutableSetOf<String>())

    private val clock = flow {
        while (currentCoroutineContext().isActive) {
            emit(pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDayManager.getSimulatedNowMillis())
            delay(CLOCK_INTERVAL_MILLIS)
        }
    }

    private fun <T> Flow<T>.withFallback(
        source: String,
        fallback: T,
    ): Flow<T> = retryWhen { throwable, attempt ->
        Timber.e(throwable, "Unable to read %s for profile %d (attempt %d)", source, profileId, attempt + 1)
        emit(fallback)
        delay(RETRY_INTERVAL_MILLIS)
        true
    }

    private val weekRange = selectedDate
        .map { date ->
            val monday = getMondayOfWeek(date)
            val sunday = monday.clone().stepForward(0, 0, 6)
            monday to sunday
        }
        .distinctUntilChanged { prev, curr ->
            prev.first.value == curr.first.value && prev.second.value == curr.second.value
        }

    private val weekLessons = weekRange.flatMapLatest { (monday, sunday) ->
        db.timetableDao().getBetweenDates(profileId, monday, sunday).asFlow()
    }.withFallback("timetable lessons", emptyList())

    private val weekEvents = weekRange.flatMapLatest { (monday, sunday) ->
        db.eventDao().getAllByDateRange(profileId, monday, sunday).asFlow()
    }.withFallback("events", emptyList())

    val uiState = combine(selectedDate, weekLessons, weekEvents, clock, syncingWeeks) { currentDate, lessons, events, now, syncing ->
        buildTimetableState(currentDate, lessons, events, now, syncing)
    }.withFallback("Timetable state", TimetableUiState(isLoading = false))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TimetableUiState(),
        )

    init {
        TimetableFragment.pageSelection = selectedDate.value
        EventBus.getDefault().register(this)

        viewModelScope.launch {
            selectedDate.collect {
                TimetableFragment.pageSelection = it
            }
        }

        viewModelScope.launch {
            combine(weekRange, weekLessons) { (monday, _), lessons ->
                monday to lessons
            }.collect { (monday, lessons) ->
                val weekKey = monday.stringY_m_d
                if (lessons.isEmpty() && !syncedWeeks.contains(weekKey) && !syncingWeeks.value.contains(weekKey)) {
                    Timber.d("Timetable week %s has no lessons, auto-syncing...", weekKey)
                    syncWeek(monday, force = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskFinishedEvent(event: ApiTaskFinishedEvent) {
        if (event.profileId == profileId) {
            syncingWeeks.value = emptySet()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskAllFinishedEvent(event: ApiTaskAllFinishedEvent) {
        syncingWeeks.value = emptySet()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskErrorEvent(event: ApiTaskErrorEvent) {
        if (event.error.profileId == profileId) {
            syncingWeeks.value = emptySet()
        }
    }

    fun syncCurrentWeek(force: Boolean = true) {
        val monday = getMondayOfWeek(selectedDate.value)
        syncWeek(monday, force = force)
    }

    fun syncWeek(monday: Date, force: Boolean = true) {
        val weekKey = monday.stringY_m_d
        if (!force && syncedWeeks.contains(weekKey)) {
            return
        }
        if (syncingWeeks.value.contains(weekKey)) {
            return
        }
        syncedWeeks.add(weekKey)
        syncingWeeks.value = syncingWeeks.value + weekKey
        TimetableFragment.pageSelection = selectedDate.value

        viewModelScope.launch(Dispatchers.IO) {
            if (app.profile.loginStoreType == LoginType.DEMO) {
                DemoDataSeeder.seedWeek(app, profileId, monday)
                withContext(Dispatchers.Main) {
                    syncingWeeks.value = syncingWeeks.value - weekKey
                }
            } else {
                EdziennikTask.syncProfile(
                    profileId = profileId,
                    featureTypes = setOf(FeatureType.TIMETABLE),
                    arguments = JsonObject("weekStart" to weekKey),
                ).enqueue(app)
            }
        }

        viewModelScope.launch {
            delay(15_000L)
            if (syncingWeeks.value.contains(weekKey)) {
                syncingWeeks.value = syncingWeeks.value - weekKey
            }
        }
    }

    fun selectDate(date: Date) {
        selectedDate.value = date
    }

    fun stepWeek(weekDelta: Int) {
        val target = selectedDate.value.clone().stepForward(0, 0, weekDelta * 7)
        selectedDate.value = target
    }

    fun stepDay(dayDelta: Int) {
        val target = selectedDate.value.clone().stepForward(0, 0, dayDelta)
        selectedDate.value = skipWeekendIfNeeded(target, dayDelta >= 0)
    }

    fun stepToAdjacentWeek(forward: Boolean) {
        val monday = getMondayOfWeek(selectedDate.value)
        selectedDate.value = if (forward) {
            monday.clone().stepForward(0, 0, 7)
        } else {
            val lastOffset = (uiState.value.days.size - 1).coerceAtLeast(4)
            monday.clone().stepForward(0, 0, -7 + lastOffset)
        }
    }

    fun selectToday() {
        selectedDate.value = Date.getToday()
    }

    fun openLessonNotes(activity: AppCompatActivity, lessonId: Long) {
        viewModelScope.launch {
            val lesson = withContext(Dispatchers.IO) {
                db.timetableDao().getByIdNow(profileId, lessonId)
            } ?: return@launch
            NoteListDialog(activity = activity, owner = lesson).show()
        }
    }

    private fun skipWeekendIfNeeded(date: Date, forward: Boolean): Date {
        if (uiState.value.days.size > 5) return date
        return when (date.weekDay) {
            Week.SATURDAY -> date.clone().stepForward(0, 0, if (forward) 2 else -1)
            Week.SUNDAY -> date.clone().stepForward(0, 0, if (forward) 1 else -2)
            else -> date
        }
    }

    private fun buildTimetableState(
        currentDate: Date,
        allLessons: List<LessonFull>,
        allEvents: List<EventFull>,
        now: Long,
        syncing: Set<String>,
    ): TimetableUiState {
        val correctedNow = now - bellCorrectionMillis
        val today = Date.fromMillis(now)
        val monday = getMondayOfWeek(currentDate)
        val isWeekSyncing = syncing.contains(monday.stringY_m_d)
        val weekendDays = setOf(Week.SATURDAY, Week.SUNDAY)
        val showWeekend = currentDate.weekDay in weekendDays ||
            allLessons.any { it.displayDate?.weekDay in weekendDays }

        val dayCount = if (showWeekend) 7 else 5
        val nowSeconds = Time.fromMillis(correctedNow).inSeconds
        val days = (0 until dayCount).map { offset ->
            val dayDate = monday.clone().stepForward(0, 0, offset)
            buildDay(
                dayDate = dayDate,
                today = today,
                currentDate = currentDate,
                allLessons = allLessons,
                allEvents = allEvents,
                nowSeconds = nowSeconds,
                isWeekSyncing = isWeekSyncing,
            )
        }.toPersistentList()

        days.asSequence()
            .filter { it.isToday }
            .flatMap { it.items.asSequence() }
            .filterIsInstance<TimetableItemUi.Lesson>()
            .firstOrNull { it.isCurrent }
            ?.let { lesson ->
                NowLessonStore.publish(
                    NowLessonUi(
                        subject = lesson.subject,
                        room = lesson.classroom,
                        progress = lesson.progress,
                        minutesRemaining = lesson.minutesRemaining,
                        endsAt = lesson.endTime,
                    ),
                )
            }

        return TimetableUiState(
            isLoading = isWeekSyncing,
            selectedDate = currentDate,
            weekTitle = formatWeekTitle(monday, days.last().date),
            isTodaySelected = currentDate.value == today.value,
            days = days,
        )
    }

    private fun buildDay(
        dayDate: Date,
        today: Date,
        currentDate: Date,
        allLessons: List<LessonFull>,
        allEvents: List<EventFull>,
        nowSeconds: Long,
        isWeekSyncing: Boolean,
    ): TimetableDayUi {
        val isWeekend = dayDate.weekDay in Week.SATURDAY..Week.SUNDAY
        val dayLessons = allLessons.filter { it.displayDate?.value == dayDate.value }
        val dayEvents = allEvents.filter { it.date.value == dayDate.value }
        val hasChanges = dayLessons.any { it.isCancelled || it.isChanged }
        val hasExams = dayEvents.any { it.type == Event.TYPE_EXAM || it.type == Event.TYPE_SHORT_QUIZ }

        val isWeekEmpty = allLessons.isEmpty()
        val isNotDownloaded = !isWeekend && isWeekEmpty
        val isLoading = isWeekSyncing

        val validLessons = dayLessons
            .asSequence()
            .filter { it.type != Lesson.TYPE_NO_LESSONS }
            .filter { it.displayStartTime != null && it.displayEndTime != null }
            .sortedWith(
                compareBy<LessonFull> { it.displayStartTime?.value }
                    .thenByDescending { it.visualPriority() },
            )
            .toList()

        val activeLessons = validLessons.filterNot(LessonFull::isCancelled)
        val openEvents = dayEvents.filter { !it.isDone }
        val isSelectedDayToday = dayDate.value == today.value
        val title = dayDate.fullDayTitle()
        val summary = if (activeLessons.isNotEmpty()) {
            val count = activeLessons.size
            val start = activeLessons.first().displayStartTime?.stringHM
            val end = activeLessons.last().displayEndTime?.stringHM
            "$count ${lessonWord(count)} • $start – $end"
        } else {
            ""
        }

        if (validLessons.isEmpty()) {
            val hasExplicitNoLessons = dayLessons.any { it.type == Lesson.TYPE_NO_LESSONS }
            val isFreeDay = isWeekend || (!isWeekEmpty && (hasExplicitNoLessons || dayLessons.isEmpty()))
            return TimetableDayUi(
                date = dayDate,
                dayOfWeekShort = dayDate.dayOfWeekShort(),
                dayOfMonth = dayDate.day,
                isToday = dayDate.value == today.value,
                isSelected = dayDate.value == currentDate.value,
                hasChanges = hasChanges,
                hasExams = hasExams,
                title = title,
                summary = summary,
                isFreeDay = isFreeDay,
                isNotDownloaded = isNotDownloaded,
                isLoading = isLoading,
            )
        }

        val items = mutableListOf<TimetableItemUi>()
        var currentLessonIndex = -1

        for (i in validLessons.indices) {
            val lesson = validLessons[i]
            val startSec = lesson.displayStartTime!!.inSeconds
            val endSec = lesson.displayEndTime!!.inSeconds

            if (i > 0) {
                val prevLesson = validLessons[i - 1]
                val prevEndSec = prevLesson.displayEndTime!!.inSeconds
                val breakMinutes = ((startSec - prevEndSec) / 60).toInt()
                if (breakMinutes in 5..120) {
                    val isBreakNow = isSelectedDayToday && nowSeconds in prevEndSec until startSec
                    items.add(
                        TimetableItemUi.Break(
                            durationMinutes = breakMinutes,
                            startTime = prevLesson.displayEndTime?.stringHM.orEmpty(),
                            endTime = lesson.displayStartTime?.stringHM.orEmpty(),
                            isCurrent = isBreakNow,
                        ),
                    )
                }
            }

            val isCurrent = isSelectedDayToday && nowSeconds in startSec until endSec
            val progress = if (isCurrent && endSec > startSec) {
                ((nowSeconds - startSec).toFloat() / (endSec - startSec)).coerceIn(0f, 1f)
            } else 0f
            val minutesRemaining = if (isCurrent) {
                ceil((endSec - nowSeconds).coerceAtLeast(0L) / 60.0).toInt()
            } else 0

            val linkedEvents = openEvents
                .filter { it.subjectId == lesson.subjectId || it.subjectLongName == lesson.subjectName }
                .map { event ->
                    TimetableLinkedEventUi(
                        id = event.id,
                        title = event.topicHtml.toString().ifBlank { event.typeName.orEmpty().ifBlank { "Wydarzenie" } },
                        kind = when (event.type) {
                            Event.TYPE_EXAM -> TodayUpcomingKind.EXAM
                            Event.TYPE_SHORT_QUIZ -> TodayUpcomingKind.QUIZ
                            Event.TYPE_PROJECT -> TodayUpcomingKind.PROJECT
                            else -> TodayUpcomingKind.DEADLINE
                        },
                    )
                }

            val statusKind = when (lesson.type) {
                Lesson.TYPE_CANCELLED -> TodayLessonStatusKind.CANCELLED
                Lesson.TYPE_CHANGE -> TodayLessonStatusKind.CHANGED
                Lesson.TYPE_SHIFTED_SOURCE, Lesson.TYPE_SHIFTED_TARGET -> TodayLessonStatusKind.SHIFTED
                else -> null
            }
            val statusDetails = when (statusKind) {
                TodayLessonStatusKind.CHANGED -> listOf(lesson.changeSubjectName, lesson.changeClassroom)
                    .filter(String::isNotBlank)
                    .joinToString(" • ")
                    .takeIf(String::isNotBlank)
                else -> null
            }
            val changeType = if (lesson.isChanged || lesson.isCancelled) {
                lesson.getDisplayChangeType(app)
            } else null

            if (isCurrent) currentLessonIndex = items.size
            items.add(
                TimetableItemUi.Lesson(
                    id = lesson.id,
                    number = lesson.displayLessonNumber,
                    subject = lesson.displaySubjectName.orEmpty().ifBlank { "—" },
                    originalSubject = if (lesson.isChanged && !lesson.oldSubjectName.isNullOrBlank()) lesson.oldSubjectName else null,
                    classroom = lesson.displayClassroom?.takeIf(String::isNotBlank),
                    teacher = lesson.displayTeacherName?.takeIf(String::isNotBlank),
                    originalTeacher = if (lesson.isChanged && !lesson.oldTeacherName.isNullOrBlank()) lesson.oldTeacherName else null,
                    topic = null,
                    changeType = changeType,
                    changeReason = null,
                    groupName = lesson.displayTeamName?.takeIf(String::isNotBlank),
                    startTime = lesson.displayStartTime?.stringHM.orEmpty(),
                    endTime = lesson.displayEndTime?.stringHM.orEmpty(),
                    startSeconds = startSec,
                    endSeconds = endSec,
                    statusKind = statusKind,
                    statusDetails = statusDetails,
                    isCurrent = isCurrent,
                    progress = progress,
                    minutesRemaining = minutesRemaining,
                    colorArgb = lesson.color?.takeIf { it != 0 }
                        ?: Colors.stringToMaterialColorCRC(lesson.displaySubjectName.orEmpty()),
                    notePreview = runCatching {
                        lesson.notes.firstOrNull()?.let { note ->
                            note.topic?.takeIf { it.isNotBlank() } ?: note.body.take(120)
                        }
                    }.getOrNull(),
                    events = linkedEvents,
                ),
            )
        }

        val preview = items.filterIsInstance<TimetableItemUi.Lesson>().map { lesson ->
            TimetableLessonPreviewUi(
                startTime = lesson.startTime,
                subject = lesson.subject,
                isCancelled = lesson.statusKind == TodayLessonStatusKind.CANCELLED,
                isChanged = lesson.statusKind == TodayLessonStatusKind.CHANGED ||
                    lesson.statusKind == TodayLessonStatusKind.SHIFTED,
                isCurrent = lesson.isCurrent,
            )
        }.toPersistentList()

        return TimetableDayUi(
            date = dayDate,
            dayOfWeekShort = dayDate.dayOfWeekShort(),
            dayOfMonth = dayDate.day,
            isToday = dayDate.value == today.value,
            isSelected = dayDate.value == currentDate.value,
            hasChanges = hasChanges,
            hasExams = hasExams,
            title = title,
            summary = summary,
            isFreeDay = false,
            isNotDownloaded = false,
            isLoading = isLoading,
            currentLessonIndex = currentLessonIndex,
            items = items.toPersistentList(),
            preview = preview,
        )
    }

    private fun lessonWord(count: Int) = when (count) {
        1 -> "lekcja"
        in 2..4 -> "lekcje"
        else -> "lekcji"
    }

    private fun LessonFull.visualPriority(): Int = when (type) {
        Lesson.TYPE_CANCELLED -> 3
        Lesson.TYPE_CHANGE -> 2
        Lesson.TYPE_SHIFTED_SOURCE, Lesson.TYPE_SHIFTED_TARGET -> 2
        else -> 0
    }

    private fun getMondayOfWeek(date: Date): Date {
        val cal = Calendar.getInstance(locale).apply { timeInMillis = date.inMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return Date.fromCalendar(cal)
    }

    private fun Date.dayOfWeekShort(): String {
        val cal = Calendar.getInstance(locale).apply { timeInMillis = inMillis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Pn"
            Calendar.TUESDAY -> "Wt"
            Calendar.WEDNESDAY -> "Śr"
            Calendar.THURSDAY -> "Czw"
            Calendar.FRIDAY -> "Pt"
            Calendar.SATURDAY -> "Sob"
            else -> "Ndz"
        }
    }

    private fun Date.fullDayTitle(): String {
        val cal = Calendar.getInstance(locale).apply { timeInMillis = inMillis }
        val dayName = SimpleDateFormat("EEEE", locale).format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
        val monthDay = SimpleDateFormat("d MMMM", locale).format(cal.time)
        return "$dayName, $monthDay"
    }

    private fun formatWeekTitle(monday: Date, last: Date): String {
        val start = Calendar.getInstance(locale).apply { timeInMillis = monday.inMillis }
        val end = Calendar.getInstance(locale).apply { timeInMillis = last.inMillis }
        val genitiveMonths = app.resources.getStringArray(R.array.months_day_of_array)
        val startMonthGen = genitiveMonths.getOrNull(start.get(Calendar.MONTH))
            ?: SimpleDateFormat("MMMM", locale).format(start.time)
        val endMonthGen = genitiveMonths.getOrNull(end.get(Calendar.MONTH))
            ?: SimpleDateFormat("MMMM", locale).format(end.time)
        val startDay = start.get(Calendar.DAY_OF_MONTH)
        val endDay = end.get(Calendar.DAY_OF_MONTH)
        val startYear = start.get(Calendar.YEAR)
        val endYear = end.get(Calendar.YEAR)
        return when {
            startYear != endYear -> "$startDay $startMonthGen $startYear – $endDay $endMonthGen $endYear"
            start.get(Calendar.MONTH) != end.get(Calendar.MONTH) -> "$startDay $startMonthGen – $endDay $endMonthGen"
            else -> "$startDay–$endDay $startMonthGen"
        }
    }

    companion object {
        private const val CLOCK_INTERVAL_MILLIS = 15_000L
        private const val RETRY_INTERVAL_MILLIS = 5_000L

        fun factory(app: App, profileId: Int): ViewModelProvider.Factory {
            val bellCorrectionMillis = app.config.timetable.bellSyncDiff?.let { difference ->
                difference.inSeconds * 1_000L * app.config.timetable.bellSyncMultiplier
            } ?: 0L
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TimetableViewModel::class.java))
                    return TimetableViewModel(
                        app = app,
                        profileId = profileId,
                        bellCorrectionMillis = bellCorrectionMillis,
                        locale = Locale.getDefault(),
                    ) as T
                }
            }
        }
    }
}

private val LessonFull.isChanged: Boolean
    get() = type in listOf(
        Lesson.TYPE_CHANGE,
        Lesson.TYPE_SHIFTED_SOURCE,
        Lesson.TYPE_SHIFTED_TARGET,
    )
