package pl.szczodrzynski.edziennik.ui.messages.compose_screen

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Message

enum class MessagesTab(val type: Int, val titleRes: Int) {
    RECEIVED(Message.TYPE_RECEIVED, R.string.messages_tab_received),
    SENT(Message.TYPE_SENT, R.string.messages_tab_sent),
    DELETED(Message.TYPE_DELETED, R.string.messages_tab_deleted),
    DRAFT(Message.TYPE_DRAFT, R.string.messages_tab_draft),
}

@Immutable
data class MessageItemUi(
    val id: Long,
    val senderOrRecipient: String,
    val subject: String,
    val snippet: String,
    val dateText: String,
    val isStarred: Boolean,
    val isUnread: Boolean,
    val hasAttachments: Boolean,
    val isDraft: Boolean,
    val rawJson: String,
)

@Immutable
data class MessagesUiState(
    val isLoading: Boolean = true,
    val selectedTab: MessagesTab = MessagesTab.RECEIVED,
    val searchQuery: String = "",
    val messages: PersistentList<MessageItemUi> = persistentListOf(),
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
)
