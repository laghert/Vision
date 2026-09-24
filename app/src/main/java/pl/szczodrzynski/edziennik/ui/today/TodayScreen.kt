package pl.szczodrzynski.edziennik.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Announcement
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Room
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import android.content.Intent
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.designsystem.CalmPullRefresh
import pl.szczodrzynski.edziennik.ui.designsystem.calmFocusShimmer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.lerp

private val BentoCardCornerRadius = 24.dp
private val PillCornerRadius = 100.dp

@Composable
fun TodayRoute(
    activity: MainActivity,
    profileId: Int,
    modifier: Modifier = Modifier,
) {
    val app = activity.application as App
    val factory = remember(app, profileId) { TodayViewModel.factory(app, profileId) }
    val viewModel: TodayViewModel = viewModel(
        key = "today-$profileId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        profileId = profileId,
        onRefresh = activity::retryProfileSync,
        onOpenWeek = { activity.selectShellTarget(NavTarget.TIMETABLE) },
        onNavigateTarget = { activity.selectShellTarget(it) },
        onMarkAllRead = activity::markAllSeen,
        onToggleAttention = viewModel::toggleAttentionOption,
        onToggleHomework = viewModel::toggleHomeworkDone,
        onMoveCard = viewModel::moveTodayCard,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    profileId: Int,
    onRefresh: () -> Unit,
    onOpenWeek: () -> Unit,
    onNavigateTarget: (NavTarget) -> Unit,
    onMarkAllRead: () -> Unit = {},
    onToggleAttention: (key: String, enabled: Boolean) -> Unit,
    onToggleHomework: (eventId: Long, isDone: Boolean) -> Unit,
    onMoveCard: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var isGreetingExpanded by rememberSaveable { mutableStateOf(true) }
    var showAttentionSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val shareTomorrow: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    context.getString(R.string.share_intent),
                ),
            )
        }
    }

    // Auto-collapse after welcoming intro duration
    LaunchedEffect(Unit) {
        delay(3500)
        isGreetingExpanded = false
    }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            onRefresh()
            delay(1100)
            refreshing = false
        }
    }
    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 88f).coerceIn(0f, 1f)
        }
    }

    val morphProgress by animateFloatAsState(
        targetValue = if (isGreetingExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "GreetingMorphProgress",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight

        CalmPullRefresh(
            refreshing = refreshing,
            onRefresh = { refreshing = true },
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp + (14.dp * (1f - morphProgress)),
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "today-header") {
                TodayCompactHeader(
                    state = state,
                    morphProgress = morphProgress,
                    collapse = collapse,
                    onRefresh = { refreshing = true },
                    onSearch = { showSearch = true },
                    onHeaderClick = { isGreetingExpanded = true },
                )
            }

            item(key = "today-tab-selector") {
                TodaySegmentedControl(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = it
                    },
                )
            }

            if (selectedTab == 0 && state.bellBar != null) {
                item(key = "today-bell-bar") {
                    TodayBellBar(state.bellBar)
                }
            }

            // Sync incomplete warning banner
            item(key = "today-sync-status") {
                AnimatedVisibility(
                    visible = state.profileIncomplete,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    TodaySyncIncomplete(onRetry = onRefresh)
                }
            }

            if (selectedTab == 0) {
                // --- TAB: DZISIAJ ---

                // Timetable changes & substitutions banner
                if (state.changesCount > 0) {
                    item(key = "today-changes-alert") {
                        TodayChangesAlertCard(
                            count = state.changesCount,
                            onOpenPlan = onOpenWeek,
                        )
                    }
                }

                if (state.isLoading) {
                    item(key = "today-loading") {
                        TodayLoadingShimmer()
                    }
                } else if (state.hasError) {
                    item(key = "today-error") {
                        TodayRecoveryCard(
                            title = stringResource(R.string.today_data_error_title),
                            supportingText = stringResource(R.string.today_data_error_support),
                            onRetry = onRefresh,
                        )
                    }
                } else {
                    // HERO: Live Activity Bento Card
                    if (state.fridayFinish) {
                        item(key = "today-friday-finish") {
                            TodayFridayFinishCard()
                        }
                    }

                    if (state.hero.phase == TodayPhase.FREE_DAY) {
                        item(key = "today-free-day") {
                            TodayFreeDay(onOpenWeek)
                        }
                    } else {
                        item(key = "today-hero") {
                            TodayLiveHero(
                                hero = state.hero,
                                nextLesson = state.nextLessons.firstOrNull(),
                                onOpenWeek = onOpenWeek,
                            )
                        }
                    }

                    // SZYBKIE AKCJE (iOS Quick Actions)
                    item(key = "today-quick-actions") {
                        TodayQuickActionsRow(
                            onOpenTimetable = onOpenWeek,
                            onOpenMessages = { onNavigateTarget(NavTarget.MESSAGES) },
                            onOpenGrades = { onNavigateTarget(NavTarget.GRADES) },
                            onOpenAgenda = { onNavigateTarget(NavTarget.AGENDA) },
                        )
                    }

                    // BENTO ROW: Szczęśliwy Numerek + Centrum Uwagi
                    item(key = "today-bento-quick-glance") {
                        TodayBentoQuickGlance(
                            state = state,
                            onOpenSettings = { showAttentionSheet = true },
                            onNavigateTarget = onNavigateTarget,
                            onMarkAllRead = onMarkAllRead,
                        )
                    }

                    state.cardOrder.forEach { cardKey ->
                        when (cardKey) {
                            "homework" -> if (state.homeworkList.isNotEmpty()) {
                                item(key = "today-homework") {
                                    TodayReorderWrap(cardKey, state.cardOrder, onMoveCard) {
                                        TodayHomeworkCard(
                                            homeworkList = state.homeworkList,
                                            onToggleHomework = onToggleHomework,
                                        )
                                    }
                                }
                            }
                            "upcoming" -> if (state.upcoming.isNotEmpty()) {
                                item(key = "today-upcoming") {
                                    TodayReorderWrap(cardKey, state.cardOrder, onMoveCard) {
                                        TodayUpcomingCard(
                                            events = state.upcoming,
                                            onOpenAgenda = { onNavigateTarget(NavTarget.AGENDA) },
                                        )
                                    }
                                }
                            }
                            "grades" -> if (state.recentGrades.isNotEmpty()) {
                                item(key = "today-recent-grades") {
                                    TodayReorderWrap(cardKey, state.cardOrder, onMoveCard) {
                                        TodayRecentGradesCard(
                                            grades = state.recentGrades,
                                            onOpenGrades = { onNavigateTarget(NavTarget.GRADES) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // JUTRZEJSZY START
                    if (state.tomorrowPreview != null && (state.hero.phase == TodayPhase.AFTER_CLASSES || state.hero.phase == TodayPhase.NEXT_CLASSES || state.hero.phase == TodayPhase.FREE_DAY)) {
                        item(key = "today-tomorrow-preview") {
                            TodayTomorrowPreviewCard(
                                preview = state.tomorrowPreview,
                                onOpenTimetable = { onNavigateTarget(NavTarget.TIMETABLE) },
                                onShare = { shareTomorrow(state.tomorrowPreview.shareText) },
                            )
                        }
                    }

                    // OŚ CZASU KOLEJNYCH LEKCJI
                    if (state.nextLessons.isNotEmpty()) {
                        item(key = "today-next-lessons") {
                            TodayConnectedTimeline(
                                lessons = state.nextLessons,
                                isNextDay = state.hero.phase == TodayPhase.NEXT_CLASSES,
                            )
                        }
                    }
                }
            } else {
                // --- TAB: JUTRO ---
                if (state.tomorrowPreview != null) {
                    item(key = "tomorrow-preview-banner") {
                        TodayTomorrowPreviewCard(
                            preview = state.tomorrowPreview,
                            onOpenTimetable = { onNavigateTarget(NavTarget.TIMETABLE) },
                            onShare = { shareTomorrow(state.tomorrowPreview.shareText) },
                        )
                    }
                }

                if (state.homeworkList.isNotEmpty()) {
                    item(key = "tomorrow-homework") {
                        TodayHomeworkCard(
                            homeworkList = state.homeworkList,
                            onToggleHomework = onToggleHomework,
                        )
                    }
                }

                if (state.tomorrowLessons.isNotEmpty()) {
                    item(key = "tomorrow-lessons-timeline") {
                        TodayConnectedTimeline(
                            lessons = state.tomorrowLessons,
                            isNextDay = true,
                        )
                    }
                } else {
                    item(key = "tomorrow-free-day") {
                        TodayFreeTomorrowCard()
                    }
                }
            }
        }
        }

        if (morphProgress > 0.005f) {
            TodayFullscreenGreetingOverlay(
                state = state,
                morphProgress = morphProgress,
                fullHeight = fullHeight,
                onDismiss = { isGreetingExpanded = false },
            )
        }
    }

    if (showSearch) {
        val app = (LocalContext.current.applicationContext as App)
        TodaySearchSheet(
            app = app,
            profileId = profileId,
            onNavigate = onNavigateTarget,
            onDismiss = { showSearch = false },
        )
    }

    if (showAttentionSheet) {
        TodayAttentionSettingsSheet(
            attention = state.attention,
            onToggle = onToggleAttention,
            onDismiss = { showAttentionSheet = false },
        )
    }
}

/**
 * Fullscreen greeting screen overlay with smooth spring morphing into top header,
 * featuring an iOS-inspired Aurora mesh glow, dynamic sun/moon halo and 3 glass summary pills.
 */
@Composable
private fun TodayFullscreenGreetingOverlay(
    state: TodayUiState,
    morphProgress: Float,
    fullHeight: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PromptBounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "BounceOffset",
    )
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "OrbPulse",
    )

    val currentHeight = fullHeight * morphProgress
    val contentAlpha = ((morphProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)

    val currentHour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    val (timeIconEmoji, ambientColor1, ambientColor2) = when {
        currentHour < 12 -> Triple(
            "☀️",
            Color(0xFFFFA726).copy(alpha = 0.32f),
            Color(0xFFFFD54F).copy(alpha = 0.22f),
        )
        currentHour in 12..17 -> Triple(
            "🌤️",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            Color(0xFF4FC3F7).copy(alpha = 0.22f),
        )
        else -> Triple(
            "🌙",
            Color(0xFF7E57C2).copy(alpha = 0.35f),
            Color(0xFF3F51B5).copy(alpha = 0.22f),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(currentHeight)
            .alpha(contentAlpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    ),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        // Floating Aurora Mesh Orbs in the background
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
                .size(240.dp)
                .scale(orbPulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ambientColor1, Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(180.dp)
                .scale(1f / orbPulse)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ambientColor2, Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Ambient glowing icon badge with dynamic halo
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = timeIconEmoji,
                        fontSize = 38.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Main Greeting Title (e.g. "Dzień dobry, Jan! 🌅")
            Text(
                text = state.greetingTitle.ifBlank { stringResource(R.string.nav_today) },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            // Date Subtitle pill
            Surface(
                shape = RoundedCornerShape(PillCornerRadius),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = state.heroDateLabel.ifBlank { state.dateLabel },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3 iOS-Style Glass Summary Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pill 1: Koniec zajęć
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Koniec",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.hero.dayEndsAt ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Pill 2: Liczba lekcji
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Lekcje",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${state.hero.lessonCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Pill 3: Szczęśliwy Numerek
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Numerek",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.luckyNumber?.let { "#$it" } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isUserLuckyNumber) Color(0xFFFFB300) else MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }

        // Tap or swipe down hint
        Surface(
            shape = RoundedCornerShape(PillCornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = bounceOffset.dp)
                .clickable(onClick = onDismiss),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Dotknij, aby przejść dalej",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Compact top-left header displayed in regular state.
 */
@Composable
private fun TodayCompactHeader(
    state: TodayUiState,
    morphProgress: Float,
    collapse: Float,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onHeaderClick: () -> Unit,
) {
    val alpha = (1f - morphProgress * 1.5f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Dzisiaj",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = lerp(34.sp, 22.sp, collapse),
                lineHeight = lerp(40.sp, 28.sp, collapse),
            ),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.greetingTitle.ifBlank { state.dayOfWeekLabel },
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = lerp(18.sp, 15.sp, collapse),
                lineHeight = lerp(24.sp, 20.sp, collapse),
            ),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderClick,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(PillCornerRadius),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.isLoading && state.lastSyncMinutes != null) {
                        Text(
                            text = when (val minutes = state.lastSyncMinutes) {
                                0L -> "teraz"
                                else -> "$minutes min"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearch),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Szukaj",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRefresh),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.today_refresh),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayReorderWrap(
    key: String,
    order: List<String>,
    onMove: (String, Int) -> Unit,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val index = order.indexOf(key)
    Box {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
        ) {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Kolejność karty",
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Przesuń w górę") },
                    enabled = index > 0,
                    onClick = {
                        menu = false
                        onMove(key, -1)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Przesuń w dół") },
                    enabled = index >= 0 && index < order.lastIndex,
                    onClick = {
                        menu = false
                        onMove(key, 1)
                    },
                )
            }
        }
    }
}

@Composable
private fun TodayQuickActionsRow(
    onOpenTimetable: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenAgenda: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            TodayQuickActionChip(
                icon = Icons.Outlined.CalendarMonth,
                label = "Cały tydzień",
                onClick = onOpenTimetable,
            )
        }
        item {
            TodayQuickActionChip(
                icon = Icons.Outlined.MailOutline,
                label = "Wiadomości",
                onClick = onOpenMessages,
            )
        }
        item {
            TodayQuickActionChip(
                icon = Icons.Outlined.Grade,
                label = "Oceny",
                onClick = onOpenGrades,
            )
        }
        item {
            TodayQuickActionChip(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                label = "Terminarz",
                onClick = onOpenAgenda,
            )
        }
    }
}

@Composable
private fun TodayQuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(PillCornerRadius))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * LIVE ACTIVITY HERO: Bento Hero Card with status pill, circular ring countdown, progress and next lesson teaser.
 */
@Composable
private fun TodayLiveHero(
    hero: TodayHeroUi,
    nextLesson: TodayLessonUi? = null,
    onOpenWeek: () -> Unit,
) {
    AnimatedContent(
        targetState = hero,
        contentKey = { Triple(it.phase, it.scheduleDateLabel, it.lesson?.id) },
        transitionSpec = {
            (fadeIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                )).togetherWith(fadeOut())
        },
        label = "TodayLessonPhase",
    ) { currentHero ->
        val isCancelled = currentHero.lesson?.status?.kind == TodayLessonStatusKind.CANCELLED
        val isChanged = currentHero.lesson?.status?.kind in listOf(TodayLessonStatusKind.CHANGED, TodayLessonStatusKind.SHIFTED)

        val containerColor = when {
            isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            isChanged -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        }

        val borderColor = when {
            isCancelled -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            isChanged -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BentoCardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top header row with Live Activity status pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TodayHeroPhasePill(currentHero)
                    currentHero.lesson?.status?.let { status -> LessonStatus(status) }
                }

                // Lesson details & title
                HeroContent(currentHero, nextLesson)

                // Day summary chips (total lessons, hours)
                if (currentHero.lessonCount > 0) {
                    TodayScheduleSummary(currentHero)
                }

                if (currentHero.phase == TodayPhase.NEXT_CLASSES) {
                    Button(
                        onClick = onOpenWeek,
                        shape = RoundedCornerShape(PillCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(stringResource(R.string.today_show_day))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePulseRadarDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "RadarScale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "RadarAlpha",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(14.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .alpha(alpha)
                .background(color, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
    }
}

@Composable
private fun TodayChangesAlertCard(
    count: Int,
    onOpenPlan: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenPlan),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (count == 1) "1 zmiana w planie dzisiaj" else "$count zmiany w planie dzisiaj",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "Dotknij, aby zobaczyć zastępstwa i odwołane lekcje w planie",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun TodayHeroPhasePill(hero: TodayHeroUi) {
    val (label, icon, color) = when (hero.phase) {
        TodayPhase.IN_CLASS -> Triple(
            "Trwa lekcja • do dzwonka ${hero.minutesRemaining} min",
            Icons.Outlined.Schedule,
            MaterialTheme.colorScheme.primary,
        )
        TodayPhase.BREAK -> Triple(
            "Przerwa • jeszcze ${hero.minutesRemaining} min",
            Icons.Outlined.Schedule,
            MaterialTheme.colorScheme.tertiary,
        )
        TodayPhase.BEFORE_CLASSES -> Triple(
            "Początek za ${hero.minutesRemaining} min",
            Icons.Outlined.Schedule,
            MaterialTheme.colorScheme.secondary,
        )
        TodayPhase.AFTER_CLASSES -> Triple(
            stringResource(R.string.today_classes_finished),
            Icons.Outlined.Check,
            MaterialTheme.colorScheme.outline,
        )
        TodayPhase.NEXT_CLASSES -> Triple(
            stringResource(R.string.today_next_classes),
            Icons.Outlined.CalendarMonth,
            MaterialTheme.colorScheme.primary,
        )
        TodayPhase.FREE_DAY -> Triple(
            stringResource(R.string.today_free_day),
            Icons.Outlined.Celebration,
            MaterialTheme.colorScheme.primary,
        )
    }

    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hero.phase == TodayPhase.IN_CLASS) {
                LivePulseRadarDot(color = color)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HeroContent(
    hero: TodayHeroUi,
    nextLesson: TodayLessonUi? = null,
) {
    val lesson = hero.lesson
    val animatedProgress by animateFloatAsState(
        targetValue = hero.progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "TodayLessonProgress",
    )

    when (hero.phase) {
        TodayPhase.BEFORE_CLASSES -> {
            Text(
                text = lesson?.subject.orEmpty().ifBlank { "Pierwsza lekcja" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            lesson?.let { HeroLessonDetails(it, showTeacher = false) }
        }
        TodayPhase.IN_CLASS -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson?.subject.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    lesson?.let { HeroLessonDetails(it, showTeacher = true) }
                }

                // Live Activity circular countdown ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(start = 6.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${hero.minutesRemaining}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "min",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Next lesson teaser pill
            if (nextLesson != null) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = buildString {
                                append("Następnie: ")
                                append(nextLesson.subject)
                                if (!nextLesson.room.isNullOrBlank()) append(" • s. ${nextLesson.room}")
                                append(" (${nextLesson.startsAt})")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        TodayPhase.BREAK -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zaraz: ${lesson?.subject.orEmpty()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    lesson?.let { HeroLessonDetails(it, showTeacher = false) }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${hero.minutesRemaining}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "min przerw.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
        TodayPhase.AFTER_CLASSES -> {
            Text(
                text = "Wszystkie lekcje zakończone!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.today_classes_finished_support),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TodayPhase.NEXT_CLASSES -> {
            Text(
                text = listOfNotNull(hero.scheduleDayOfWeekLabel, hero.scheduleDateLabel)
                    .joinToString(" • "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            lesson?.let { HeroLessonDetails(it, showTeacher = true) }
        }
        TodayPhase.FREE_DAY -> Unit
    }
}

@Composable
private fun HeroLessonDetails(lesson: TodayLessonUi, showTeacher: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lesson.room?.let {
            PillBadge(icon = Icons.Outlined.Room, text = "Sala $it")
        }
        if (showTeacher && !lesson.teacher.isNullOrBlank()) {
            PillBadge(icon = Icons.Outlined.PersonOutline, text = lesson.teacher)
        }
        PillBadge(icon = Icons.Outlined.Schedule, text = "${lesson.startsAt}–${lesson.endsAt}")
    }
}

@Composable
private fun PillBadge(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayScheduleSummary(hero: TodayHeroUi) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SummaryChip(
            icon = Icons.Outlined.School,
            text = pluralStringResource(
                R.plurals.today_lessons_count,
                hero.lessonCount,
                hero.lessonCount,
            ),
        )
        if (hero.dayStartsAt != null && hero.dayEndsAt != null) {
            SummaryChip(
                icon = Icons.Outlined.Schedule,
                text = "${hero.dayStartsAt} – ${hero.dayEndsAt}",
            )
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LessonStatus(status: TodayLessonStatusUi) {
    val label = when (status.kind) {
        TodayLessonStatusKind.CANCELLED -> stringResource(R.string.today_status_cancelled)
        TodayLessonStatusKind.CHANGED -> stringResource(R.string.today_status_changed)
        TodayLessonStatusKind.SHIFTED -> stringResource(R.string.today_status_shifted)
    }
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = if (status.kind == TodayLessonStatusKind.CANCELLED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        contentColor = if (status.kind == TodayLessonStatusKind.CANCELLED) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onTertiary
        },
    ) {
        Text(
            text = status.details?.let { "$label • $it" } ?: label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * BENTO ROW: Szczęśliwy Numerek + Do Sprawdzenia
 * If luckyNumber is present: shows 2 columns side by side.
 * If luckyNumber is NOT present: Do Sprawdzenia expands to full width!
 */
@Composable
private fun TodayBentoQuickGlance(
    state: TodayUiState,
    onOpenSettings: () -> Unit,
    onNavigateTarget: (NavTarget) -> Unit,
    onMarkAllRead: () -> Unit,
) {
    val hasLuckyNumber = state.luckyNumber != null && state.luckyNumber > 0

    if (hasLuckyNumber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Lucky Number Bento Tile
            TodayLuckyNumberCard(
                luckyNumber = state.luckyNumber!!,
                isUserLuckyNumber = state.isUserLuckyNumber,
                modifier = Modifier.weight(0.9f),
            )

            // Attention Center Bento Tile
            TodayAttentionCenterCard(
                attention = state.attention,
                onOpenSettings = onOpenSettings,
                onNavigateTarget = onNavigateTarget,
                onMarkAllRead = onMarkAllRead,
                modifier = Modifier.weight(1.1f),
            )
        }
    } else {
        // Only Attention Center Tile filling entire width
        TodayAttentionCenterCard(
            attention = state.attention,
            onOpenSettings = onOpenSettings,
            onNavigateTarget = onNavigateTarget,
            onMarkAllRead = onMarkAllRead,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Szczęśliwy Numerek Bento Card, elevated with a VIP Gold Card gradient when it's the user's number.
 */
@Composable
private fun TodayLuckyNumberCard(
    luckyNumber: Int,
    isUserLuckyNumber: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isUserLuckyNumber) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(BentoCardCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFECB3).copy(alpha = 0.9f),
                                Color(0xFFFFE082).copy(alpha = 0.7f),
                                Color(0xFFFFD54F).copy(alpha = 0.5f),
                            ),
                        ),
                    )
                    .padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "🍀", fontSize = 18.sp)
                        Surface(
                            shape = RoundedCornerShape(PillCornerRadius),
                            color = Color(0xFFFFB300),
                        ) {
                            Text(
                                text = "✨ VIP ✨",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Text(
                        text = "#$luckyNumber",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                        ),
                        color = Color(0xFFE65100),
                    )

                    Text(
                        text = "🎉 Twój numer! Dziś jesteś nietykalny!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBF360C),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(BentoCardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = "🍀", fontSize = 16.sp)
                    Text(
                        text = stringResource(R.string.card_type_lucky_number),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = "#$luckyNumber",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Szczęśliwy numerek na dziś",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Centrum "Do sprawdzenia" Bento Card.
 */
@Composable
private fun TodayAttentionCenterCard(
    attention: TodayAttentionUi,
    onOpenSettings: () -> Unit,
    onNavigateTarget: (NavTarget) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.today_attention),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (attention.hasItems) {
                        Surface(
                            shape = RoundedCornerShape(PillCornerRadius),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = attention.total.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                        IconButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = "Oznacz wszystkie jako przeczytane",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Ustawienia do sprawdzenia",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!attention.hasItems) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Wszystko sprawdzone ✨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (attention.enabledGrades && attention.newGrades > 0) {
                        AttentionCompactChip(
                            icon = Icons.Outlined.Grade,
                            label = stringResource(R.string.today_new_grades),
                            count = attention.newGrades,
                            badgeColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateTarget(NavTarget.GRADES) },
                        )
                    }
                    if (attention.enabledMessages && attention.unreadMessages > 0) {
                        AttentionCompactChip(
                            icon = Icons.Outlined.MailOutline,
                            label = stringResource(R.string.today_unread_messages),
                            count = attention.unreadMessages,
                            badgeColor = MaterialTheme.colorScheme.secondary,
                            onClick = { onNavigateTarget(NavTarget.MESSAGES) },
                        )
                    }
                    if (attention.enabledAnnouncements && attention.newAnnouncements > 0) {
                        AttentionCompactChip(
                            icon = Icons.Outlined.Announcement,
                            label = stringResource(R.string.menu_announcements),
                            count = attention.newAnnouncements,
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { onNavigateTarget(NavTarget.ANNOUNCEMENTS) },
                        )
                    }
                    if (attention.enabledNotices && attention.newNotices > 0) {
                        AttentionCompactChip(
                            icon = Icons.Outlined.SentimentSatisfied,
                            label = stringResource(R.string.menu_notices),
                            count = attention.newNotices,
                            badgeColor = MaterialTheme.colorScheme.error,
                            onClick = { onNavigateTarget(NavTarget.BEHAVIOUR) },
                        )
                    }
                    if (attention.enabledHomework && attention.homeworkTomorrow > 0) {
                        AttentionCompactChip(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            label = stringResource(R.string.today_homework_tomorrow),
                            count = attention.homeworkTomorrow,
                            badgeColor = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigateTarget(NavTarget.AGENDA) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttentionCompactChip(
    icon: ImageVector,
    label: String,
    count: Int,
    badgeColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = badgeColor)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(PillCornerRadius),
                color = badgeColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = "+$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Bottom sheet to configure categories included in "Do sprawdzenia".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayAttentionSettingsSheet(
    attention: TodayAttentionUi,
    onToggle: (key: String, enabled: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = BentoCardCornerRadius, topEnd = BentoCardCornerRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Dostosuj „Do sprawdzenia”",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Wybierz elementy, które mają być liczone i wyświetlane w kafelku powiadomień.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            AttentionSwitchRow(
                title = stringResource(R.string.today_new_grades),
                checked = attention.enabledGrades,
                onCheckedChange = { onToggle("grades", it) },
            )
            AttentionSwitchRow(
                title = stringResource(R.string.today_unread_messages),
                checked = attention.enabledMessages,
                onCheckedChange = { onToggle("messages", it) },
            )
            AttentionSwitchRow(
                title = stringResource(R.string.menu_announcements),
                checked = attention.enabledAnnouncements,
                onCheckedChange = { onToggle("announcements", it) },
            )
            AttentionSwitchRow(
                title = stringResource(R.string.menu_notices),
                checked = attention.enabledNotices,
                onCheckedChange = { onToggle("notices", it) },
            )
            AttentionSwitchRow(
                title = stringResource(R.string.today_homework_tomorrow),
                checked = attention.enabledHomework,
                onCheckedChange = { onToggle("homework", it) },
            )
        }
    }
}

@Composable
private fun AttentionSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * NAJBLIŻSZE WYDARZENIA & SPRAWDZIANY: Bento Inset Grouped Card.
 */
@Composable
private fun TodayUpcomingCard(
    events: List<TodayUpcomingUi>,
    onOpenAgenda: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.today_upcoming),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(PillCornerRadius),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = "${events.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                events.forEach { event ->
                    TodayUpcomingRow(event = event, onClick = onOpenAgenda)
                }
            }
        }
    }
}

@Composable
private fun TodayUpcomingRow(
    event: TodayUpcomingUi,
    onClick: () -> Unit,
) {
    val (icon, badgeColor) = when (event.kind) {
        TodayUpcomingKind.EXAM -> Pair(Icons.Outlined.School, MaterialTheme.colorScheme.error)
        TodayUpcomingKind.QUIZ -> Pair(Icons.Outlined.Schedule, MaterialTheme.colorScheme.tertiary)
        TodayUpcomingKind.PROJECT -> Pair(Icons.AutoMirrored.Outlined.Assignment, MaterialTheme.colorScheme.secondary)
        TodayUpcomingKind.DEADLINE -> Pair(Icons.Outlined.CalendarMonth, MaterialTheme.colorScheme.primary)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = badgeColor,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(PillCornerRadius),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ) {
                Text(
                    text = event.relativeDateLabel.ifBlank { event.dateLabel },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * CONNECTED TIMELINE: Kolejne lekcje połączone pionową osią czasu w stylu iOS.
 */
@Composable
private fun TodayConnectedTimeline(
    lessons: List<TodayLessonUi>,
    isNextDay: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (isNextDay) "Plan na kolejny dzień" else stringResource(R.string.today_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column {
                lessons.forEachIndexed { index, lesson ->
                    TodayTimelineItem(
                        lesson = lesson,
                        isLast = index == lessons.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTimelineItem(
    lesson: TodayLessonUi,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Timeline indicator column (Dot + connecting line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp),
            ) {}
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Lesson details card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 10.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column {
                    Text(
                        text = lesson.startsAt,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = lesson.endsAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    lesson.teacher?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    lesson.status?.let { status ->
                        LessonStatus(status)
                    }
                }

                lesson.room?.let {
                    Surface(
                        shape = RoundedCornerShape(PillCornerRadius),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    ) {
                        Text(
                            text = "s. $it",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Free day celebration card.
 */
@Composable
private fun TodayFreeDay(onOpenWeek: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Celebration,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.today_free_day),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Nic dziś w planie. Odpocznij albo sprawdź tydzień.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onOpenWeek,
                shape = RoundedCornerShape(PillCornerRadius),
            ) {
                Text("Zobacz plan")
            }
        }
    }
}

@Composable
private fun TodayLoadingShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShimmerBlock(height = 210.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) { ShimmerBlock(height = 130.dp) }
            Box(modifier = Modifier.weight(1f)) { ShimmerBlock(height = 130.dp) }
        }
        ShimmerBlock(height = 150.dp)
        ShimmerBlock(height = 100.dp)
    }
}

@Composable
private fun ShimmerBlock(height: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(BentoCardCornerRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .calmFocusShimmer(),
    )
}

@Composable
private fun TodaySyncIncomplete(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.today_sync_incomplete_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.today_sync_incomplete_support),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(PillCornerRadius),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.today_try_sync_again))
            }
        }
    }
}

@Composable
private fun TodayRecoveryCard(
    title: String,
    supportingText: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(PillCornerRadius),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.today_try_sync_again))
            }
        }
    }
}

@Composable
private fun TodayRecentGradesCard(
    grades: List<TodayRecentGradeUi>,
    onOpenGrades: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Grade,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Najnowsze oceny",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(PillCornerRadius),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onOpenGrades),
                ) {
                    Text(
                        text = "Wszystkie ›",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Grades horizontal slider
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(grades) { grade ->
                    TodayRecentGradeItem(
                        grade = grade,
                        onClick = onOpenGrades,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayRecentGradeItem(
    grade: TodayRecentGradeUi,
    onClick: () -> Unit,
) {
    val gradeColor = when (grade.gradeValue.firstOrNull()) {
        '6', '5' -> Color(0xFF2E7D32)
        '4' -> Color(0xFF00838F)
        '3' -> Color(0xFFEF6C00)
        '2', '1' -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = gradeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = grade.gradeValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                if (grade.weight > 0f) {
                    Text(
                        text = "w: ${if (grade.weight % 1.0f == 0.0f) grade.weight.toInt() else grade.weight}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = grade.subjectName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = grade.categoryName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (grade.dateString.isNotBlank()) {
                Text(
                    text = grade.dateString,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TodayTomorrowPreviewCard(
    preview: TodayTomorrowPreviewUi,
    onOpenTimetable: () -> Unit,
    onShare: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Jutro • ${preview.dayOfWeek}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(PillCornerRadius),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = "${preview.lessonCount} lekcji",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TodayLoadChip("${preview.lessonCount} lekcji")
                if (preview.examCount > 0) {
                    TodayLoadChip(
                        if (preview.examCount == 1) "1 sprawdzian" else "${preview.examCount} sprawdziany",
                    )
                }
                preview.endsAt?.let { TodayLoadChip("do $it") }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenTimetable),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Zaczynasz: ${preview.firstLessonSubject}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Godzina ${preview.firstLessonTime}${preview.firstLessonRoom?.let { " • sala $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (preview.shareText.isNotBlank()) {
                TextButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Wyślij jutro")
                }
            }
        }
    }
}

@Composable
private fun TodayLoadChip(text: String) {
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun TodayBellBar(bar: TodayBellBarUi) {
    val label = if (bar.isBreak) "Po przerwie" else "Do dzwonka"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = buildString {
                    append(label)
                    append(" • ")
                    append(bar.subject)
                    append(" za ${bar.minutesRemaining} min")
                    bar.room?.let { append(" • sala $it") }
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayFridayFinishCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Tydzień z głowy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ostatnia lekcja za Tobą. Miłego weekendu!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * iOS-styled Segmented Control with animated capsule selector.
 */
@Composable
private fun TodaySegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(PillCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tabs = listOf("Dzisiaj" to Icons.Outlined.Today, "Jutro" to Icons.Outlined.CalendarMonth)
            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTab == index
                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(200),
                    label = "TabBg",
                )
                val tabTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "TabTextColor",
                )

                Surface(
                    shape = RoundedCornerShape(PillCornerRadius),
                    color = tabBg,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PillCornerRadius))
                        .clickable { onTabSelected(index) },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = tabTextColor,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTextColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * INTERACTIVE HOMEWORK CARD: iOS Reminders style checklist with instant strikethrough & db toggle.
 */
@Composable
private fun TodayHomeworkCard(
    homeworkList: List<TodayHomeworkUi>,
    onToggleHomework: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Zadania domowe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val pendingCount = homeworkList.count { !it.isDone }
                Surface(
                    shape = RoundedCornerShape(PillCornerRadius),
                    color = if (pendingCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = if (pendingCount > 0) "$pendingCount do zrobienia" else "Wszystko zrobione! 🎉",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pendingCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                homeworkList.forEach { item ->
                    TodayHomeworkItemRow(
                        item = item,
                        onToggle = { onToggleHomework(item.id, !item.isDone) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayHomeworkItemRow(
    item: TodayHomeworkUi,
    onToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (item.isDone) 0.45f else 0.85f),
        border = BorderStroke(
            1.dp,
            if (item.isDone) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Interactive iOS checkbox circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isDone) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                    )
                    .border(
                        width = 2.dp,
                        color = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            ) {
                if (item.isDone) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.subjectName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                    )
                    if (item.dueDateLabel.isNotBlank()) {
                        Text(
                            text = "• ${item.dueDateLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                Text(
                    text = item.topic.ifBlank { "Brak opisu" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Free day card for tomorrow tab.
 */
@Composable
private fun TodayFreeTomorrowCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "🏖️", fontSize = 44.sp)
            Text(
                text = "Jutro dzień wolny od zajęć!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Brak zaplanowanych lekcji na jutrzejszy dzień. Czas na odpoczynek!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}


