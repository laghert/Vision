package pl.szczodrzynski.edziennik.ui.today

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentListOf
import pl.szczodrzynski.edziennik.data.enums.NavTarget

class MockTodayPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentLesson = TodayLessonUi(
            id = 101L,
            subject = "Fizyka",
            room = "sala 104",
            teacher = "dr hab. Anna Kowalska",
            startsAt = "09:45",
            endsAt = "10:30",
        )

        val nextLessonsList = persistentListOf(
            TodayLessonUi(
                id = 102L,
                subject = "Informatyka",
                room = "pracownia 3",
                teacher = "mgr Piotr Zieliński",
                startsAt = "10:45",
                endsAt = "11:30",
            ),
            TodayLessonUi(
                id = 103L,
                subject = "Język angielski",
                room = "sala 12",
                teacher = "mgr Ewa Wiśniewska",
                startsAt = "11:40",
                endsAt = "12:25",
            ),
            TodayLessonUi(
                id = 104L,
                subject = "Historia",
                room = "sala 108",
                teacher = "mgr Marek Lewandowski",
                startsAt = "12:45",
                endsAt = "13:30",
            ),
        )

        val gradesList = persistentListOf(
            TodayRecentGradeUi(
                id = 1L,
                gradeValue = "5",
                subjectName = "Matematyka",
                categoryName = "Sprawdzian: Geometria analityczna",
                weight = 3f,
                color = 0xFF4CAF50.toInt(),
                dateString = "dzisiaj",
            ),
            TodayRecentGradeUi(
                id = 2L,
                gradeValue = "5+",
                subjectName = "Fizyka",
                categoryName = "Odpowiedź ustna - Termodynamika",
                weight = 2f,
                color = 0xFF2196F3.toInt(),
                dateString = "dzisiaj",
            ),
            TodayRecentGradeUi(
                id = 3L,
                gradeValue = "6",
                subjectName = "Informatyka",
                categoryName = "Projekt aplikacji mobilnej",
                weight = 4f,
                color = 0xFF9C27B0.toInt(),
                dateString = "wczoraj",
            ),
        )

        val upcomingList = persistentListOf(
            TodayUpcomingUi(
                id = 1L,
                title = "Sprawdzian: II Wojna Światowa",
                supportingText = "Historia • Zakres: rozdział 4-5",
                dateLabel = "19 wrz",
                relativeDateLabel = "za 2 dni",
                kind = TodayUpcomingKind.EXAM,
            ),
            TodayUpcomingUi(
                id = 2L,
                title = "Kartkówka: Ciągi arytmetyczne",
                supportingText = "Matematyka",
                dateLabel = "18 wrz",
                relativeDateLabel = "jutro",
                kind = TodayUpcomingKind.QUIZ,
            ),
        )

        val homeworkList = persistentListOf(
            TodayHomeworkUi(
                id = 1L,
                subjectName = "Język polski",
                topic = "Wypracowanie: Interpretacja motywu wędrówki w literaturze",
                dueDateLabel = "jutro",
                isDone = false,
            ),
            TodayHomeworkUi(
                id = 2L,
                subjectName = "Matematyka",
                topic = "Zadania 1-5 ze strony 142",
                dueDateLabel = "za 2 dni",
                isDone = false,
            ),
        )

        val mockState = TodayUiState(
            isLoading = false,
            hasError = false,
            profileIncomplete = false,
            userName = "Jan",
            greetingTitle = "Dzień dobry, Jan!",
            heroDateLabel = "Czwartek, 17 września",
            dayOfWeekLabel = "Czwartek",
            dateLabel = "17 września",
            lastSyncMinutes = 3,
            hero = TodayHeroUi(
                phase = TodayPhase.IN_CLASS,
                lesson = currentLesson,
                minutesRemaining = 24,
                progress = 0.45f,
                lessonCount = 5,
                dayStartsAt = "08:00",
                dayEndsAt = "13:30",
            ),
            nextLessons = nextLessonsList,
            attention = TodayAttentionUi(
                newGrades = 2,
                unreadMessages = 1,
                newAnnouncements = 0,
                newNotices = 0,
                homeworkTomorrow = 1,
            ),
            upcoming = upcomingList,
            luckyNumber = 17,
            isUserLuckyNumber = false,
            changesCount = 0,
            recentGrades = gradesList,
            homeworkList = homeworkList,
            isDemoProfile = true,
            bellBar = TodayBellBarUi(
                subject = "Fizyka",
                minutesRemaining = 24,
                room = "sala 104",
                isBreak = false,
            ),
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0F1117),
                    surface = Color(0xFF161922),
                    surfaceVariant = Color(0xFF202431),
                    onBackground = Color(0xFFE6E8EE),
                    onSurface = Color(0xFFE6E8EE),
                    onSurfaceVariant = Color(0xFF9AA0B0),
                    primary = Color(0xFF6C8CFF),
                    primaryContainer = Color(0xFF253366),
                    onPrimaryContainer = Color(0xFFDCE2FF),
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodayScreen(
                        state = mockState,
                        profileId = 1,
                        onRefresh = {},
                        onOpenWeek = {},
                        onNavigateTarget = {},
                        onToggleAttention = { _, _ -> },
                        onToggleHomework = { _, _ -> },
                        onMoveCard = { _, _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
