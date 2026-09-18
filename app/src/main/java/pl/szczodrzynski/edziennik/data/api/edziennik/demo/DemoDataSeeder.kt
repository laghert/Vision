package pl.szczodrzynski.edziennik.data.api.edziennik.demo

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.Calendar

object DemoDataSeeder {
    fun seed(app: App, profileId: Int) {
        val db = app.db

        // 1. TEACHERS
        val teachers = listOf(
            Teacher(profileId, 101L, "Anna", "Kowalska").apply {
                this.type = Teacher.TYPE_TEACHER
                this.typeDescription = "Nauczyciel matematyki"
            },
            Teacher(profileId, 102L, "Piotr", "Nowak").apply {
                this.type = Teacher.TYPE_EDUCATOR or Teacher.TYPE_TEACHER
                this.typeDescription = "Wychowawca, Język polski"
            },
            Teacher(profileId, 103L, "Marta", "Zielińska").apply {
                this.type = Teacher.TYPE_TEACHER
                this.typeDescription = "Język angielski"
            },
            Teacher(profileId, 104L, "Tomasz", "Wiśniewski").apply {
                this.type = Teacher.TYPE_TEACHER
                this.typeDescription = "Informatyka"
            },
            Teacher(profileId, 105L, "Krzysztof", "Lewandowski").apply {
                this.type = Teacher.TYPE_TEACHER
                this.typeDescription = "Fizyka"
            },
            Teacher(profileId, 106L, "Maria", "Dąbrowska").apply {
                this.type = Teacher.TYPE_PRINCIPAL
                this.typeDescription = "Dyrektor szkoły"
            },
        )
        teachers.forEach { db.teacherDao().add(it) }

        // 2. SUBJECTS
        val subjects = listOf(
            Subject(profileId, 1L, "Matematyka", "MAT"),
            Subject(profileId, 2L, "Język polski", "POL"),
            Subject(profileId, 3L, "Język angielski", "ANG"),
            Subject(profileId, 4L, "Informatyka", "INF"),
            Subject(profileId, 5L, "Fizyka", "FIZ"),
            Subject(profileId, 6L, "Biologia", "BIO"),
            Subject(profileId, 7L, "Historia", "HIS"),
            Subject(profileId, 8L, "Wychowanie fizyczne", "WF"),
            Subject(profileId, 9L, "Chemia", "CHM"),
        )
        subjects.forEach { db.subjectDao().add(it) }

        // 3. GRADES (with one subject < 2.0 GPA to showcase ⚠️ Zagrożenie!)
        val grades = listOf(
            // Matematyka: 5, 4+, 5- -> GPA ~4.67
            Grade(profileId, 201L, "5", Grade.TYPE_NORMAL, 5f, 3f, 0xFF4CAF50.toInt(), "Sprawdzian", "Geometria analityczna", null, 1, 101L, 1L),
            Grade(profileId, 202L, "4+", Grade.TYPE_NORMAL, 4.5f, 2f, 0xFF8BC34A.toInt(), "Kartkówka", "Wektory", null, 1, 101L, 1L),
            Grade(profileId, 203L, "5-", Grade.TYPE_NORMAL, 4.75f, 1f, 0xFF4CAF50.toInt(), "Odpowiedź", "Funkcja liniowa", null, 1, 101L, 1L),

            // Język polski: 4, 3+, 5 -> GPA ~4.0
            Grade(profileId, 204L, "4", Grade.TYPE_NORMAL, 4f, 3f, 0xFF2196F3.toInt(), "Wypracowanie", "Motyw cierpienia w Dziadach cz. III", null, 1, 102L, 2L),
            Grade(profileId, 205L, "3+", Grade.TYPE_NORMAL, 3.5f, 1f, 0xFFFF9800.toInt(), "Kartkówka", "Środki stylistyczne", null, 1, 102L, 2L),
            Grade(profileId, 206L, "5", Grade.TYPE_NORMAL, 5f, 2f, 0xFF4CAF50.toInt(), "Projekt", "Prezentacja epoki romantyzmu", null, 1, 102L, 2L),

            // Język angielski: 6, 5, 5+ -> GPA ~5.5
            Grade(profileId, 207L, "6", Grade.TYPE_NORMAL, 6f, 3f, 0xFF009688.toInt(), "Sprawdzian", "Unit 3 - Advanced Grammar", null, 1, 103L, 3L),
            Grade(profileId, 208L, "5", Grade.TYPE_NORMAL, 5f, 2f, 0xFF4CAF50.toInt(), "Kartkówka", "Phrasal Verbs", null, 1, 103L, 3L),

            // Informatyka: 6, 6, 5 -> GPA ~5.8
            Grade(profileId, 209L, "6", Grade.TYPE_NORMAL, 6f, 3f, 0xFF9C27B0.toInt(), "Sprawdzian", "Algorytmy sortowania w Pythonie", null, 1, 104L, 4L),
            Grade(profileId, 210L, "6", Grade.TYPE_NORMAL, 6f, 2f, 0xFF9C27B0.toInt(), "Projekt", "Aplikacja webowa", null, 1, 104L, 4L),

            // Fizyka: 1, 2, 1+ -> GPA ~1.44 (ZAGROŻENIE!)
            Grade(profileId, 211L, "1", Grade.TYPE_NORMAL, 1f, 3f, 0xFFF44336.toInt(), "Sprawdzian", "Dynamika i siły bezwładności", null, 1, 105L, 5L),
            Grade(profileId, 212L, "2", Grade.TYPE_NORMAL, 2f, 2f, 0xFFFF5722.toInt(), "Kartkówka", "Zasady dynamiki Newtona", null, 1, 105L, 5L),
            Grade(profileId, 213L, "1+", Grade.TYPE_NORMAL, 1.5f, 1f, 0xFFF44336.toInt(), "Odpowiedź", "Ruch jednostajny prostoliniowy", null, 1, 105L, 5L),

            // Biologia: 4, 5 -> GPA ~4.5
            Grade(profileId, 214L, "4", Grade.TYPE_NORMAL, 4f, 2f, 0xFF4CAF50.toInt(), "Sprawdzian", "Genetyka molekularna", null, 1, 106L, 6L),
            Grade(profileId, 215L, "5", Grade.TYPE_NORMAL, 5f, 1f, 0xFF4CAF50.toInt(), "Zadanie", "Budowa DNA", null, 1, 106L, 6L),
        )
        grades.forEach { db.gradeDao().add(it) }

        // 4. FULL WEEK LESSONS (Mon - Fri)
        val today = Date.getToday()
        val monday = today.weekStart
        val nowCal = Calendar.getInstance()
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val baseHour = if (currentHour in 7..16) currentHour else 8

        val weekLessons = mutableListOf<Lesson>()
        var lessonId = 300L

        for (dayIdx in 0..4) {
            val dayDate = monday.clone().stepForward(0, 0, dayIdx)
            val isDayToday = dayDate.value == today.value

            val daySchedule = when (dayIdx) {
                0 -> listOf( // Poniedziałek
                    Triple(2L, 102L, "204"), // Język polski
                    Triple(1L, 101L, "105"), // Matematyka
                    Triple(3L, 103L, "302"), // Język angielski
                    Triple(7L, 102L, "108"), // Historia
                    Triple(8L, 104L, "Hala"), // WF
                )
                1 -> listOf( // Wtorek
                    Triple(1L, 101L, "105"), // Matematyka
                    Triple(5L, 105L, "210"), // Fizyka
                    Triple(6L, 106L, "BIO 2"), // Biologia
                    Triple(2L, 102L, "204"), // Język polski
                    Triple(8L, 104L, "Hala"), // WF
                )
                2 -> listOf( // Środa
                    Triple(4L, 104L, "LAB 1"), // Informatyka
                    Triple(4L, 104L, "LAB 1"), // Informatyka
                    Triple(3L, 103L, "302"), // Język angielski
                    Triple(1L, 101L, "105"), // Matematyka
                    Triple(6L, 106L, "BIO 2"), // Biologia
                )
                3 -> listOf( // Czwartek
                    Triple(2L, 102L, "204"), // Język polski
                    Triple(2L, 102L, "204"), // Język polski
                    Triple(5L, 105L, "210"), // Fizyka (Zastępstwo)
                    Triple(7L, 102L, "108"), // Historia
                    Triple(3L, 103L, "302"), // Język angielski
                    Triple(8L, 104L, "Hala"), // WF
                )
                else -> listOf( // Piątek
                    Triple(1L, 101L, "105"), // Matematyka
                    Triple(3L, 103L, "302"), // Język angielski
                    Triple(6L, 106L, "BIO 2"), // Biologia
                    Triple(8L, 104L, "Hala"), // WF
                    Triple(2L, 102L, "204"), // Godzina z wychowawcą
                )
            }

            daySchedule.forEachIndexed { idx, (subjId, teachId, room) ->
                lessonId++
                val (startH, startM, endH, endM) = if (isDayToday) {
                    val h = baseHour - 1 + idx
                    listOf(h, 0, h, 45)
                } else {
                    when (idx) {
                        0 -> listOf(8, 0, 8, 45)
                        1 -> listOf(8, 55, 9, 40)
                        2 -> listOf(9, 50, 10, 35)
                        3 -> listOf(10, 45, 11, 30)
                        4 -> listOf(11, 45, 12, 30)
                        else -> listOf(12, 45, 13, 30)
                    }
                }

                weekLessons.add(
                    Lesson(profileId, lessonId).apply {
                        date = dayDate
                        lessonNumber = idx + 1
                        startTime = Time(startH, startM, 0)
                        endTime = Time(endH, endM, 0)
                        subjectId = subjId
                        teacherId = teachId
                        classroom = room
                        if (dayIdx == 3 && idx == 2) {
                            type = Lesson.TYPE_CHANGE
                        }
                    },
                )
            }
        }
        db.timetableDao().replaceAll(weekLessons)

        // 5. EVENT TYPES & EVENTS (Homework, Quizzes, Exams)
        val eventTypes = listOf(
            EventType(profileId, Event.TYPE_EXAM, "Sprawdzian", Event.COLOR_EXAM),
            EventType(profileId, Event.TYPE_SHORT_QUIZ, "Kartkówka", Event.COLOR_SHORT_QUIZ),
            EventType(profileId, Event.TYPE_HOMEWORK, "Zadanie domowe", Event.COLOR_HOMEWORK),
            EventType(profileId, Event.TYPE_ESSAY, "Wypracowanie", Event.COLOR_ESSAY),
            EventType(profileId, Event.TYPE_PROJECT, "Projekt", Event.COLOR_PROJECT),
        )
        db.eventTypeDao().addAll(eventTypes)

        val tomorrowDate = today.clone().stepForward(0, 0, 1)
        val events = listOf(
            Event(profileId, 401L, today, Time(14, 0, 0), "Zadania 1-5 ze strony 142 (Układy równań)", Event.COLOR_HOMEWORK, Event.TYPE_HOMEWORK, 101L, 1L, 0L).apply { isDone = false },
            Event(profileId, 402L, tomorrowDate, Time(8, 0, 0), "Esej: 'Rola fatum w tragedii antycznej' (min. 250 słów)", Event.COLOR_HOMEWORK, Event.TYPE_HOMEWORK, 102L, 2L, 0L).apply { isDone = false },
            Event(profileId, 403L, tomorrowDate, Time(10, 0, 0), "Nauka słówek z rozdziału 4 (Technology & Future)", Event.COLOR_HOMEWORK, Event.TYPE_HOMEWORK, 103L, 3L, 0L).apply { isDone = true },
            Event(profileId, 404L, today.clone().stepForward(0, 0, 2), Time(9, 0, 0), "Sprawdzian: Rachunek prawdopodobieństwa", Event.COLOR_EXAM, Event.TYPE_EXAM, 101L, 1L, 0L),
            Event(profileId, 405L, tomorrowDate, Time(11, 0, 0), "Kartkówka: Prawa Keplera i grawitacja", Event.COLOR_SHORT_QUIZ, Event.TYPE_SHORT_QUIZ, 105L, 5L, 0L),
            Event(profileId, 406L, today.clone().stepForward(0, 0, 3), Time(12, 0, 0), "Kartkówka: Słownictwo Unit 4", Event.COLOR_SHORT_QUIZ, Event.TYPE_SHORT_QUIZ, 103L, 3L, 0L),
        )
        db.eventDao().replaceAll(events)

        // 6. MESSAGES
        val messages = listOf(
            Message(profileId, 501L, Message.TYPE_RECEIVED, "Szczegóły wycieczki do Centrum Nauki Kopernik", "Dzień dobry,\nPrzypominam, że zbiórka przed szkołą o 7:45. Proszę o zabranie legitymacji szkolnych oraz zgód rodziców.\n\nPozdrawiam,\nPiotr Nowak", 102L).apply {
                isStarred = true
            },
            Message(profileId, 502L, Message.TYPE_RECEIVED, "Konsultacje przed sprawdzianem z matematyki", "Cześć Janek,\nW czwartek na 7. godzinie lekcyjnej odbędą się dodatkowe konsultacje z prawdopodobieństwa dla chętnych.\n\nAnna Kowalska", 101L).apply {
                isStarred = false
            },
            Message(profileId, 503L, Message.TYPE_RECEIVED, "Wyniki konkursu z programowania", "Gratulacje! Zakwalifikowałeś się do etapu okręgowego Olimpiady Informatycznej. Szczegóły prześlę w kolejnej wiadomości.\n\nTomasz Wiśniewski", 104L).apply {
                isStarred = true
            },
        )
        db.messageDao().replaceAll(messages)

        // 7. ANNOUNCEMENTS
        val announcements = listOf(
            Announcement(profileId, 601L, "Organizacja Dnia Sportu i Dnia Otwartego", "Szanowni Uczniowie i Rodzice,\n\nW najbliższy piątek odbędzie się coroczny szkolny Dzień Sportu połączony z Dniem Otwartym dla kandydatów do naszej szkoły.\n\nLekcje będą skrócone do 30 minut, a po 4. lekcji rozpoczną się zawody w hali sportowej.\n\nSerdecznie zapraszamy do aktywnego udziału i kibicowania!\n\nZ wyrazami szacunku,\nDyrekcja Szkoły", today, today.clone().stepForward(0, 0, 7), 106L),
            Announcement(profileId, 602L, "Zbiórka charytatywna dla schroniska", "Samorząd Uczniowski zaprasza wszystkich uczniów do włączenia się w zbiórkę karmy, ciepłych koców i zabawek dla lokalnego schroniska dla zwierząt.\n\nDary można zostawiać w sali 102 do końca bieżącego miesiąca. Każdy gest ma znaczenie!", today, tomorrowDate, 102L),
        )
        db.announcementDao().replaceAll(announcements)

        // 8. ATTENDANCE RECORDS (give ~94.5% presence)
        val attendances = listOf(
            Attendance(profileId, 701L, Attendance.TYPE_PRESENT, "Obecność", "ob", "ob", 0xFF009688.toInt(), today, Time(8, 0, 0), 1, 102L, 2L),
            Attendance(profileId, 702L, Attendance.TYPE_PRESENT, "Obecność", "ob", "ob", 0xFF009688.toInt(), today, Time(9, 0, 0), 1, 101L, 1L),
            Attendance(profileId, 703L, Attendance.TYPE_BELATED, "Spóźnienie", "sp", "sp", 0xFFFFC107.toInt(), today, Time(10, 0, 0), 1, 103L, 3L),
            Attendance(profileId, 704L, Attendance.TYPE_PRESENT, "Obecność", "ob", "ob", 0xFF009688.toInt(), today, Time(11, 0, 0), 1, 104L, 4L),
            Attendance(profileId, 705L, Attendance.TYPE_ABSENT_EXCUSED, "Usprawiedliwiona", "u", "u", 0xFF8BC34A.toInt(), today.clone().stepForward(0, 0, -1), Time(8, 0, 0), 1, 105L, 5L),
            Attendance(profileId, 706L, Attendance.TYPE_PRESENT, "Obecność", "ob", "ob", 0xFF009688.toInt(), today.clone().stepForward(0, 0, -1), Time(9, 0, 0), 1, 101L, 1L),
            Attendance(profileId, 707L, Attendance.TYPE_PRESENT, "Obecność", "ob", "ob", 0xFF009688.toInt(), today.clone().stepForward(0, 0, -2), Time(8, 0, 0), 1, 102L, 2L),
        )
        db.attendanceDao().replaceAll(attendances)

        // 9. LUCKY NUMBER (Set to 17, matching studentNumber = 17 for VIP Gold Card!)
        db.luckyNumberDao().add(LuckyNumber(profileId, today, 17))
    }
}
