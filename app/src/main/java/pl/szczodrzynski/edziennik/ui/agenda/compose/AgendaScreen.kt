package pl.szczodrzynski.edziennik.ui.agenda.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R

private val PillRadius = 100.dp

@Composable
fun AgendaRoute(
    activity: MainActivity,
    profileId: Int,
    modifier: Modifier = Modifier,
) {
    val app = activity.application as App
    val factory = remember(app, profileId) { AgendaViewModel.factory(app, profileId) }
    val viewModel: AgendaViewModel = viewModel(
        key = "agenda-$profileId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AgendaScreen(
        state = state,
        onRefresh = activity::retryProfileSync,
        onSelectFilter = viewModel::setFilter,
        onToggleFilterFromLastLogin = viewModel::toggleFilterFromLastLogin,
        onToggleCalendarStrip = viewModel::toggleCalendarStrip,
        onSelectDate = viewModel::selectDate,
        onToggleDone = viewModel::toggleEventDone,
        modifier = modifier,
    )
}

@Composable
fun AgendaScreen(
    state: AgendaUiState,
    onRefresh: () -> Unit,
    onSelectFilter: (AgendaFilter) -> Unit,
    onToggleFilterFromLastLogin: () -> Unit,
    onToggleCalendarStrip: () -> Unit,
    onSelectDate: (String?) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var pastEventsExpanded by rememberSaveable(state.upcomingEvents.isEmpty()) {
        mutableStateOf(state.upcomingEvents.isEmpty())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Header
        item(key = "agenda-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.menu_agenda),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.filterFromLastLogin) {
                            "${state.upcomingEvents.size + state.pastEvents.size} nowych od ostatniego logowania"
                        } else if (state.upcomingEvents.isNotEmpty()) {
                            "${state.upcomingEvents.size} nadchodzących wydarzeń"
                        } else {
                            "Brak nadchodzących wydarzeń"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.filterFromLastLogin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Sleek Calendar Toggle Button (hides/reveals the 14-day strip)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCalendarStrip()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isCalendarStripExpanded) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Pokaż kalendarz dni",
                            tint = if (state.isCalendarStripExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.today_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }

        // 14-Day Strip (Hidden by default, shown when toggled on)
        item(key = "agenda-day-strip") {
            AnimatedVisibility(
                visible = state.isCalendarStripExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Wybierz dzień z planu:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.selectedDateString != null) {
                            TextButton(
                                onClick = { onSelectDate(null) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("Pokaż wszystkie dni", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(state.days, key = { it.dateString }) { day ->
                            val isSelected = state.selectedDateString == day.dateString
                            val bgCol by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                label = "DayBg",
                            )
                            val textCol by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    day.isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                label = "DayText",
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = bgCol,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                ),
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectDate(day.dateString) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = day.dayOfWeek,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = textCol.copy(alpha = 0.8f),
                                    )
                                    Text(
                                        text = day.dayNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol,
                                    )
                                    if (day.hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    CircleShape,
                                                ),
                                        )
                                    } else {
                                        Spacer(Modifier.size(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Streamlined Filter Chips
        item(key = "agenda-filter-pills") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedFilter == AgendaFilter.ALL && !state.filterFromLastLogin,
                        onClick = {
                            if (state.filterFromLastLogin) onToggleFilterFromLastLogin()
                            onSelectFilter(AgendaFilter.ALL)
                        },
                        label = { Text("Wszystkie (${state.totalCount})") },
                        shape = RoundedCornerShape(PillRadius),
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedFilter == AgendaFilter.EXAMS && !state.filterFromLastLogin,
                        onClick = {
                            if (state.filterFromLastLogin) onToggleFilterFromLastLogin()
                            onSelectFilter(AgendaFilter.EXAMS)
                        },
                        label = { Text("🔴 Sprawdziany (${state.examsCount})") },
                        shape = RoundedCornerShape(PillRadius),
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedFilter == AgendaFilter.QUIZZES && !state.filterFromLastLogin,
                        onClick = {
                            if (state.filterFromLastLogin) onToggleFilterFromLastLogin()
                            onSelectFilter(AgendaFilter.QUIZZES)
                        },
                        label = { Text("🟠 Kartkówki (${state.quizzesCount})") },
                        shape = RoundedCornerShape(PillRadius),
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedFilter == AgendaFilter.HOMEWORK && !state.filterFromLastLogin,
                        onClick = {
                            if (state.filterFromLastLogin) onToggleFilterFromLastLogin()
                            onSelectFilter(AgendaFilter.HOMEWORK)
                        },
                        label = { Text("🟣 Zadania (${state.homeworkCount})") },
                        shape = RoundedCornerShape(PillRadius),
                    )
                }
                if (state.newSinceLastLoginCount > 0) {
                    item {
                        FilterChip(
                            selected = state.filterFromLastLogin,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFilterFromLastLogin()
                            },
                            label = { Text("✨ Od logowania (${state.newSinceLastLoginCount})") },
                            leadingIcon = {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            shape = RoundedCornerShape(PillRadius),
                        )
                    }
                }
            }
        }

        // UPCOMING EVENTS (Always displayed first!)
        if (state.upcomingEvents.isNotEmpty()) {
            item(key = "agenda-upcoming-header") {
                Text(
                    text = "Nadchodzące (${state.upcomingEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }

            items(state.upcomingEvents, key = { "up-${it.id}" }) { event ->
                AgendaEventCard(
                    event = event,
                    onToggleDone = { onToggleDone(event.id, !event.isDone) },
                )
            }
        } else if (state.pastEvents.isEmpty()) {
            // Completely empty state
            item(key = "agenda-empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(text = "🎉", fontSize = 40.sp)
                        Text(
                            text = "Brak zaplanowanych wydarzeń",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "W wybranym terminie nie masz żadnych sprawdzianów ani zadań.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // PAST EVENTS (Collapsible section below upcoming, NEVER shown first!)
        if (state.pastEvents.isNotEmpty()) {
            item(key = "agenda-past-header") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pastEventsExpanded = !pastEventsExpanded
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Przeszłe wydarzenia (${state.pastEvents.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Icon(
                            imageVector = if (pastEventsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (pastEventsExpanded) "Zwiń" else "Rozwiń",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (pastEventsExpanded) {
                items(state.pastEvents, key = { "past-${it.id}" }) { event ->
                    AgendaEventCard(
                        event = event,
                        onToggleDone = { onToggleDone(event.id, !event.isDone) },
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaEventCard(
    event: AgendaEventItemUi,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isQuiz = event.typeName == "Kartkówka"
    val isExam = event.typeName == "Sprawdzian"

    val stripeColor = when {
        isExam -> MaterialTheme.colorScheme.error
        isQuiz -> MaterialTheme.colorScheme.tertiary
        event.isHomework -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isPast) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (event.isPast) 0.dp else 1.dp),
        border = BorderStroke(
            1.dp,
            if (event.isFromLastLogin) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top row: Subject badge + New badge + countdown badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(stripeColor),
                    )
                    Text(
                        text = event.subjectName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (event.isFromLastLogin) {
                        Surface(
                            shape = RoundedCornerShape(PillRadius),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "NOWE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(PillRadius),
                    color = when (event.countdownLabel) {
                        "Dzisiaj", "Jutro" -> MaterialTheme.colorScheme.primary
                        "Przeszłe" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                ) {
                    Text(
                        text = event.countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (event.countdownLabel) {
                            "Dzisiaj", "Jutro" -> MaterialTheme.colorScheme.onPrimary
                            "Przeszłe" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }

            // Topic (Headline)
            Text(
                text = event.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (event.isDone || event.isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (event.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )

            // Bottom row: type badge, date & subtle homework checkmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = event.typeName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }

                    Text(
                        text = "${event.dateLabel}${if (event.timeLabel.isNotBlank()) " • ${event.timeLabel}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (event.isHomework) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(PillRadius),
                        color = if (event.isDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(PillRadius))
                            .clickable(onClick = onToggleDone),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(if (event.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.2.dp, if (event.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                            ) {
                                if (event.isDone) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                            Text(
                                text = if (event.isDone) "Zrobione" else "Do zrobienia",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (event.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
