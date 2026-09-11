package pl.szczodrzynski.edziennik.ui.navigation

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        ?.let(NavTarget::getById)
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
            if (!expanded) {
                MainNavigationBar(
                    currentTarget = currentTarget,
                    moreSelected = moreSelected,
                    unreadCount = unreadCount,
                    onTargetSelected = activity::selectShellTarget,
                    onMoreSelected = { activity.openMore() },
                )
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
                    onMoreSelected = { activity.openMore() },
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
    unreadCount: (NavTarget?) -> Int,
    onTargetSelected: (NavTarget) -> Unit,
    onMoreSelected: () -> Unit,
) {
    NavigationBar(windowInsets = WindowInsets.navigationBars) {
        primaryDestinations.forEach { destination ->
            val selected = destination.target?.let { it == currentTarget } ?: moreSelected
            NavigationBarItem(
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(targets, key = NavTarget::id) { target ->
            val count = target.badgeType?.let { badgeType ->
                state.unreadCounters
                    .filter { it.profileId == state.profile.id && it.thingType == badgeType }
                    .sumOf { it.count }
            } ?: 0
            ListItem(
                headlineContent = { Text(stringResource(target.nameRes)) },
                supportingContent = target.descriptionRes?.let { description ->
                    { Text(stringResource(description)) }
                },
                leadingContent = target.icon?.let { icon ->
                    { Icon(icon, contentDescription = null) }
                },
                trailingContent = if (count > 0) {
                    { Badge { Text(if (count > 99) "99+" else count.toString()) } }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTargetSelected(target) },
            )
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
