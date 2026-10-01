package pl.szczodrzynski.edziennik.ui.grades.compose

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import pl.szczodrzynski.edziennik.core.telemetry.VisionTelemetry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.ui.designsystem.calmFocusShimmer

private val BentoRadius = 20.dp
private val PillRadius = 100.dp

@Composable
fun GradesRoute(
    activity: MainActivity,
    profileId: Int,
    modifier: Modifier = Modifier,
) {
    val app = activity.application as App
    val factory = remember(app, profileId) { GradesViewModel.factory(app, profileId) }
    val viewModel: GradesViewModel = viewModel(
        key = "grades-$profileId",
        factory = factory,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GradesScreen(
        state = state,
        onSelectSemester = viewModel::selectSemester,
        onToggleFilterFromLastLogin = viewModel::toggleFilterFromLastLogin,
        onAddSimulatedGrade = viewModel::addSimulatedGrade,
        onClearSimulatedGrades = viewModel::clearSimulatedGrades,
        onRefresh = activity::retryProfileSync,
        onTogglePinnedGrade = viewModel::togglePinnedGrade,
        onMarkGradeSeen = viewModel::markGradeSeen,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    state: GradesUiState,
    onSelectSemester: (GradesSemesterTab) -> Unit,
    onToggleFilterFromLastLogin: () -> Unit,
    onAddSimulatedGrade: (Long, Float, Float) -> Unit,
    onClearSimulatedGrades: (Long) -> Unit,
    onRefresh: () -> Unit,
    onTogglePinnedGrade: (Long) -> Unit = {},
    onMarkGradeSeen: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var selectedSubjectForDetails by remember { mutableStateOf<GradeSubjectUi?>(null) }
    var selectedGradeForDetails by remember { mutableStateOf<Pair<GradeItemUi, String>?>(null) }
    var isCompactGrid by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Oceny",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (state.newSinceLastLoginCount > 0) {
                    Text(
                        text = "${state.newSinceLastLoginCount} nowych od ostatniego logowania",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isCompactGrid = !isCompactGrid
                        VisionTelemetry.recordEvent(
                            "grades_semantic_zoom",
                            mapOf("mode" to if (isCompactGrid) "compact" else "comfortable", "source" to "button"),
                        )
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isCompactGrid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                        contentDescription = if (isCompactGrid) "Widok szczegółowy (Bento)" else "Widok minimalistyczny (Siatka)",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Odśwież oceny", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Semester Pills Tab Row
        GradesSemesterTabs(
            selectedTab = state.selectedSemester,
            onSelectTab = { tab ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectSemester(tab)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // "Od ostatniego logowania" Filter Chip
        if (state.newSinceLastLoginCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                FilterChip(
                    selected = state.filterFromLastLogin,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFilterFromLastLogin()
                    },
                    label = {
                        Text("✨ Od ostatniego logowania (${state.newSinceLastLoginCount})")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = RoundedCornerShape(PillRadius),
                )
            }
        }

        if (state.isLoading) {
            GradesLoadingShimmer()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isCompactGrid) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var zoom = 1f
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.isConsumed }) break
                                if (event.changes.size >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    zoom *= zoomChange
                                    if (zoom < 0.82f && !isCompactGrid) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isCompactGrid = true
                                        VisionTelemetry.recordEvent(
                                            "grades_semantic_zoom",
                                            mapOf("mode" to "compact", "source" to "pinch"),
                                        )
                                        event.changes.forEach { it.consume() }
                                        break
                                    } else if (zoom > 1.22f && isCompactGrid) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isCompactGrid = false
                                        VisionTelemetry.recordEvent(
                                            "grades_semantic_zoom",
                                            mapOf("mode" to "comfortable", "source" to "spread"),
                                        )
                                        event.changes.forEach { it.consume() }
                                        break
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Hero GPA & Red Stripe Card (only when not filtering)
                if (!state.filterFromLastLogin) {
                    item(key = "hero-gpa") {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        GradesHeroCard(
                            overallAverage = state.overallAverage,
                            redStripeProgress = state.redStripeProgress,
                            redStripeDiff = state.redStripeDiff,
                            gradeDistribution = state.gradeDistribution,
                            historyPoints = state.averageHistoryPoints,
                            onExportCsv = {
                                pl.szczodrzynski.edziennik.ui.grades.GradesExport.exportCsv(context, state)
                            },
                        )
                    }
                }

                // Section Header
                item(key = "subjects-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (state.filterFromLastLogin) "Nowe oceny (${state.subjects.sumOf { it.grades.size }})"
                            else "Przedmioty (${state.subjects.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isCompactGrid) "Siatka minimalistyczna" else "Kliknij, aby otworzyć",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Subjects List
                if (!isCompactGrid) {
                    // Full Bento Cards
                    items(state.subjects, key = { it.subjectId }) { subject ->
                        val hasSimulation = state.simulatedGrades[subject.subjectId]?.isNotEmpty() == true
                        SubjectBentoCard(
                            subject = subject,
                            hasSimulation = hasSimulation,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedSubjectForDetails = subject
                            },
                            onGradeClick = { grade ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGradeForDetails = grade to subject.subjectName
                            },
                            onTogglePinnedGrade = onTogglePinnedGrade,
                            onMarkGradeSeen = onMarkGradeSeen,
                        )
                    }
                } else {
                    // 2-Column Minimalist Grid (pairs in LazyColumn)
                    val subjectPairs = state.subjects.chunked(2)
                    items(subjectPairs, key = { pair -> "compact-${pair.first().subjectId}" }) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { subject ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val hasSimulation = state.simulatedGrades[subject.subjectId]?.isNotEmpty() == true
                                    SubjectCompactCard(
                                        subject = subject,
                                        hasSimulation = hasSimulation,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedSubjectForDetails = subject
                                        },
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Modal Bottom Sheet for Subject Details & What-If Simulator (SIMULATOR AT THE BOTTOM)
    selectedSubjectForDetails?.let { subject ->
        val currentSubject = state.subjects.firstOrNull { it.subjectId == subject.subjectId } ?: subject
        val simulatedList = state.simulatedGrades[currentSubject.subjectId] ?: emptyList()

        SubjectDetailsModalSheet(
            subject = currentSubject,
            simulatedGrades = simulatedList,
            onAddSimulatedGrade = { value, weight ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAddSimulatedGrade(currentSubject.subjectId, value, weight)
            },
            onClearSimulated = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClearSimulatedGrades(currentSubject.subjectId)
            },
            onGradeClick = { grade ->
                selectedGradeForDetails = grade to currentSubject.subjectName
            },
            onDismiss = { selectedSubjectForDetails = null },
        )
    }

    // Modal Bottom Sheet for Single Grade Full Details
    selectedGradeForDetails?.let { (grade, subjectName) ->
        GradeDetailsModalSheet(
            grade = grade,
            subjectName = subjectName,
            onTogglePinned = { onTogglePinnedGrade(grade.id) },
            onMarkSeen = { onMarkGradeSeen(grade.id) },
            onDismiss = { selectedGradeForDetails = null },
        )
    }
}

/**
 * iOS-style Segmented Tabs for Semesters.
 */
@Composable
private fun GradesSemesterTabs(
    selectedTab: GradesSemesterTab,
    onSelectTab: (GradesSemesterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PillRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GradesSemesterTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabBg",
                )
                val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(PillRadius))
                        .clickable { onSelectTab(tab) },
                    shape = RoundedCornerShape(PillRadius),
                    color = bgColor,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Hero Bento Card: Overall GPA, Red Ribbon distance, and Grade Distribution Bar Chart.
 */
@Composable
private fun GradesHeroCard(
    overallAverage: Float?,
    redStripeProgress: Float,
    redStripeDiff: Float?,
    gradeDistribution: Map<Int, Int>,
    historyPoints: List<Float> = emptyList(),
    onExportCsv: () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = redStripeProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "redStripeProgress",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BentoRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Row: Big GPA Number + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Średnia ogólna",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    Text(
                        text = overallAverage?.let { "%.2f".format(it) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // Red Stripe Status Pill
                Surface(
                    shape = RoundedCornerShape(PillRadius),
                    color = if (redStripeProgress >= 1f) Color(0xFFC62828) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (redStripeProgress >= 1f) Color(0xFFE53935) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (redStripeProgress >= 1f) "🎓 Pasek!" else "🎯 Cel: 4.75",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (redStripeProgress >= 1f) Color.White else MaterialTheme.colorScheme.primary,
                        )
                        redStripeDiff?.let { diff ->
                            Text(
                                text = if (diff >= 0f) "+%.2f".format(diff) else "%.2f".format(diff),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (redStripeProgress >= 1f) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // Progress bar towards red stripe (4.75)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(PillRadius)),
                    color = Color(0xFFC62828),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                )
                Text(
                    text = if (redStripeProgress >= 1f) "Średnia kwalifikuje się do świadectwa z paskiem! 🌟"
                    else "Do świadectwa z wyróżnieniem brakuje jeszcze odrobiny wysiłku.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp,
            )

            // Minimalist Grade Distribution Chart (6, 5, 4, 3, 2, 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                val maxCount = (gradeDistribution.values.maxOrNull() ?: 1).coerceAtLeast(1)

                (6 downTo 1).forEach { gradeNum ->
                    val count = gradeDistribution[gradeNum] ?: 0
                    val heightRatio = (count.toFloat() / maxCount).coerceIn(0.15f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (count > 0) 1f else 0.4f),
                        )

                        // Vertical bar
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((34 * heightRatio).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (gradeNum) {
                                        6, 5 -> Color(0xFF2E7D32).copy(alpha = 0.75f)
                                        4 -> Color(0xFF00838F).copy(alpha = 0.75f)
                                        3 -> Color(0xFFEF6C00).copy(alpha = 0.75f)
                                        else -> Color(0xFFC62828).copy(alpha = 0.75f)
                                    },
                                ),
                        )

                        Text(
                            text = gradeNum.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Timeline Chart of Average Progression
            if (historyPoints.size >= 2) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trend średniej w czasie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = String.format(java.util.Locale.getDefault(), "od %.2f do %.2f", historyPoints.first(), historyPoints.last()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val minVal = (historyPoints.minOrNull() ?: 1f) - 0.2f
                        val maxVal = (historyPoints.maxOrNull() ?: 6f) + 0.2f
                        val range = (maxVal - minVal).coerceAtLeast(0.5f)
                        val stepX = this.size.width / (historyPoints.size - 1).coerceAtLeast(1)
                        val path = androidx.compose.ui.graphics.Path()

                        historyPoints.forEachIndexed { idx, point ->
                            val x = idx * stepX
                            val normY = (point - minVal) / range
                            val y = this.size.height - (normY * this.size.height)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF2F6FED),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // Export CSV action
            androidx.compose.material3.OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text("📊 Eksportuj oceny do arkusza (.csv)", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Subject Bento Card with average pill, proposed/final badges, and grade pills.
 */
@Composable
private fun SubjectBentoCard(
    subject: GradeSubjectUi,
    hasSimulation: Boolean,
    onClick: () -> Unit,
    onGradeClick: (GradeItemUi) -> Unit,
    onTogglePinnedGrade: (Long) -> Unit,
    onMarkGradeSeen: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BentoRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(BentoRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (subject.average != null && subject.average < 2.0f) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        border = BorderStroke(
            1.dp,
            if (hasSimulation) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else if (subject.average != null && subject.average < 2.0f) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: Subject Name + Average Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (subject.average != null && subject.average < 2.0f) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ),
                    )
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (subject.average != null && subject.average < 2.0f) {
                        Surface(
                            shape = RoundedCornerShape(PillRadius),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "⚠️ Zagrożenie",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (hasSimulation) {
                        Surface(
                            shape = RoundedCornerShape(PillRadius),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "✨ Co jeśli",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    // Average Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = subject.average?.let { avg ->
                            when {
                                avg >= 4.75f -> Color(0xFF2E7D32).copy(alpha = 0.18f)
                                avg >= 3.75f -> Color(0xFF00838F).copy(alpha = 0.18f)
                                avg >= 2.50f -> Color(0xFFEF6C00).copy(alpha = 0.18f)
                                else -> Color(0xFFC62828).copy(alpha = 0.18f)
                            }
                        } ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            text = subject.average?.let { "%.2f".format(it) } ?: "—",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = subject.average?.let { avg ->
                                when {
                                    avg >= 4.75f -> Color(0xFF2E7D32)
                                    avg >= 3.75f -> Color(0xFF00838F)
                                    avg >= 2.50f -> Color(0xFFEF6C00)
                                    else -> Color(0xFFC62828)
                                }
                            } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Proposed & Final Badges (if any)
            if (subject.proposedGrade != null || subject.finalGrade != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subject.proposedGrade?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        ) {
                            Text(
                                text = "Proponowana: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    subject.finalGrade?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "Końcowa: $it",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            // Grades Row (Pills)
            if (subject.grades.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(subject.grades) { grade ->
                        GradePill(
                            grade = grade,
                            subjectName = subject.subjectName,
                            onClick = { onGradeClick(grade) },
                            onTogglePinned = { onTogglePinnedGrade(grade.id) },
                            onMarkSeen = { onMarkGradeSeen(grade.id) },
                        )
                    }
                }
            } else {
                Text(
                    text = "Brak wpisanych ocen w tym okresie",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/**
 * Minimalist compact subject card for dense 2-column grid.
 */
@Composable
private fun SubjectCompactCard(
    subject: GradeSubjectUi,
    hasSimulation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isThreatened = subject.average != null && subject.average < 2.0f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isThreatened) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        border = BorderStroke(
            1.dp,
            if (hasSimulation) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else if (isThreatened) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top row: Subject name & Threat/Sim icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = subject.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isThreatened) {
                    Text(text = "⚠️", fontSize = 12.sp)
                } else if (hasSimulation) {
                    Text(text = "✨", fontSize = 12.sp)
                }
            }

            // Middle row: Prominent Average Pill + proposed/final if any
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val avgColor = subject.average?.let { avg ->
                    when {
                        avg >= 4.75f -> Color(0xFF2E7D32)
                        avg >= 3.75f -> Color(0xFF00838F)
                        avg >= 2.50f -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }
                } ?: MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = avgColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, avgColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = subject.average?.let { "%.2f".format(it) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = avgColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                val finalOrProposed = subject.finalGrade ?: subject.proposedGrade
                if (finalOrProposed != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    ) {
                        Text(
                            text = finalOrProposed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            // Bottom: Heat-map Micro-Dots representing latest grades
            if (subject.grades.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val recentGrades = subject.grades.takeLast(7)
                    recentGrades.forEach { grade ->
                        val dotColor = when (grade.name.firstOrNull()) {
                            '6', '5' -> Color(0xFF2E7D32)
                            '4' -> Color(0xFF00838F)
                            '3' -> Color(0xFFEF6C00)
                            '2', '1' -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    }
                    if (subject.grades.size > 7) {
                        Text(
                            text = "+${subject.grades.size - 7}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                Text(
                    text = "Brak ocen",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/**
 * Individual Grade Pill in card row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GradePill(
    grade: GradeItemUi,
    subjectName: String,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onMarkSeen: () -> Unit,
) {
    val gradeColor = when (grade.name.firstOrNull()) {
        '6', '5' -> Color(0xFF2E7D32)
        '4' -> Color(0xFF00838F)
        '3' -> Color(0xFFEF6C00)
        '2', '1' -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = gradeColor.copy(alpha = 0.14f),
            border = BorderStroke(
                if (grade.isFromLastLogin) 1.5.dp else 1.dp,
                if (grade.isFromLastLogin) MaterialTheme.colorScheme.primary else gradeColor.copy(alpha = 0.3f),
            ),
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = true
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (grade.isPinned) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(10.dp), tint = gradeColor)
                    }
                    if (grade.isFromLastLogin) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(end = 2.dp),
                        )
                    }
                    Text(
                        text = grade.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                    )
                }
                if (grade.weight > 0f) {
                    Text(
                        text = "${if (grade.weight % 1f == 0f) grade.weight.toInt() else grade.weight}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = gradeColor.copy(alpha = 0.75f),
                    )
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("⚡ Co jeśli poprawię na 5?") },
                onClick = {
                    menu = false
                    onClick()
                },
            )
            DropdownMenuItem(
                text = { Text(if (grade.isPinned) "Odepnij" else "Przypnij") },
                onClick = { menu = false; onTogglePinned() },
            )
            DropdownMenuItem(
                text = { Text("Szczegóły") },
                onClick = { menu = false; onClick() },
            )
            DropdownMenuItem(
                text = { Text("Udostępnij") },
                onClick = {
                    menu = false
                    val text = "$subjectName: ${grade.name}" +
                        grade.category.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            },
                            "Udostępnij",
                        ),
                    )
                },
            )
            DropdownMenuItem(
                text = { Text("Oznacz jako przeczytane") },
                onClick = { menu = false; onMarkSeen() },
            )
        }
    }
}

/**
 * Modal Bottom Sheet for Subject Details & What-If Grade Simulator.
 * Note: Simulator is now at the BOTTOM of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDetailsModalSheet(
    subject: GradeSubjectUi,
    simulatedGrades: List<SimulatedGrade>,
    onAddSimulatedGrade: (Float, Float) -> Unit,
    onClearSimulated: () -> Unit,
    onGradeClick: (GradeItemUi) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSimGradeValue by remember { mutableFloatStateOf(5f) }
    var selectedSimWeight by remember { mutableFloatStateOf(1f) }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Subject name + Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Aktualna średnia: ${subject.average?.let { "%.2f".format(it) } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Zamknij")
                }
            }

            // LIST OF GRADES IN THIS SUBJECT (at top)
            Text(
                text = "Wszystkie oceny (${subject.grades.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (subject.grades.isEmpty()) {
                Text(
                    text = "Brak wpisanych ocen w tym okresie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(subject.grades) { grade ->
                        DetailedGradeListItem(
                            grade = grade,
                            onClick = { onGradeClick(grade) },
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp,
            )

            // WHAT-IF SIMULATOR CARD (PLACED AT THE BOTTOM)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Symulator ocen (Co jeśli...)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (simulatedGrades.isNotEmpty()) {
                            TextButton(onClick = onClearSimulated, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Wyczyść (${simulatedGrades.size})", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Pick Grade value: 6, 5, 4, 3, 2, 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf(6f, 5f, 4f, 3f, 2f, 1f).forEach { gVal ->
                            val isSelected = selectedSimGradeValue == gVal
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { selectedSimGradeValue = gVal },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = gVal.toInt().toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Pick Weight: 1, 2, 3, 4, 5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Waga:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        listOf(1f, 2f, 3f, 4f, 5f).forEach { wVal ->
                            val isSelected = selectedSimWeight == wVal
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedSimWeight = wVal },
                            ) {
                                Text(
                                    text = "w: ${wVal.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onAddSimulatedGrade(selectedSimGradeValue, selectedSimWeight) },
                        shape = RoundedCornerShape(PillRadius),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zasymuluj ocenę ${selectedSimGradeValue.toInt()} (waga ${selectedSimWeight.toInt()})")
                    }
                }
            }
        }
    }
}

/**
 * Detailed Grade List Item in Subject Sheet.
 */
@Composable
private fun DetailedGradeListItem(
    grade: GradeItemUi,
    onClick: () -> Unit = {},
) {
    val gradeColor = when (grade.name.firstOrNull()) {
        '6', '5' -> Color(0xFF2E7D32)
        '4' -> Color(0xFF00838F)
        '3' -> Color(0xFFEF6C00)
        '2', '1' -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Big Grade Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = gradeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f)),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = grade.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                    )
                }
            }

            // Grade Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = grade.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (grade.isFromLastLogin) {
                        Surface(
                            shape = RoundedCornerShape(PillRadius),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "NOWA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                if (!grade.comment.isNullOrBlank()) {
                    Text(
                        text = grade.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!grade.description.isNullOrBlank()) {
                    Text(
                        text = grade.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (grade.weight > 0f) {
                        Text(
                            text = "Waga: ${if (grade.weight % 1f == 0f) grade.weight.toInt() else grade.weight}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (grade.dateString.isNotBlank()) {
                        Text(
                            text = "• ${grade.dateString}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    if (!grade.teacher.isNullOrBlank()) {
                        Text(
                            text = "• ${grade.teacher}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full details bottom sheet for a single grade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDetailsModalSheet(
    grade: GradeItemUi,
    subjectName: String,
    onTogglePinned: () -> Unit,
    onMarkSeen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val gradeColor = when (grade.name.firstOrNull()) {
        '6', '5' -> Color(0xFF2E7D32)
        '4' -> Color(0xFF00838F)
        '3' -> Color(0xFFEF6C00)
        '2', '1' -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
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
            // Header: Grade badge + Subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = gradeColor.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, gradeColor),
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = grade.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = gradeColor,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Semestr ${grade.semester}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (grade.isFromLastLogin) {
                        Surface(
                            shape = RoundedCornerShape(PillRadius),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = "✨ Nowa od ostatniego logowania",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Zamknij")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Details Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GradeDetailRow(label = "Kategoria", value = grade.category)
                if (grade.weight > 0f) {
                    GradeDetailRow(label = "Waga", value = "${if (grade.weight % 1f == 0f) grade.weight.toInt() else grade.weight}")
                }
                if (grade.value > 0f) {
                    GradeDetailRow(label = "Wartość do średniej", value = "%.2f".format(grade.value))
                }
                if (grade.dateString.isNotBlank()) {
                    GradeDetailRow(label = "Data wpisania", value = grade.dateString)
                }
                if (!grade.teacher.isNullOrBlank()) {
                    GradeDetailRow(label = "Nauczyciel", value = grade.teacher)
                }
                if (!grade.comment.isNullOrBlank()) {
                    GradeDetailRow(label = "Komentarz", value = grade.comment)
                }
                if (!grade.description.isNullOrBlank()) {
                    GradeDetailRow(label = "Opis", value = grade.description)
                }
                grade.classAverage?.let {
                    GradeDetailRow(label = "Średnia klasy", value = "%.2f".format(it))
                }
                GradeDetailRow(label = "Liczy się do średniej", value = if (grade.isCounted) "Tak" else "Nie")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val text = "$subjectName: ${grade.name} (${grade.category})" +
                            grade.comment.takeIf { !it.isNullOrBlank() }?.let { " - $it" }.orEmpty()
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                },
                                "Udostępnij ocenę",
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(PillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Udostępnij", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        onTogglePinned()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(PillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (grade.isPinned) "Odepnij" else "Przypnij")
                }
            }
        }
    }
}

@Composable
private fun GradeDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f),
        )
    }
}

@Composable
private fun GradesLoadingShimmer() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(BentoRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .calmFocusShimmer(),
        )
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                .clip(RoundedCornerShape(BentoRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .calmFocusShimmer(),
            )
        }
    }
}
