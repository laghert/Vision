package pl.szczodrzynski.edziennik.ui.navigation

import pl.szczodrzynski.edziennik.data.enums.NavTarget

/** Type-safe route values used at the MainActivity/Compose Navigation boundary. */
sealed interface CalmFocusRoute {
    val value: String

    data class Screen(
        val target: NavTarget,
        val entryId: Long,
    ) : CalmFocusRoute {
        override val value = "screen/${target.id}/$entryId"
    }

    data object More : CalmFocusRoute {
        override val value = "more"
    }

    companion object {
        const val SCREEN_PATTERN = "screen/{targetId}/{entryId}"
        const val TARGET_ID_ARGUMENT = "targetId"
        const val ENTRY_ID_ARGUMENT = "entryId"
        const val SCREEN_ARGUMENTS_KEY = "screenArguments"
        const val FRAGMENT_CONTAINER_ID_KEY = "fragmentContainerId"
    }
}
