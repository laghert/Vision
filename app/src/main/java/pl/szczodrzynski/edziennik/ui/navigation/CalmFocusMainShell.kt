package pl.szczodrzynski.edziennik.ui.navigation

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

private data class PrimaryDestination(
    val labelRes: Int,
    val target: NavTarget? = null,
    val icon: ImageVector,
)

private val primaryDestinations = listOf(
    PrimaryDestination(R.string.nav_today, NavTarget.HOME, Icons.Outlined.Today),
    PrimaryDestination(R.string.nav_plan, NavTarget.TIMETABLE, Icons.Outlined.CalendarMonth),
    PrimaryDestination(R.string.menu_grades, NavTarget.GRADES, Icons.Outlined.Grade),
    PrimaryDestination(R.string.menu_messages, NavTarget.MESSAGES, Icons.Outlined.MailOutline),
    PrimaryDestination(R.string.menu_more, icon = Icons.Outlined.MoreHoriz),
)

private val moreExpandedDestinations = listOf(
    PrimaryDestination(R.string.menu_notices, NavTarget.BEHAVIOUR, Icons.Outlined.SentimentSatisfied),
    PrimaryDestination(R.string.menu_agenda, NavTarget.AGENDA, Icons.Outlined.ViewAgenda),
    PrimaryDestination(R.string.menu_attendance, NavTarget.ATTENDANCE, Icons.Outlined.FactCheck),
    PrimaryDestination(R.string.menu_settings, NavTarget.SETTINGS, Icons.Outlined.Settings),
    PrimaryDestination(R.string.menu_all, null, Icons.Outlined.GridView),
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
@Composable
fun CalmFocusMainShell(
    activity: MainActivity,
    state: MainShellState,
    initialRoute: CalmFocusRoute.Screen,
    initialArguments: Bundle?,
) {
    val navController = rememberNavController()
    val expanded = calculateWindowSizeClass(activity).widthSizeClass == WindowWidthSizeClass.Expanded
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTarget = backStackEntry?.arguments
        ?.getInt(CalmFocusRoute.TARGET_ID_ARGUMENT)
        ?.let(NavTarget::getByIdOrNull)
        ?: state.target
    val moreSelected = backStackEntry?.destination?.route == CalmFocusRoute.More.value ||
        currentTarget !in primaryDestinations.mapNotNull { it.target }
    val primaryTargets = primaryDestinations.mapNotNull { it.target }
    val unreadCount: (NavTarget?) -> Int = { target ->
        val badgeTypes = if (target != null) {
            setOfNotNull(target.badgeType)
        } else {
            NavTarget.entries
                .filterNot { it in primaryTargets }
                .mapNotNullTo(mutableSetOf()) { it.badgeType }
        }
        state.unreadCounters
            .filter { it.profileId == state.profile.id && it.thingType in badgeTypes }
            .sumOf { it.count }
    }
    val canNavigateUp = currentTarget !in primaryTargets &&
        (navController.previousBackStackEntry != null || currentTarget.popTo != null)

    var isMoreExpanded by rememberSaveable { mutableStateOf(false) }
    var showAllTargetsSheet by rememberSaveable { mutableStateOf(false) }

    val moreExpandedTargets = remember { moreExpandedDestinations.mapNotNull { it.target } }
    LaunchedEffect(currentTarget) {
        if (currentTarget in moreExpandedTargets) {
            isMoreExpanded = true
        } else if (currentTarget in primaryTargets) {
            isMoreExpanded = false
        }
    }

    DisposableEffect(navController) {
        activity.bindNavController(navController)
        onDispose { activity.unbindNavController(navController) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            MainTopBar(
                state = state,
                currentTarget = currentTarget,
                moreSelected = backStackEntry?.destination?.route == CalmFocusRoute.More.value,
                canNavigateUp = canNavigateUp,
                onNavigateUp = activity::navigateUp,
                onProfileSelected = activity::selectProfile,
                onProfileAction = activity::handleProfileAction,
            )
        },
        bottomBar = {
            Column {
                val nowLesson by NowLessonStore.current.collectAsStateWithLifecycle()
                nowLesson?.let { lesson ->
                    NowPlayingLessonBar(
                        lesson = lesson,
                        onClick = { activity.selectShellTarget(NavTarget.TIMETABLE) },
                    )
                }
                if (!expanded) {
                    MainNavigationBar(
                        currentTarget = currentTarget,
                        moreSelected = moreSelected,
                        isExpandedMore = isMoreExpanded,
                        onToggleExpandedMore = { isMoreExpanded = it },
                        unreadCount = unreadCount,
                        onTargetSelected = activity::selectShellTarget,
                        onAllSelected = { showAllTargetsSheet = true },
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.fab.enabled) {
                ExtendedFloatingActionButton(
                    onClick = { state.fab.performClick(View(activity)) },
                    expanded = state.fab.extended,
                    icon = { LegacyFabIcon(state.fab) },
                    text = { Text(state.fab.text.toString()) },
                )
            }
        },
    ) { contentPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (expanded) {
                MainNavigationRail(
                    currentTarget = currentTarget,
                    moreSelected = moreSelected,
                    unreadCount = unreadCount,
                    onTargetSelected = activity::selectShellTarget,
                    onMoreSelected = { showAllTargetsSheet = true },
                )
            }
            NavHost(
                navController = navController,
                startDestination = initialRoute.value,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (expanded) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                composable(
                    route = CalmFocusRoute.SCREEN_PATTERN,
                    arguments = listOf(
                        navArgument(CalmFocusRoute.TARGET_ID_ARGUMENT) { type = NavType.IntType },
                        navArgument(CalmFocusRoute.ENTRY_ID_ARGUMENT) { type = NavType.LongType },
                    ),
                ) { entry ->
                    val target = NavTarget.getById(
                        entry.arguments?.getInt(CalmFocusRoute.TARGET_ID_ARGUMENT)
                            ?: NavTarget.HOME.id,
                    )
                    if (target == NavTarget.HOME) {
                        pl.szczodrzynski.edziennik.ui.today.TodayRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else if (target == NavTarget.TIMETABLE) {
                        pl.szczodrzynski.edziennik.ui.timetable.TimetableRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else if (target == NavTarget.GRADES) {
                        pl.szczodrzynski.edziennik.ui.grades.compose.GradesRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else if (target == NavTarget.MESSAGES) {
                        pl.szczodrzynski.edziennik.ui.messages.compose_screen.MessagesRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else if (target == NavTarget.AGENDA) {
                        pl.szczodrzynski.edziennik.ui.agenda.compose.AgendaRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else if (target == NavTarget.ATTENDANCE) {
                        pl.szczodrzynski.edziennik.ui.attendance.compose.AttendanceRoute(
                            activity = activity,
                            profileId = state.profile.id,
                        )
                    } else {
                        val entryId = entry.arguments?.getLong(CalmFocusRoute.ENTRY_ID_ARGUMENT) ?: 0L
                        val fragmentArguments = remember(entry) {
                            entry.savedStateHandle.get<Bundle>(CalmFocusRoute.SCREEN_ARGUMENTS_KEY)
                                ?: initialArguments.takeIf { entryId == initialRoute.entryId }?.also {
                                    entry.savedStateHandle[CalmFocusRoute.SCREEN_ARGUMENTS_KEY] = it
                                }
                        }
                        val containerId = remember(entry) {
                            entry.savedStateHandle.get<Int>(CalmFocusRoute.FRAGMENT_CONTAINER_ID_KEY)
                                ?: View.generateViewId().also {
                                    entry.savedStateHandle[CalmFocusRoute.FRAGMENT_CONTAINER_ID_KEY] = it
                                }
                        }
                        LegacyFragmentHost(
                            activity = activity,
                            target = target,
                            entryId = entryId,
                            arguments = fragmentArguments,
                            containerId = containerId,
                            state = state,
                        )
                    }
                }
                composable(CalmFocusRoute.More.value) {
                    MoreScreen(
                        targets = activity.availableMoreTargets(),
                        state = state,
                        onTargetSelected = activity::selectMoreTarget,
                    )
                }
            }
        }
    }

    if (showAllTargetsSheet) {
        AllTargetsBottomSheet(
            targets = activity.availableMoreTargets(),
            state = state,
            onTargetSelected = { target ->
                showAllTargetsSheet = false
                isMoreExpanded = false
                activity.selectMoreTarget(target)
            },
            onDismiss = { showAllTargetsSheet = false },
        )
    }

    if (state.contextActions.isOpen) {
        ContextActionsSheet(state.contextActions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    state: MainShellState,
    currentTarget: NavTarget,
    moreSelected: Boolean,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onProfileSelected: (Profile) -> Unit,
    onProfileAction: (NavTarget) -> Unit,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ),
        navigationIcon = {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (moreSelected) R.string.menu_more else currentTarget.titleRes ?: currentTarget.nameRes))
                Text(
                    text = state.subtitle ?: state.profile.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            if (state.contextActions.items.isNotEmpty()) {
                IconButton(onClick = state.contextActions::open) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.menu_more))
                }
            }
            Box {
                IconButton(onClick = { profileMenuExpanded = true }) {
                    ProfileAvatar(state.profile)
                }
                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false },
                ) {
                    state.profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(profile.name)
                                    profile.subname?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                            leadingIcon = { ProfileAvatar(profile) },
                            onClick = {
                                profileMenuExpanded = false
                                onProfileSelected(profile)
                            },
                        )
                    }
                    HorizontalDivider()
                    ProfileActionItem(NavTarget.PROFILE_ADD, Icons.Outlined.Add, onProfileAction) {
                        profileMenuExpanded = false
                    }
                    ProfileActionItem(NavTarget.PROFILE_MANAGER, Icons.Outlined.ManageAccounts, onProfileAction) {
                        profileMenuExpanded = false
                    }
                    ProfileActionItem(NavTarget.PROFILE_SYNC_ALL, Icons.Outlined.Sync, onProfileAction) {
                        profileMenuExpanded = false
                    }
                    ProfileActionItem(NavTarget.PROFILE_MARK_AS_READ, Icons.Outlined.DoneAll, onProfileAction) {
                        profileMenuExpanded = false
                    }
                }
            }
        },
    )
}

@Composable
private fun ProfileActionItem(
    target: NavTarget,
    icon: ImageVector,
    onAction: (NavTarget) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(target.nameRes)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = {
            closeMenu()
            onAction(target)
        },
    )
}

@Composable
private fun ProfileAvatar(profile: Profile) {
    AndroidView(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
        factory = { context ->
            AppCompatImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = profile::applyImageTo,
    )
}

@Composable
private fun MainNavigationBar(
    currentTarget: NavTarget,
    moreSelected: Boolean,
    isExpandedMore: Boolean,
    onToggleExpandedMore: (Boolean) -> Unit,
    unreadCount: (NavTarget?) -> Int,
    onTargetSelected: (NavTarget) -> Unit,
    onAllSelected: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        AnimatedContent(
            targetState = isExpandedMore,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width / 2 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width / 2 } + fadeOut())
                }
            },
            label = "DockExpansionAnim",
        ) { expanded ->
            if (!expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    primaryDestinations.forEach { destination ->
                        val selected = destination.target?.let { it == currentTarget } ?: moreSelected
                        val count = unreadCount(destination.target)

                        DockItem(
                            destination = destination,
                            selected = selected,
                            count = count,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (destination.target != null) {
                                    onTargetSelected(destination.target)
                                } else {
                                    onToggleExpandedMore(true)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleExpandedMore(false)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Zwiń",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    moreExpandedDestinations.forEach { destination ->
                        val selected = destination.target?.let { it == currentTarget } ?: false
                        val count = unreadCount(destination.target)

                        DockItem(
                            destination = destination,
                            selected = selected,
                            count = count,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (destination.target != null) {
                                    onTargetSelected(destination.target)
                                } else {
                                    onAllSelected()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    destination: PrimaryDestination,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dockScale",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "dockContentColor",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(indicatorColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            )
                        }
                    }
                },
            ) {
                AnimatedDestinationIcon(
                    icon = destination.icon,
                    selected = selected,
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun MainNavigationRail(
    currentTarget: NavTarget,
    moreSelected: Boolean,
    unreadCount: (NavTarget?) -> Int,
    onTargetSelected: (NavTarget) -> Unit,
    onMoreSelected: () -> Unit,
) {
    NavigationRail {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            primaryDestinations.forEach { destination ->
                val selected = destination.target?.let { it == currentTarget } ?: moreSelected
                NavigationRailItem(
                    selected = selected,
                    onClick = { destination.target?.let(onTargetSelected) ?: onMoreSelected() },
                    icon = {
                    DestinationIconWithBadge(
                        icon = destination.icon,
                        selected = selected,
                        count = unreadCount(destination.target),
                    )
                },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun DestinationIconWithBadge(
    icon: ImageVector,
    selected: Boolean,
    count: Int,
) {
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge { Text(if (count > 99) "99+" else count.toString()) }
            }
        },
    ) {
        AnimatedDestinationIcon(icon, selected)
    }
}

@Composable
private fun AnimatedDestinationIcon(icon: ImageVector, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "navigationIconScale",
    )
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

@Composable
private fun MoreScreen(
    targets: List<NavTarget>,
    state: MainShellState,
    onTargetSelected: (NavTarget) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(targets, key = NavTarget::id) { target ->
            val count = target.badgeType?.let { badgeType ->
                state.unreadCounters
                    .filter { it.profileId == state.profile.id && it.thingType == badgeType }
                    .sumOf { it.count }
            } ?: 0

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTargetSelected(target)
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            target.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(target.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        target.descriptionRes?.let { descRes ->
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (count > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(if (count > 99) "99+" else count.toString())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllTargetsBottomSheet(
    targets: List<NavTarget>,
    state: MainShellState,
    onTargetSelected: (NavTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Wszystkie moduły",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Wybierz sekcję, do której chcesz przejść",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Zamknij")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(targets, key = NavTarget::id) { target ->
                    val count = target.badgeType?.let { badgeType ->
                        state.unreadCounters
                            .filter { it.profileId == state.profile.id && it.thingType == badgeType }
                            .sumOf { it.count }
                    } ?: 0

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTargetSelected(target)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    target.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(target.nameRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                target.descriptionRes?.let { descRes ->
                                    Text(
                                        text = stringResource(descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            if (count > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ) {
                                    Text(if (count > 99) "99+" else count.toString())
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
private fun LegacyFragmentHost(
    activity: MainActivity,
    target: NavTarget,
    entryId: Long,
    arguments: Bundle?,
    containerId: Int,
    state: MainShellState,
) {
    val fragmentTag = remember(entryId) { "calm-focus-$entryId-${target.id}" }
    var refreshView by remember { mutableStateOf<SwipeRefreshLayout?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            CoordinatorLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                val swipeRefresh = SwipeRefreshLayout(context).apply {
                    layoutParams = CoordinatorLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setColorSchemeResources(
                        R.color.md_blue_500,
                        R.color.md_amber_500,
                        R.color.md_green_500,
                    )
                }
                val fragmentContainer = FragmentContainerView(context).apply {
                    id = containerId
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    doOnAttach {
                        activity.mountFragment(target, arguments, containerId, fragmentTag)
                    }
                }
                swipeRefresh.addView(fragmentContainer)
                addView(swipeRefresh)
                refreshView = swipeRefresh
                state.refresh.bind(swipeRefresh, activity::refreshCurrentFeature)
                activity.bindSnackbarHost(this)
            }
        },
    )

    DisposableEffect(fragmentTag) {
        onDispose {
            refreshView?.let(state.refresh::unbind)
            activity.unmountFragment(fragmentTag)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextActionsSheet(controller: ContextActionsController) {
    ModalBottomSheet(onDismissRequest = controller::close) {
        LazyColumn {
            items(controller.items.toList()) { item ->
                when (item) {
                    is BottomSheetSeparatorItem -> HorizontalDivider()
                    is BottomSheetPrimaryItem -> LegacyContextAction(item, controller)
                }
            }
        }
    }
}

@Composable
private fun LegacyContextAction(
    item: BottomSheetPrimaryItem,
    controller: ContextActionsController,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = {
            Text(item.titleRes?.let { stringResource(it) } ?: item.title?.toString().orEmpty())
        },
        supportingContent = when {
            item.descriptionRes != null -> ({ Text(stringResource(item.descriptionRes!!)) })
            item.description != null -> ({ Text(item.description.toString()) })
            else -> null
        },
        leadingContent = {
            AndroidView(
                modifier = Modifier.size(24.dp),
                factory = { AppCompatImageView(it) },
                update = { image ->
                    val imageHolder = item.icon
                    val iconicsIcon = item.iconicsIcon
                    when {
                        imageHolder != null -> imageHolder.applyTo(image, null)
                        iconicsIcon != null -> image.setImageDrawable(
                            IconicsDrawable(context, iconicsIcon).apply { sizeDp = 24 },
                        )
                        else -> image.setImageDrawable(null)
                    }
                },
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                controller.close()
                item.onClickListener?.onClick(View(context))
            },
    )
}

@Composable
private fun NowPlayingLessonBar(
    lesson: NowLessonUi,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trwa • ${lesson.subject}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append("do ${lesson.endsAt}")
                            append(" • ${lesson.minutesRemaining} min")
                            lesson.room?.let { append(" • sala $it") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { lesson.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(100.dp)),
            )
        }
    }
}

@Composable
private fun LegacyFabIcon(controller: MainFabController) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.size(24.dp),
        factory = { AppCompatImageView(it) },
        update = { image ->
            image.setImageDrawable(
                controller.icon?.let { IconicsDrawable(context, it).apply { sizeDp = 24 } },
            )
        },
    )
}
