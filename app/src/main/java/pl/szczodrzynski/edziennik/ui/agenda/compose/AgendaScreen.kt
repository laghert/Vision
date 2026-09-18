package pl.szczodrzynski.edziennik.ui.agenda.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onSelectDate: (String?) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        text = if (state.totalCount == 1) "1 zaplanowane wydarzenie" else "${state.totalCount} zaplanowanych wydarzeń",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.today_refresh),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 14-Day Strip (iOS Calendar style)
        item(key = "agenda-day-strip") {
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
                        shape = RoundedCornerShape(18.dp),
                        color = bgCol,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier
                            .width(54.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectDate(day.dateString) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
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
                            // Event indicator dot
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

        // Filter Pills
        item(key = "agenda-filter-pills") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    AgendaFilterChip(
                        label = "Wszystkie (${state.totalCount})",
                        isSelected = state.selectedFilter == AgendaFilter.ALL,
                        onClick = { onSelectFilter(AgendaFilter.ALL) },
                    )
                }
                item {
                    AgendaFilterChip(
                        label = "🔴 Sprawdziany (${state.examsCount})",
                        isSelected = state.selectedFilter == AgendaFilter.EXAMS,
                        onClick = { onSelectFilter(AgendaFilter.EXAMS) },
                    )
                }
                item {
                    AgendaFilterChip(
                        label = "🟠 Kartkówki (${state.quizzesCount})",
                        isSelected = state.selectedFilter == AgendaFilter.QUIZZES,
                        onClick = { onSelectFilter(AgendaFilter.QUIZZES) },
                    )
                }
                item {
                    AgendaFilterChip(
                        label = "🟣 Zadania (${state.homeworkCount})",
                        isSelected = state.selectedFilter == AgendaFilter.HOMEWORK,
                        onClick = { onSelectFilter(AgendaFilter.HOMEWORK) },
                    )
                }
            }
        }

        // Event List or Empty State
        if (state.events.isEmpty()) {
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
                        Text(
                            text = "🎉",
                            fontSize = 40.sp,
                        )
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
        } else {
            items(state.events, key = { it.id }) { event ->
                AgendaEventCard(
                    event = event,
                    onToggleDone = { onToggleDone(event.id, !event.isDone) },
                )
            }
        }
    }
}

@Composable
private fun AgendaFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        label = "FilterChipBg",
    )
    val textCol by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "FilterChipText",
    )

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bgCol,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        ),
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textCol,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AgendaEventCard(
    event: AgendaEventItemUi,
    onToggleDone: () -> Unit,
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top row: Subject badge + countdown badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (event.countdownLabel == "Dzisiaj" || event.countdownLabel == "Jutro") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                ) {
                    Text(
                        text = event.countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (event.countdownLabel == "Dzisiaj" || event.countdownLabel == "Jutro") {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            // Topic (Headline)
            Text(
                text = event.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (event.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (event.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )

            // Bottom row: type badge, date & optional homework toggle
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
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
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
                        shape = RoundedCornerShape(100.dp),
                        color = if (event.isDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(onClick = onToggleDone),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (event.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.5.dp, if (event.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                            ) {
                                if (event.isDone) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(11.dp),
                                    )
                                }
                            }
                            Text(
                                text = if (event.isDone) "Zrobione" else "Do zrobienia",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (event.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

