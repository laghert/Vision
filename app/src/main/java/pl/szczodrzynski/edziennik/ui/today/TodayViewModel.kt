package pl.szczodrzynski.edziennik.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.EndpointTimer
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlinx.coroutines.flow.MutableStateFlow
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.full.LuckyNumberFull
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import pl.szczodrzynski.edziennik.core.work.LessonReminderWorker
import pl.szczodrzynski.edziennik.ui.navigation.NowLessonStore
import pl.szczodrzynski.edziennik.ui.navigation.NowLessonUi
import timber.log.Timber
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModel private constructor(
    private val app: App,
    private val profileId: Int,
    private val bellCorrectionMillis: Long,
    private val locale: Locale,
) : ViewModel() {

    private val db: AppDb = app.db
    private val settingsTrigger = MutableStateFlow(0)
    private var scheduledReminderLessonId: Long? = null

    private data class UnseenCounts(
        val grades: Int,
        val messages: Int,
        val announcements: Int,
        val notices: Int,
    )

    private data class PrimarySnapshot(
        val lessons: List<LessonFull>,
        val unseenGrades: Int,
        val unreadMessages: Int,
        val unseenAnnouncements: Int,
        val unseenNotices: Int,
        val profile: Profile?,
        val profileIncomplete: Boolean,
    )

    private data class SecondarySnapshot(
        val upcomingEvents: List<EventFull>,
        val homeworkTomorrow: List<EventFull>,
        val timers: List<EndpointTimer>,
        val luckyNumber: LuckyNumberFull?,
        val recentGrades: List<GradeFull> = emptyList(),
    )

    private val clock = flow {
        while (currentCoroutineContext().isActive) {
            emit(pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDayManager.getSimulatedNowMillis())
            delay(CLOCK_INTERVAL_MILLIS)
        }
    }

    private val date = clock
        .map(Date::fromMillis)
        .distinctUntilChanged { previous, current -> previous.value == current.value }

    private val unseenCounts = combine(
        db.metadataDao().countUnseenGrades(profileId).asFlow()
            .withFallback("unseen grades", 0),
        db.metadataDao().countUnseenReceivedMessages(profileId).asFlow()
            .withFallback("unread messages", 0),
        db.metadataDao().countUnseen(profileId, MetadataType.ANNOUNCEMENT).asFlow()
            .withFallback("unseen announcements", 0),
        db.metadataDao().countUnseen(profileId, MetadataType.NOTICE).asFlow()
            .withFallback("unseen notices", 0),
    ) { grades, messages, announcements, notices ->
        UnseenCounts(
            grades = grades,
            messages = messages,
            announcements = announcements,
            notices = notices,
        )
    }

    private val primary = combine(
        date.flatMapLatest { rangeStart ->
            val rangeEnd = rangeStart.clone().stepForward(0, 0, FUTURE_SEARCH_DAYS)
            db.timetableDao().getBetweenDates(profileId, rangeStart, rangeEnd).asFlow()
        }.withFallback("lessons", emptyList()),
        unseenCounts,
        db.profileDao().getById(profileId).asFlow()
            .withFallback("profile state", null),
    ) { lessons, unseen, profile ->
        PrimarySnapshot(
            lessons = lessons,
            unseenGrades = unseen.grades,
            unreadMessages = unseen.messages,
            unseenAnnouncements = unseen.announcements,
            unseenNotices = unseen.notices,
            profile = profile,
            profileIncomplete = profile?.empty ?: true,
        )
    }

    private val secondary = combine(
        date.flatMapLatest {
            db.eventDao().getUpcomingNotDone(profileId, it, UPCOMING_DISPLAY_LIMIT).asFlow()
        }.withFallback("upcoming events", emptyList()),
        date.flatMapLatest {
            val tomorrow = it.clone().stepForward(0, 0, 1)
            db.eventDao().getHomeworkNotDoneByDate(profileId, tomorrow).asFlow()
        }.withFallback("tomorrow homework", emptyList()),
        db.endpointTimerDao().getAll(profileId).asFlow()
            .withFallback("endpoint timers", emptyList()),
        date.flatMapLatest {
            db.luckyNumberDao().getNearestFuture(profileId, it).asFlow()
        }.withFallback("lucky number", null),
        db.gradeDao().getAll(profileId).asFlow()
            .withFallback("recent grades", emptyList()),
    ) { upcomingEvents, homeworkTomorrow, timers, luckyNumber, recentGrades ->
        SecondarySnapshot(upcomingEvents, homeworkTomorrow, timers, luckyNumber, recentGrades)
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.withFallback(
        source: String,
        fallback: T,
    ): kotlinx.coroutines.flow.Flow<T> = retryWhen { throwable, attempt ->
        Timber.e(
            throwable,
            "Unable to read Today %s for profile %d (attempt %d)",
            source,
            profileId,
            attempt + 1,
        )
        emit(fallback)
        delay(RETRY_INTERVAL_MILLIS)
        true
    }

    val uiState = combine(primary, secondary, clock, settingsTrigger) { primary, secondary, now, _ ->
        buildState(primary, secondary, now)
    }.retryWhen { throwable, attempt ->
        Timber.e(throwable, "Unable to build Today state for profile %d (attempt %d)", profileId, attempt + 1)
        val today = Date.getToday()
        emit(
            TodayUiState(
                isLoading = false,
                hasError = true,
                profileIncomplete = true,
                dayOfWeekLabel = today.nominativeWeekdayTitle(),
                dateLabel = "${today.nominativeWeekdayTitle()}, ${today.formattedString}",
            ),
        )
        delay(RETRY_INTERVAL_MILLIS)
        true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = TodayUiState(),
    )

    private fun buildState(
        primary: PrimarySnapshot,
        secondary: SecondarySnapshot,
        now: Long,
    ): TodayUiState {
        val today = Date.fromMillis(now)
        val correctedNow = now - bellCorrectionMillis
        val timeline = primary.lessons
            .asSequence()
            .filter { it.type != Lesson.TYPE_NO_LESSONS }
            .filter {
                it.displayDate != null &&
                    it.displayStartTime != null &&
                    it.displayEndTime != null
            }
            .sortedWith(
                compareBy<LessonFull> { it.displayDate?.value }
                    .thenBy { it.displayStartTime?.value }
                    .thenByDescending { it.visualPriority() },
            )
            .toList()
        val lessonsByDate = timeline.groupBy { it.displayDate!!.value }
        val todayLessons = lessonsByDate[today.value].orEmpty()
        val activeTodayLessons = todayLessons.filterNot(LessonFull::isCancelled)
        val schedule = when {
            activeTodayLessons.isEmpty() -> Schedule(
                hero = TodayHeroUi(phase = TodayPhase.FREE_DAY),
                nextLessons = emptyList(),
            )
            else -> buildSchedule(activeTodayLessons, correctedNow)
        }
        val lastSync = secondary.timers.mapNotNull(EndpointTimer::lastSync).maxOrNull()

        val profileConfig = app.config[profileId].ui
        val attention = TodayAttentionUi(
            newGrades = primary.unseenGrades,
            unreadMessages = primary.unreadMessages,
            newAnnouncements = primary.unseenAnnouncements,
            newNotices = primary.unseenNotices,
            homeworkTomorrow = secondary.homeworkTomorrow.size,
            enabledGrades = profileConfig.checkGrades,
            enabledMessages = profileConfig.checkMessages,
            enabledAnnouncements = profileConfig.checkAnnouncements,
            enabledNotices = profileConfig.checkNotices,
            enabledHomework = profileConfig.checkHomework,
        )

        val luckyNumberVal = secondary.luckyNumber?.takeIf {
            it.date.value == today.value && it.number > 0
        }?.number

        val studentNumber = primary.profile?.studentNumber
        val isUserLucky = luckyNumberVal != null && studentNumber != null && studentNumber > 0 && studentNumber == luckyNumberVal

        val studentName = primary.profile?.studentNameShort?.takeIf { it.isNotBlank() }
            ?: primary.profile?.name?.takeIf { it.isNotBlank() }
            ?: primary.profile?.studentNameLong

        val greetingTitle = TodayGreetings.getGreeting(studentName, now, locale)
        val heroDateLabel = TodayGreetings.formatHeroDate(now, locale)

        val upcoming = secondary.upcomingEvents
            .asSequence()
            .filter { !it.isHomework && !it.isDone && it.date.value >= today.value }
            .map { it.toUpcomingUi(today) }
            .toList()
            .toPersistentList()

        val recentGrades = secondary.recentGrades
            .take(5)
            .map { g ->
                val dateStr = if (g.addedDate > 0) {
                    val d = Date.fromMillis(g.addedDate)
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = g.addedDate }
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = cal.get(java.util.Calendar.MINUTE)
                    val timeStr = "%02d:%02d".format(hour, minute)
                    if (d.value == today.value) "Dziś, $timeStr"
                    else if (d.value == today.clone().stepForward(0, 0, -1).value) "Wczoraj, $timeStr"
                    else d.formattedString
                } else ""

                TodayRecentGradeUi(
                    id = g.id,
                    gradeValue = g.name.ifBlank { "•" },
                    subjectName = g.subjectShortName ?: g.subjectLongName ?: "Inne",
                    categoryName = g.category ?: g.comment ?: "Ocena",
                    weight = g.weight,
                    color = g.color,
                    dateString = dateStr,
                )
            }
            .toPersistentList()

        val tomorrow = today.clone().stepForward(0, 0, 1)
        val tomorrowLessons = primary.lessons
            .filter { it.displayDate?.value == tomorrow.value && !it.isCancelled && it.type != Lesson.TYPE_NO_LESSONS }
            .sortedBy { it.displayStartTime?.value }

        val tomorrowExamCount = secondary.upcomingEvents.count { event ->
            event.date.value == tomorrow.value &&
                (event.type == Event.TYPE_EXAM || event.type == Event.TYPE_SHORT_QUIZ)
        }
        val tomorrowPreview = if (tomorrowLessons.isNotEmpty()) {
            val first = tomorrowLessons.first()
            val shareLines = tomorrowLessons.mapIndexed { index, lesson ->
                val time = lesson.displayStartTime?.stringHM ?: "—"
                val subject = lesson.displaySubjectName ?: "Lekcja"
                val room = lesson.displayClassroom?.let { " (sala $it)" }.orEmpty()
                "${index + 1}. $time $subject$room"
            }
            TodayTomorrowPreviewUi(
                dayOfWeek = tomorrow.nominativeWeekdayTitle(),
                dateLabel = tomorrow.formattedString,
                lessonCount = tomorrowLessons.size,
                firstLessonSubject = first.displaySubjectName ?: "Lekcja",
                firstLessonTime = first.displayStartTime?.stringHM ?: "—",
                firstLessonRoom = first.displayClassroom,
                examCount = tomorrowExamCount,
                endsAt = tomorrowLessons.lastOrNull()?.displayEndTime?.stringHM,
                shareText = buildString {
                    append("Jutro (${tomorrow.nominativeWeekdayTitle()}, ${tomorrow.formattedString}):\n")
                    shareLines.forEach { append(it).append('\n') }
                    if (tomorrowExamCount > 0) {
                        append("Sprawdziany: $tomorrowExamCount")
                    }
                }.trim(),
            )
        } else null

        val bellBar = when (schedule.hero.phase) {
            TodayPhase.BEFORE_CLASSES, TodayPhase.BREAK -> schedule.hero.lesson?.let { lesson ->
                TodayBellBarUi(
                    subject = lesson.subject,
                    minutesRemaining = schedule.hero.minutesRemaining,
                    room = lesson.room,
                    isBreak = schedule.hero.phase == TodayPhase.BREAK,
                )
            }
            else -> null
        }
        val isFriday = Calendar.getInstance(locale).apply { timeInMillis = now }
            .get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        scheduleLessonReminder(today, schedule, activeTodayLessons)
        NowLessonStore.publish(
            if (schedule.hero.phase == TodayPhase.IN_CLASS) {
                schedule.hero.lesson?.let { lesson ->
                    NowLessonUi(
                        subject = lesson.subject,
                        room = lesson.room,
                        progress = schedule.hero.progress,
                        minutesRemaining = schedule.hero.minutesRemaining,
                        endsAt = lesson.endsAt,
                    )
                }
            } else {
                null
            },
        )

        return TodayUiState(
            isLoading = false,
            profileIncomplete = primary.profileIncomplete,
            userName = studentName.orEmpty(),
            greetingTitle = greetingTitle,
            heroDateLabel = heroDateLabel,
            dayOfWeekLabel = today.nominativeWeekdayTitle(),
            dateLabel = "${today.nominativeWeekdayTitle()}, ${today.formattedString}",
            lastSyncMinutes = lastSync?.let { ((now - it).coerceAtLeast(0L) / 60_000L) },
            hero = schedule.hero,
            nextLessons = schedule.nextLessons.toPersistentList(),
            attention = attention,
            upcoming = upcoming,
            luckyNumber = luckyNumberVal,
            isUserLuckyNumber = isUserLucky,
            changesCount = todayLessons.count { it.isCancelled || it.isChange },
            recentGrades = recentGrades,
            tomorrowPreview = tomorrowPreview,
            tomorrowLessons = tomorrowLessons.map { it.toLessonUi() }.toPersistentList(),
            fridayFinish = isFriday && schedule.hero.phase == TodayPhase.AFTER_CLASSES,
            bellBar = bellBar,
            cardOrder = app.config[profileId].ui.todayCardOrder.ifEmpty {
                listOf("homework", "upcoming", "grades")
            },
            homeworkList = secondary.homeworkTomorrow
                .map { hw ->
                    TodayHomeworkUi(
                        id = hw.id,
                        subjectName = hw.subjectShortName ?: hw.subjectLongName ?: "Zadanie",
                        topic = hw.getNoteSubstituteText(false)?.toString() ?: hw.topicHtml?.toString() ?: hw.topic ?: "Brak opisu",
                        dueDateLabel = hw.date.formattedString,
                        isDone = hw.isDone,
                    )
                }
                .toPersistentList(),
            isDemoProfile = primary.profile?.loginStoreType == pl.szczodrzynski.edziennik.data.enums.LoginType.DEMO,
            simulatedDayTitle = pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDayManager.currentOption.value.title,
        )
    }

    fun toggleHomeworkDone(eventId: Long, isDone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = db.eventDao().getByIdNow(profileId, eventId) ?: return@launch
            event.isDone = isDone
            db.eventDao().update(event)
        }
    }

    private data class Schedule(
        val hero: TodayHeroUi,
        val nextLessons: List<TodayLessonUi>,
    )

    private fun buildSchedule(lessons: List<LessonFull>, correctedNow: Long): Schedule {
        if (lessons.isEmpty()) {
            return Schedule(TodayHeroUi(phase = TodayPhase.FREE_DAY), emptyList())
        }

        val nowSeconds = Time.fromMillis(correctedNow).inSeconds
        val currentIndex = lessons.indices
            .filter { index ->
                val lesson = lessons[index]
                val start = lesson.displayStartTime?.inSeconds ?: return@filter false
                val end = lesson.displayEndTime?.inSeconds ?: return@filter false
                nowSeconds >= start && nowSeconds < end
            }
            .maxByOrNull { lessons[it].visualPriority() }
            ?: -1
        val nextIndex = lessons.indexOfFirst {
            (it.displayStartTime?.inSeconds ?: Long.MIN_VALUE) > nowSeconds
        }

        val heroIndex: Int
        val hero = when {
            currentIndex >= 0 -> {
                heroIndex = currentIndex
                val lesson = lessons[currentIndex]
                val start = lesson.displayStartTime!!.inSeconds
                val end = lesson.displayEndTime!!.inSeconds
                TodayHeroUi(
                    phase = TodayPhase.IN_CLASS,
                    lesson = lesson.toLessonUi(),
                    minutesRemaining = minutesUntil(end, nowSeconds),
                    progress = ((nowSeconds - start).toFloat() / (end - start).coerceAtLeast(1L))
                        .coerceIn(0f, 1f),
                )
            }
            nextIndex == 0 -> {
                heroIndex = 0
                val lesson = lessons.first()
                TodayHeroUi(
                    phase = TodayPhase.BEFORE_CLASSES,
                    lesson = lesson.toLessonUi(),
                    minutesRemaining = minutesUntil(lesson.displayStartTime!!.inSeconds, nowSeconds),
                )
            }
            nextIndex > 0 -> {
                heroIndex = nextIndex
                val lesson = lessons[nextIndex]
                TodayHeroUi(
                    phase = TodayPhase.BREAK,
                    lesson = lesson.toLessonUi(),
                    minutesRemaining = minutesUntil(lesson.displayStartTime!!.inSeconds, nowSeconds),
                )
            }
            else -> {
                heroIndex = lessons.lastIndex
                TodayHeroUi(phase = TodayPhase.AFTER_CLASSES)
            }
        }.withDaySummary(lessons)

        val following = if (hero.phase == TodayPhase.AFTER_CLASSES) {
            emptyList()
        } else {
            lessons.drop(heroIndex + 1).take(NEXT_LESSON_LIMIT).map { it.toLessonUi() }
        }
        return Schedule(hero, following)
    }

    private fun TodayHeroUi.withDaySummary(lessons: List<LessonFull>): TodayHeroUi {
        val activeLessons = lessons.filterNot(LessonFull::isCancelled)
        return copy(
            lessonCount = activeLessons.size,
            dayStartsAt = activeLessons.firstOrNull()?.displayStartTime?.stringHM,
            dayEndsAt = activeLessons.lastOrNull()?.displayEndTime?.stringHM,
        )
    }

    private fun LessonFull.visualPriority(): Int = when (type) {
        Lesson.TYPE_CANCELLED -> 3
        Lesson.TYPE_CHANGE -> 2
        Lesson.TYPE_SHIFTED_SOURCE, Lesson.TYPE_SHIFTED_TARGET -> 2
        else -> 0
    }

    private fun minutesUntil(targetSeconds: Long, nowSeconds: Long): Int =
        ceil((targetSeconds - nowSeconds).coerceAtLeast(0L) / 60.0).toInt()

    private fun LessonFull.toLessonUi(): TodayLessonUi {
        val statusKind = when (type) {
            Lesson.TYPE_CANCELLED -> TodayLessonStatusKind.CANCELLED
            Lesson.TYPE_CHANGE -> TodayLessonStatusKind.CHANGED
            Lesson.TYPE_SHIFTED_SOURCE, Lesson.TYPE_SHIFTED_TARGET -> TodayLessonStatusKind.SHIFTED
            else -> null
        }
        val statusDetails = when (statusKind) {
            TodayLessonStatusKind.CHANGED -> listOf(changeSubjectName, changeClassroom)
                .filter(String::isNotBlank)
                .joinToString(" • ")
                .takeIf(String::isNotBlank)
            TodayLessonStatusKind.CANCELLED,
            TodayLessonStatusKind.SHIFTED,
            null,
            -> null
        }
        return TodayLessonUi(
            id = id,
            subject = displaySubjectName.orEmpty().ifBlank { "—" },
            room = displayClassroom?.takeIf(String::isNotBlank),
            teacher = displayTeacherName?.takeIf(String::isNotBlank),
            startsAt = displayStartTime?.stringHM.orEmpty(),
            endsAt = displayEndTime?.stringHM.orEmpty(),
            status = statusKind?.let { TodayLessonStatusUi(it, statusDetails) },
        )
    }

    fun moveTodayCard(key: String, delta: Int) {
        val ui = app.config[profileId].ui
        val order = ui.todayCardOrder.toMutableList().ifEmpty {
            mutableListOf("homework", "upcoming", "grades")
        }
        val index = order.indexOf(key)
        if (index < 0) return
        val target = (index + delta).coerceIn(0, order.lastIndex)
        if (target == index) return
        order.removeAt(index)
        order.add(target, key)
        ui.todayCardOrder = order
        settingsTrigger.value++
    }

    fun toggleAttentionOption(key: String, enabled: Boolean) {
        val ui = app.config[profileId].ui
        when (key) {
            "grades" -> ui.checkGrades = enabled
            "messages" -> ui.checkMessages = enabled
            "announcements" -> ui.checkAnnouncements = enabled
            "notices" -> ui.checkNotices = enabled
            "homework" -> ui.checkHomework = enabled
        }
        settingsTrigger.value++
    }

    private fun EventFull.toUpcomingUi(today: Date): TodayUpcomingUi {
        val diff = Date.diffDays(date, today)
        val relLabel = when (diff) {
            0 -> "Dziś"
            1 -> "Jutro"
            2 -> "Pojutrze"
            in 3..6 -> date.dayOfWeekLabel()
            else -> date.formattedStringShort
        }
        return TodayUpcomingUi(
            id = id,
            title = topicHtml.toString().ifBlank { typeName.orEmpty().ifBlank { "—" } },
            supportingText = subjectLongName?.takeIf(String::isNotBlank)
                ?: typeName?.takeIf(String::isNotBlank)
                ?: teacherName?.takeIf(String::isNotBlank),
            dateLabel = date.formattedStringShort,
            relativeDateLabel = relLabel,
            kind = when (type) {
                Event.TYPE_EXAM -> TodayUpcomingKind.EXAM
                Event.TYPE_SHORT_QUIZ -> TodayUpcomingKind.QUIZ
                Event.TYPE_PROJECT -> TodayUpcomingKind.PROJECT
                else -> TodayUpcomingKind.DEADLINE
            },
        )
    }

    private fun Date.dayOfWeekLabel(): String = nominativeWeekdayTitle()

    private fun Date.nominativeWeekdayTitle(): String {
        val name = when (weekDay) {
            0 -> "poniedziałek"
            1 -> "wtorek"
            2 -> "środa"
            3 -> "czwartek"
            4 -> "piątek"
            5 -> "sobota"
            else -> "niedziela"
        }
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun scheduleLessonReminder(
        today: Date,
        schedule: Schedule,
        todayLessons: List<LessonFull>,
    ) {
        val target = when (schedule.hero.phase) {
            TodayPhase.BEFORE_CLASSES, TodayPhase.BREAK -> todayLessons.firstOrNull { lesson ->
                lesson.displayStartTime?.stringHM == schedule.hero.lesson?.startsAt
            }
            else -> null
        }
        if (target == null) {
            if (scheduledReminderLessonId != null) {
                LessonReminderWorker.cancel(app)
                scheduledReminderLessonId = null
            }
            return
        }
        if (scheduledReminderLessonId == target.id) return
        val startMillis = today.combineWith(target.displayStartTime)
        LessonReminderWorker.schedule(
            app = app,
            startMillis = startMillis,
            subject = target.displaySubjectName.orEmpty(),
            room = target.displayClassroom,
        )
        scheduledReminderLessonId = target.id
    }

    companion object {
        private const val CLOCK_INTERVAL_MILLIS = 15_000L
        private const val RETRY_INTERVAL_MILLIS = 5_000L

        private const val FUTURE_SEARCH_DAYS = 14
        private const val UPCOMING_DISPLAY_LIMIT = 4
        private const val NEXT_LESSON_LIMIT = 3

        fun factory(app: App, profileId: Int): ViewModelProvider.Factory {
            val bellCorrectionMillis = app.config.timetable.bellSyncDiff?.let { difference ->
                difference.inSeconds * 1_000L * app.config.timetable.bellSyncMultiplier
            } ?: 0L
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TodayViewModel::class.java))
                    return TodayViewModel(
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
