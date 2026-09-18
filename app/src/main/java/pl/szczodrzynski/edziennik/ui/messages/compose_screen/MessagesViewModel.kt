package pl.szczodrzynski.edziennik.ui.messages.compose_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.models.Date

class MessagesViewModel(
    private val app: App,
    private val profileId: Int,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(MessagesTab.RECEIVED)
    private val searchQuery = MutableStateFlow("")

    val uiState = combine(
        app.db.messageDao().getAll(profileId).asFlow(),
        app.db.teacherDao().getAllTeachers(profileId).asFlow(),
        selectedTab,
        searchQuery,
    ) { allMessages, teachers, tab, query ->
        val teachersMap = teachers.associateBy { it.id }

        // Filter messages for current tab
        val tabMessages = allMessages.filter { it.type == tab.type }

        // Format and map messages
        val mappedList = tabMessages.map { message ->
            val senderOrRecipient = if (message.isSent) {
                val recipients = message.recipients
                if (!recipients.isNullOrEmpty()) {
                    recipients.joinToString(", ") { r ->
                        r.fullName ?: teachersMap[r.id]?.fullName ?: ""
                    }.ifBlank { "Odbiorca" }
                } else {
                    "Odbiorca"
                }
            } else {
                message.senderName?.ifBlank { null }
                    ?: teachersMap[message.senderId]?.fullName
                    ?: "Nadawca"
            }

            val snippet = message.bodyHtml?.toString()?.take(160)?.replace("\n", " ")?.trim().orEmpty()
            val dateText = try {
                Date.fromMillis(message.addedDate).formattedStringShort
            } catch (_: Exception) {
                ""
            }

            val isUnread = !message.isSent && !message.isDraft && !message.seen

            MessageItemUi(
                id = message.id,
                senderOrRecipient = senderOrRecipient,
                subject = message.subject.ifBlank { "(Brak tematu)" },
                snippet = snippet,
                dateText = dateText,
                isStarred = message.isStarred,
                isUnread = isUnread,
                hasAttachments = message.hasAttachments,
                isDraft = message.isDraft,
                rawJson = app.gson.toJson(message),
            )
        }

        // Apply search query filter if present
        val filteredList = if (query.isBlank()) {
            mappedList
        } else {
            val q = query.lowercase().trim()
            mappedList.filter {
                it.senderOrRecipient.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q) ||
                    it.snippet.lowercase().contains(q)
            }
        }

        val unreadInTab = mappedList.count { it.isUnread }

        MessagesUiState(
            isLoading = false,
            selectedTab = tab,
            searchQuery = query,
            messages = filteredList.toPersistentList(),
            totalCount = mappedList.size,
            unreadCount = unreadInTab,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessagesUiState(isLoading = true),
    )

    fun selectTab(tab: MessagesTab) {
        selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleStar(messageId: Long, isStarred: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = app.db.messageDao().getByIdNow(profileId, messageId) ?: return@launch
            app.messageManager.starMessage(message, !isStarred)
        }
    }

    fun markSeen(messageId: Long, seen: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = app.db.messageDao().getByIdNow(profileId, messageId) ?: return@launch
            app.db.metadataDao().setSeen(profileId, message, seen)
        }
    }

    companion object {
        fun factory(app: App, profileId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MessagesViewModel(app, profileId) as T
            }
        }
    }
}
