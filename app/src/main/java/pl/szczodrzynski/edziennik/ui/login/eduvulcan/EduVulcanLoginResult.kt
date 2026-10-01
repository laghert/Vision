package pl.szczodrzynski.edziennik.ui.login.eduvulcan

data class EduVulcanLoginResult(
    val isError: Boolean = false,
    val token: String? = null,
    val symbol: String? = null,
    val pin: String? = null,
)
