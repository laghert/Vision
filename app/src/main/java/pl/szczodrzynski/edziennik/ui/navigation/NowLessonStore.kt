package pl.szczodrzynski.edziennik.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowLessonUi(
    val subject: String,
    val room: String?,
    val progress: Float,
    val minutesRemaining: Int,
    val endsAt: String,
)

object NowLessonStore {
    private val _current = MutableStateFlow<NowLessonUi?>(null)
    val current: StateFlow<NowLessonUi?> = _current.asStateFlow()

    fun publish(value: NowLessonUi?) {
        _current.value = value
    }
}
