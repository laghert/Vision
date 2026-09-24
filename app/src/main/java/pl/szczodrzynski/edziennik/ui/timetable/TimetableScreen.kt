package pl.szczodrzynski.edziennik.ui.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Room
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.designsystem.CalmPullRefresh
import pl.szczodrzynski.edziennik.ui.designsystem.calmFocusShimmer
import kotlinx.coroutines.delay
import pl.szczodrzynski.edziennik.ui.today.TodayLessonStatusKind
import pl.szczodrzynski.edziennik.ui.today.TodayUpcomingKind
import pl.szczodrzynski.edziennik.utils.models.Date

private val BentoRadius = 20.dp
private val PillRadius = 100.dp

@Composable
fun TimetableRoute(
    activity: MainActivity,
    profileId: Int,
    modifier: Modifier = Modifier,
) {
    val app = activity.application as App
    val factory = remember(app, profileId) { TimetableViewModel.factory(app, profileId) }
    val viewModel: TimetableViewModel = viewModel(
        key = "timetable-$profileId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TimetableScreen(
        state = state,
        onSelectDate = viewModel::selectDate,
        onStepWeek = viewModel::stepWeek,
        onStepDay = viewModel::stepDay,
        onStepAdjacentWeek = viewModel::stepToAdjacentWeek,
        onSelectToday = viewModel::selectToday,
        onRefresh = { viewModel.syncCurrentWeek(force = true) },
        onShareWeek = { TimetableShare.shareWeek(activity, it) },
        onLessonLongPress = { viewModel.openLessonNotes(activity, it) },
        modifier = modifier,
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onSelectDate: (Date) -> Unit,
    onStepWeek: (Int) -> Unit,
    onStepDay: (Int) -> Unit,
    onStepAdjacentWeek: (Boolean) -> Unit,
    onSelectToday: () -> Unit,
    onRefresh: () -> Unit,
    onShareWeek: (TimetableUiState) -> Unit,
    onLessonLongPress: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showWeekGrid by remember { mutableStateOf(false) }
    var selectedLessonForDetails by remember { mutableStateOf<TimetableItemUi.Lesson?>(null) }
    val haptic = LocalHapticFeedback.current
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            onRefresh()
            delay(1100)
            refreshing = false
        }
    }

    val pageCount = state.days.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val selectedIndex = state.days.indexOfFirst { it.date.value == state.selectedDate.value }
    val currentState by rememberUpdatedState(state)
    val currentOnSelectDate by rememberUpdatedState(onSelectDate)

    LaunchedEffect(pagerState) {
        var userDrag = false
        snapshotFlow { pagerState.isScrollInProgress to pagerState.settledPage }
            .collect { (scrolling, page) ->
                if (scrolling) {
                    userDrag = true
                    return@collect
                }
                if (!userDrag) return@collect
                userDrag = false
                val date = currentState.days.getOrNull(page)?.date ?: return@collect
                if (date.value != currentState.selectedDate.value) {
                    currentOnSelectDate(date)
                }
            }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val highlightedDate = state.days.getOrNull(pagerState.currentPage)?.date?.value
        ?: state.selectedDate.value

    CalmPullRefresh(
        refreshing = refreshing,
        onRefresh = { refreshing = true },
        modifier = modifier,
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        TimetableTopBar(
            weekTitle = state.weekTitle,
            showTodayButton = !state.isTodaySelected,
            onStepWeek = { step ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onStepWeek(step)
            },
            onSelectToday = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectToday()
            },
            onOpenCalendar = { showDatePicker = true },
            onOpenWeekGrid = { showWeekGrid = true },
            onShare = { onShareWeek(state) },
            onRefresh = { refreshing = true },
        )

        TimetableWeekStrip(
            days = state.days,
            highlightedDate = highlightedDate,
            onSelectDate = { date ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectDate(date)
            },
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val day = state.days.getOrNull(page)
            if (state.isLoading || day == null) {
                TimetableLoadingShimmer()
            } else {
                TimetableDayPage(
                    day = day,
                    onNextDay = { onStepDay(1) },
                    onRefresh = onRefresh,
                    onLessonClick = { lesson ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedLessonForDetails = lesson
                    },
                    onLessonLongPress = { lesson ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLessonLongPress(lesson.id)
                    },
                )
            }
        }
    }
    }

    selectedLessonForDetails?.let { lesson ->
        LessonDetailsBottomSheet(
            lesson = lesson,
            onDismiss = { selectedLessonForDetails = null },
        )
    }

    if (showWeekGrid) {
        ModalBottomSheet(
            onDismissRequest = { showWeekGrid = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TimetableWeekGrid(
                days = state.days,
                onSelectDate = { date ->
                    onSelectDate(date)
                    showWeekGrid = false
                },
                onShare = {
                    onShareWeek(state)
                    showWeekGrid = false
                },
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.inMillisUtc,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onSelectDate(Date.fromMillisUtc(it))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimetableTopBar(
    weekTitle: String,
    showTodayButton: Boolean,
    onStepWeek: (Int) -> Unit,
    onSelectToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenWeekGrid: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onStepWeek(-1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Poprzedni tydzień", modifier = Modifier.size(22.dp))
        }

        Text(
            text = weekTitle.ifBlank { "Plan lekcji" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenCalendar)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            textAlign = TextAlign.Center,
        )

        IconButton(onClick = { onStepWeek(1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Następny tydzień", modifier = Modifier.size(22.dp))
        }

        if (showTodayButton) {
            Surface(
                shape = RoundedCornerShape(PillRadius),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clickable(onClick = onSelectToday),
            ) {
                Text(
                    text = "Dziś",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Box {
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Więcej", modifier = Modifier.size(19.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Siatka tygodnia") },
                    leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
                    onClick = { menuOpen = false; onOpenWeekGrid() },
                )
                DropdownMenuItem(
                    text = { Text("Udostępnij plan") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = { menuOpen = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text("Wybierz datę") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    onClick = { menuOpen = false; onOpenCalendar() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.today_refresh)) },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    onClick = { menuOpen = false; onRefresh() },
                )
            }
        }
    }
}

@Composable
private fun TimetableWeekStrip(
    days: List<TimetableDayUi>,
    highlightedDate: Int,
    onSelectDate: (Date) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            days.forEach { day ->
                TimetableDayPill(
                    day = day,
                    selected = day.date.value == highlightedDate,
                    onClick = { onSelectDate(day.date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (days.any { it.hasExams || it.hasChanges }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
                Text(
                    text = "  sprawdzian   ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
                Text(
                    text = "  zastępstwo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimetableDayPill(
    day: TimetableDayUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DayNumberBg",
    )
    val numberContent = if (selected) MaterialTheme.colorScheme.onPrimary else {
        if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = day.dayOfWeekShort,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected || day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (selected || day.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        Surface(
            shape = CircleShape,
            color = numberColor,
            border = if (day.isToday && !selected) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            } else {
                null
            },
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = numberContent,
                )
            }
        }

        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (day.hasExams) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
            if (day.hasChanges) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

/**
 * Lesson Row: Left-side timeline ruler (hours + vertical guide) + Right-side Bento Card.
 */
@Composable
private fun TimetableLessonRow(
    lesson: TimetableItemUi.Lesson,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    muted: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .graphicsLayer { alpha = if (muted) 0.48f else 1f },
        verticalAlignment = Alignment.Top,
    ) {
        // Left Column: Ruler with Hours and vertical line
        Column(
            modifier = Modifier
                .width(54.dp)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = lesson.startTime,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                color = if (lesson.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )

            if (lesson.isCurrent) {
                NowPulseDot()
            } else {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .width(2.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                )
            }

            Text(
                text = lesson.endTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        // Right Column: Lesson Bento Card
        TimetableLessonCard(
            lesson = lesson,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Break row: small badge in left timeline ruler + horizontal divider on the right.
 */
@Composable
private fun TimetableBreakRow(breakItem: TimetableItemUi.Break) {
    val isLong = breakItem.durationMinutes >= 20

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Column: Break pill
        Box(
            modifier = Modifier
                .width(54.dp)
                .padding(end = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (breakItem.isCurrent) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (breakItem.isCurrent) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                ),
            ) {
                Text(
                    text = "${breakItem.durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (breakItem.isCurrent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }

        // Right Column: Horizontal divider + break text
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp,
            )

            Icon(
                imageVector = Icons.Outlined.Coffee,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
                text = if (isLong) "Długa przerwa (${breakItem.startTime}–${breakItem.endTime})"
                else "Przerwa (${breakItem.startTime}–${breakItem.endTime})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp,
            )
        }
    }
}

/**
 * Lesson Bento Card in modern iOS/MD3 hybrid layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimetableLessonCard(
    lesson: TimetableItemUi.Lesson,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isCancelled = lesson.statusKind == TodayLessonStatusKind.CANCELLED
    val isChanged = lesson.statusKind in listOf(TodayLessonStatusKind.CHANGED, TodayLessonStatusKind.SHIFTED)

    val containerColor = when {
        lesson.isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isChanged -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val borderColor = when {
        lesson.isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        isCancelled -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        isChanged -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val subjectColor = Color(lesson.colorArgb)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (lesson.isCurrent) Modifier.shadow(10.dp, RoundedCornerShape(BentoRadius))
                else Modifier,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(BentoRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, if (lesson.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else borderColor),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(subjectColor),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            // Top Row: Number badge + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Number Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (lesson.isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = if (lesson.isCurrent) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = lesson.number?.toString() ?: "•",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Status or Live indicator
                if (lesson.isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(PillRadius),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = "Trwa • ${lesson.minutesRemaining} min",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else if (lesson.statusKind != null) {
                    val statusText = when (lesson.statusKind) {
                        TodayLessonStatusKind.CANCELLED -> "Odwołana"
                        TodayLessonStatusKind.CHANGED -> "Zastępstwo"
                        TodayLessonStatusKind.SHIFTED -> "Przesunięta"
                    }
                    val badgeColor = if (isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    Surface(
                        shape = RoundedCornerShape(PillRadius),
                        color = badgeColor.copy(alpha = 0.18f),
                    ) {
                        Text(
                            text = lesson.statusDetails?.let { "$statusText • $it" } ?: statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // Subject name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            if (lesson.originalSubject != null) {
                Text(
                    text = "Zamiast: ${lesson.originalSubject}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            lesson.notePreview?.takeIf { it.isNotBlank() }?.let { note ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Meta tags (Room & Teacher)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                lesson.classroom?.let {
                    LessonMetaChip(icon = Icons.Outlined.Room, text = "Sala $it")
                }
                lesson.teacher?.let {
                    LessonMetaChip(icon = Icons.Outlined.PersonOutline, text = it)
                }
            }

            // Live progress bar if lesson is ongoing
            if (lesson.isCurrent) {
                LinearProgressIndicator(
                    progress = { lesson.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(PillRadius)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                )
            }

            // Linked Exams / Tests / Homework badges
            if (lesson.events.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lesson.events.forEach { event ->
                        val (icon, color) = when (event.kind) {
                            TodayUpcomingKind.EXAM -> Pair(Icons.Outlined.School, MaterialTheme.colorScheme.error)
                            TodayUpcomingKind.QUIZ -> Pair(Icons.Outlined.Schedule, MaterialTheme.colorScheme.tertiary)
                            TodayUpcomingKind.PROJECT -> Pair(Icons.AutoMirrored.Outlined.Assignment, MaterialTheme.colorScheme.secondary)
                            TodayUpcomingKind.DEADLINE -> Pair(Icons.Outlined.CalendarMonth, MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = color)
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = color,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun NowPulseDot() {
    val pulse = rememberInfiniteTransition(label = "nowPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nowScale",
    )
    val alpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nowAlpha",
    )
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun LessonMetaChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(PillRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Interactive Lesson Details Bottom Sheet (iOS Modal Sheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonDetailsBottomSheet(
    lesson: TimetableItemUi.Lesson,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(100.dp),
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row: Subject, number, close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = lesson.number?.toString() ?: "•",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = lesson.subject,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${lesson.startTime} – ${lesson.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Check, contentDescription = "Zamknij")
                }
            }

            // Ongoing class card
            if (lesson.isCurrent) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Lekcja w trakcie",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Zostało: ${lesson.minutesRemaining} min",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { lesson.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }

            // Substitution / Change notice
            if (lesson.statusKind != null) {
                val isCancelled = lesson.statusKind == TodayLessonStatusKind.CANCELLED
                val cardColor = if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                val titleColor = if (isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isCancelled) "Lekcja odwołana" else "Zastępstwo / Zmiana",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                        )
                        lesson.statusDetails?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (lesson.originalSubject != null) {
                            Text(
                                text = "Oryginalny przedmiot: ${lesson.originalSubject}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (lesson.originalTeacher != null) {
                            Text(
                                text = "Zastępstwo za: ${lesson.originalTeacher}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Details Bento Grid: Sala & Nauczyciel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Sala
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Room, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Sala", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = lesson.classroom?.let { "Sala $it" } ?: "Nie określono",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Nauczyciel
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Nauczyciel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = lesson.teacher ?: "Nie określono",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Grupa (jeśli jest)
            lesson.groupName?.let { group ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "Grupa: $group", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Linked exams / tests / homeworks
            if (lesson.events.isNotEmpty()) {
                Text(
                    text = "Powiązane wydarzenia i sprawdziany",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                lesson.events.forEach { event ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = when (event.kind) {
                                        TodayUpcomingKind.EXAM -> "Sprawdzian"
                                        TodayUpcomingKind.QUIZ -> "Kartkówka"
                                        TodayUpcomingKind.PROJECT -> "Projekt"
                                        TodayUpcomingKind.DEADLINE -> "Zadanie domowe"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            val shareContext = androidx.compose.ui.platform.LocalContext.current
            TextButton(
                onClick = {
                    val text = buildString {
                        append(lesson.subject)
                        append(" • ")
                        append(lesson.startTime)
                        append("–")
                        append(lesson.endTime)
                        lesson.classroom?.let { append(" • sala $it") }
                    }
                    shareContext.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            },
                            shareContext.getString(R.string.share_intent),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Udostępnij lekcję")
            }
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(PillRadius),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text("Zamknij", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Not Downloaded Card: displayed when the timetable for the selected week has not been downloaded yet.
 */
@Composable
private fun TimetableNotDownloadedCard(
    isLoading: Boolean,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(BentoRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp,
                )
                Text(
                    text = "Pobieranie planu lekcji…",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Pobieramy plan z serwera dziennika.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Plan nie został pobrany",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Plan lekcji na ten tydzień nie znajduje się jeszcze w pamięci urządzenia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(PillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pobierz plan lekcji")
                }
            }
        }
    }
}

/**
 * Free Day Card: displayed when there are no lessons on the selected day.
 */
@Composable
private fun TimetableFreeDayCard(
    onNextDay: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(BentoRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Weekend,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Dzień wolny",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Nic w planie. Zobacz następny dzień albo odśwież.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(PillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Odśwież")
                }
                Button(
                    onClick = onNextDay,
                    shape = RoundedCornerShape(PillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Następny dzień")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimetableDayPage(
    day: TimetableDayUi,
    onNextDay: () -> Unit,
    onRefresh: () -> Unit,
    onLessonClick: (TimetableItemUi.Lesson) -> Unit,
    onLessonLongPress: (TimetableItemUi.Lesson) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(day.date.value) {
        if (day.currentLessonIndex >= 0) {
            listState.animateScrollToItem(day.currentLessonIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AnimatedContent(
                targetState = Triple(day.date.value, day.title, day.summary),
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(200)) { it / 8 }) togetherWith
                        (fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { -it / 8 })
                },
                label = "DayTitle",
                modifier = Modifier.weight(1f),
            ) { (_, title, summary) ->
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (day.currentLessonIndex >= 0) {
                Surface(
                    shape = RoundedCornerShape(PillRadius),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable {
                        coroutineScope.launch { listState.animateScrollToItem(day.currentLessonIndex) }
                    },
                ) {
                    Text(
                        text = "Teraz",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        if (day.isNotDownloaded) {
            TimetableNotDownloadedCard(
                isLoading = day.isLoading,
                onDownload = onRefresh,
            )
        } else if (day.isFreeDay || day.items.isEmpty()) {
            TimetableFreeDayCard(
                onNextDay = onNextDay,
                onRefresh = onRefresh,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(day.items) { item ->
                    when (item) {
                        is TimetableItemUi.Break -> TimetableBreakRow(item)
                        is TimetableItemUi.Lesson -> TimetableLessonRow(
                            lesson = item,
                            onClick = { onLessonClick(item) },
                            onLongClick = { onLessonLongPress(item) },
                            muted = day.currentLessonIndex >= 0 && !item.isCurrent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableWeekGrid(
    days: List<TimetableDayUi>,
    onSelectDate: (Date) -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tydzień",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Udostępnij")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEach { day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                day.isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                day.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                        )
                        .clickable { onSelectDate(day.date) }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = day.dayOfWeekShort,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (day.preview.isEmpty()) {
                        Text(
                            text = "Wolne",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        day.preview.take(6).forEach { lesson ->
                            Text(
                                text = "${lesson.startTime} ${lesson.subject}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (lesson.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                                color = when {
                                    lesson.isCancelled -> MaterialTheme.colorScheme.error
                                    lesson.isChanged -> MaterialTheme.colorScheme.tertiary
                                    lesson.isCurrent -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableLoadingShimmer() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(BentoRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .calmFocusShimmer(),
            )
        }
    }
}
