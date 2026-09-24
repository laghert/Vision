/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.web.VulcanWebLuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

class VulcanData(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanData"
        private const val MAX_CONCURRENCY = 4

        private val phase1Endpoints = setOf(
            ENDPOINT_VULCAN_HEBE_MAIN,
            ENDPOINT_VULCAN_HEBE_ADDRESSBOOK,
            ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2,
            ENDPOINT_VULCAN_HEBE_TEACHERS,
            ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES,
        )
    }

    private var firstSemesterSync = false
    private val firstSemesterSyncExclude = listOf(
        ENDPOINT_VULCAN_HEBE_MAIN,
        ENDPOINT_VULCAN_HEBE_PUSH_CONFIG,
        ENDPOINT_VULCAN_HEBE_ADDRESSBOOK,
        ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2,
        ENDPOINT_VULCAN_HEBE_TIMETABLE,
        ENDPOINT_VULCAN_HEBE_EXAMS,
        ENDPOINT_VULCAN_HEBE_HOMEWORK,
        ENDPOINT_VULCAN_HEBE_NOTICES,
        ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES,
        ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX,
        ENDPOINT_VULCAN_HEBE_MESSAGES_SENT,
        ENDPOINT_VULCAN_HEBE_TEACHERS,
        ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER
    )

    init {
        if (data.studentSemesterNumber == 2 && data.profile?.empty != false) {
            firstSemesterSync = true
            data.studentSemesterId = data.semester1Id
            data.studentSemesterNumber = 1
        }

        val phase1 = mutableListOf<Pair<Int, Long?>>()
        val phase2 = mutableListOf<Pair<Int, Long?>>()
        synchronized(data.targetEndpoints) {
            for ((id, lastSync) in data.targetEndpoints) {
                if (id in phase1Endpoints) {
                    phase1.add(id to lastSync)
                } else {
                    phase2.add(id to lastSync)
                }
            }
        }

        runBatchInParallel(phase1) {
            runBatchInParallel(phase2) {
                if (firstSemesterSync) {
                    data.studentSemesterId = data.semester2Id
                    data.studentSemesterNumber = 2
                }
                onSuccess()
            }
        }
    }

    private fun runBatchInParallel(
        items: List<Pair<Int, Long?>>,
        maxConcurrency: Int = MAX_CONCURRENCY,
        onBatchFinished: () -> Unit,
    ) {
        if (items.isEmpty() || data.cancelled) {
            onBatchFinished()
            return
        }

        val queue = ArrayDeque(items)
        val remaining = AtomicInteger(items.size)
        val lock = Any()
        var finished = false

        fun checkAndLaunchNext() {
            if (data.cancelled) {
                synchronized(lock) {
                    if (!finished) {
                        finished = true
                        onBatchFinished()
                    }
                }
                return
            }

            val nextItem: Pair<Int, Long?>?
            synchronized(lock) {
                nextItem = if (queue.isNotEmpty()) queue.removeFirst() else null
            }

            if (nextItem == null) return

            val (endpointId, lastSync) = nextItem

            val onEndpointComplete: (Int) -> Unit = {
                synchronized(data.targetEndpoints) {
                    data.targetEndpoints.remove(endpointId)
                }
                data.progress(data.progressStep)
                val left = remaining.decrementAndGet()
                if (left <= 0) {
                    synchronized(lock) {
                        if (!finished) {
                            finished = true
                            onBatchFinished()
                        }
                    }
                } else {
                    checkAndLaunchNext()
                }
            }

            if (firstSemesterSync && endpointId !in firstSemesterSyncExclude) {
                useEndpoint(endpointId, lastSync) {
                    data.studentSemesterId = data.semester2Id
                    data.studentSemesterNumber = 2
                    useEndpoint(endpointId, lastSync) {
                        data.studentSemesterId = data.semester1Id
                        data.studentSemesterNumber = 1
                        onEndpointComplete(endpointId)
                    }
                }
            } else {
                useEndpoint(endpointId, lastSync, onEndpointComplete)
            }
        }

        synchronized(lock) {
            val launchCount = minOf(maxConcurrency, items.size)
            for (i in 0 until launchCount) {
                checkAndLaunchNext()
            }
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        Timber.d("Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                VulcanWebLuckyNumber(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_MAIN -> {
                if (data.profile == null) {
                    onSuccess(ENDPOINT_VULCAN_HEBE_MAIN)
                    return
                }
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                VulcanHebeMain(data, lastSync).getStudents(
                    profile = data.profile,
                    profileList = null
                ) {
                    onSuccess(ENDPOINT_VULCAN_HEBE_MAIN)
                }
            }
            ENDPOINT_VULCAN_HEBE_PUSH_CONFIG -> {
                data.startProgress(R.string.edziennik_progress_endpoint_push_config)
                VulcanHebePushConfig(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_ADDRESSBOOK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_addressbook)
                VulcanHebeAddressbook(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2 -> {
                data.startProgress(R.string.edziennik_progress_endpoint_addressbook)
                VulcanHebeAddressbook2(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_TEACHERS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teachers)
                VulcanHebeTeachers(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_TIMETABLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                VulcanHebeTimetable(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_EXAMS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_exams)
                VulcanHebeExams(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                VulcanHebeGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY -> {
                data.startProgress(R.string.edziennik_progress_endpoint_proposed_grades)
                VulcanHebeGradeSummary(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                VulcanHebeHomework(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_notices)
                VulcanHebeNotices(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                VulcanHebeAttendance(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages)
                VulcanHebeMessageBoxes(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                VulcanHebeMessages(data, lastSync, onSuccess).getMessages(Message.TYPE_RECEIVED)
            }
            ENDPOINT_VULCAN_HEBE_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                VulcanHebeMessages(data, lastSync, onSuccess).getMessages(Message.TYPE_SENT)
            }
            ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                VulcanHebeLuckyNumber(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}
