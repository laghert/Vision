package pl.szczodrzynski.edziennik.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Room
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.enums.NavTarget

data class TodaySearchHit(
    val title: String,
    val subtitle: String,
    val kind: Kind,
    val target: NavTarget,
) {
    enum class Kind { TEACHER, ROOM, SUBJECT }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaySearchSheet(
    app: App,
    profileId: Int,
    onNavigate: (NavTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf(emptyList<TodaySearchHit>()) }

    LaunchedEffect(query) {
        hits = withContext(Dispatchers.IO) {
            searchProfile(app, profileId, query)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Szukaj", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nauczyciel, sala, przedmiot") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            if (query.isBlank()) {
                Text(
                    text = "Wpisz min. 2 znaki.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (hits.isEmpty()) {
                Text(
                    text = "Nic nie znaleziono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(hits) { hit ->
                        ListItem(
                            headlineContent = { Text(hit.title) },
                            supportingContent = { Text(hit.subtitle) },
                            leadingContent = {
                                Icon(hit.kind.icon(), contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                onNavigate(hit.target)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun TodaySearchHit.Kind.icon(): ImageVector = when (this) {
    TodaySearchHit.Kind.TEACHER -> Icons.Outlined.PersonOutline
    TodaySearchHit.Kind.ROOM -> Icons.Outlined.Room
    TodaySearchHit.Kind.SUBJECT -> Icons.Outlined.School
}

private fun searchProfile(app: App, profileId: Int, raw: String): List<TodaySearchHit> {
    val query = raw.trim().lowercase()
    if (query.length < 2) return emptyList()
    val hits = mutableListOf<TodaySearchHit>()
    app.db.teacherDao().getAllNow(profileId).forEach { teacher ->
        val name = teacher.fullName.orEmpty()
        if (name.lowercase().contains(query)) {
            hits += TodaySearchHit(name, "Nauczyciel", TodaySearchHit.Kind.TEACHER, NavTarget.MESSAGES)
        }
    }
    app.db.classroomDao().getAllNow(profileId).forEach { room ->
        if (room.name.lowercase().contains(query)) {
            hits += TodaySearchHit(room.name, "Sala", TodaySearchHit.Kind.ROOM, NavTarget.TIMETABLE)
        }
    }
    app.db.subjectDao().getAllNow(profileId).forEach { subject ->
        val longName = subject.longName.orEmpty()
        val shortName = subject.shortName.orEmpty()
        if (longName.lowercase().contains(query) || shortName.lowercase().contains(query)) {
            hits += TodaySearchHit(longName.ifBlank { shortName }, "Przedmiot", TodaySearchHit.Kind.SUBJECT, NavTarget.GRADES)
        }
    }
    return hits.take(40)
}
