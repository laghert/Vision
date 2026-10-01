package pl.szczodrzynski.edziennik

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.core.manager.UserActionManager
import pl.szczodrzynski.edziennik.core.work.AppManagerDetectedEvent
import pl.szczodrzynski.edziennik.core.work.SyncWorker
import pl.szczodrzynski.edziennik.data.api.ERROR_VULCAN_API_DEPRECATED
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskProgressEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskStartedEvent
import pl.szczodrzynski.edziennik.data.api.events.ProfileListEmptyEvent
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.getAppData
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.getIntOrNull
import pl.szczodrzynski.edziennik.ext.hasUIFeature
import pl.szczodrzynski.edziennik.ext.isBeforeYear
import pl.szczodrzynski.edziennik.ext.putExtras
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.shouldArchive
import pl.szczodrzynski.edziennik.ext.takePositive
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.designsystem.CalmFocusTheme
import pl.szczodrzynski.edziennik.ui.dialogs.ChangelogDialog
import pl.szczodrzynski.edziennik.ui.dialogs.ErrorDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ProfileConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.SyncViewListDialog
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.main.ErrorSnackbar
import pl.szczodrzynski.edziennik.ui.main.MainSnackbar
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.ui.navigation.CalmFocusMainShell
import pl.szczodrzynski.edziennik.ui.navigation.CalmFocusRoute
import pl.szczodrzynski.edziennik.ui.navigation.MainShellState
import pl.szczodrzynski.edziennik.ui.navigation.createFragment
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment
import pl.szczodrzynski.edziennik.utils.PausedNavigationData
import pl.szczodrzynski.edziennik.utils.appManagerIntentList
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), CoroutineScope {
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    val app: App by lazy { applicationContext as App }
    val mainSnackbar: MainSnackbar by lazy { MainSnackbar(this) }
    val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }
    val requestHandler by lazy { MainActivityRequestHandler(this) }

    private lateinit var shellState: MainShellState
    val bottomSheet get() = shellState.contextActions
    val fabController get() = shellState.fab
    val swipeRefreshLayout get() = shellState.refresh

    var onBeforeNavigate: (() -> Boolean)? = null
    private var pausedNavigationData: PausedNavigationData? = null
    private var pausedNavigationResetsStack = false
    private var pausedNavigateUp = false
    private var pausedOpenMore = false

    lateinit var navTarget: NavTarget
        private set
    private var navArguments: Bundle? = null
    private var navController: NavController? = null
    private var nextEntryId = System.currentTimeMillis()
    private var pendingNavigation: PendingNavigation? = null
    private var eventReceiversRegistered = false

    private data class PendingNavigation(
        val target: NavTarget,
        val arguments: Bundle?,
        val replaceCurrent: Boolean,
        val resetStack: Boolean,
    )

    private val destinationListener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
        if (!::shellState.isInitialized) return@OnDestinationChangedListener
        bottomSheet.removeAllContextual()
        fabController.reset()
        swipeRefreshLayout.isEnabled = false
        controller.currentBackStackEntry
            ?.savedStateHandle
            ?.get<Bundle>(CalmFocusRoute.SCREEN_ARGUMENTS_KEY)
            ?.let { navArguments = it }
        if (destination.route == CalmFocusRoute.SCREEN_PATTERN) {
            arguments
                ?.takeIf { it.containsKey(CalmFocusRoute.TARGET_ID_ARGUMENT) }
                ?.getInt(CalmFocusRoute.TARGET_ID_ARGUMENT)
                ?.let(NavTarget::getByIdOrNull)
                ?.let {
                navTarget = it
                shellState.target = it
                updateTaskDescription(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Legacy fragments are recreated by the Compose destination host. Prevent FragmentManager
        // from restoring them before their stable FragmentContainerView exists.
        savedInstanceState?.remove("android:support:fragments")
        super.onCreate(savedInstanceState)
        Timber.i("Activity created")

        app.uiManager.applyTheme(this)
        app.uiManager.applyLanguage(this)
        app.buildManager.validateBuild(this)

        if (App.profileId == 0) {
            onProfileListEmptyEvent(ProfileListEmptyEvent())
            return
        }

        val launchExtras = Bundle().apply {
            intent?.extras?.let { putAll(it) }
            savedInstanceState?.let { putAll(it) }
        }
        val requestedProfileId = launchExtras.getIntOrNull("profileId").takePositive()
        if (requestedProfileId != null && requestedProfileId != App.profileId) {
            app.profileLoad(requestedProfileId) {
                if (!isFinishing && !isDestroyed) initializeShell(launchExtras)
            }
        } else {
            initializeShell(launchExtras)
        }
    }

    private fun initializeShell(launchExtras: Bundle) {
        val requestedTarget = launchExtras.getEnum<NavTarget>("fragmentId") ?: NavTarget.HOME
        navTarget = if (
            (requestedTarget.devModeOnly && !App.devMode) ||
            (requestedTarget.featureType != null && !app.profile.hasUIFeature(requestedTarget.featureType)) ||
            requestedTarget in setOf(
                NavTarget.PROFILE_ADD,
                NavTarget.PROFILE_MARK_AS_READ,
                NavTarget.PROFILE_SYNC_ALL,
            )
        ) {
            NavTarget.HOME
        } else {
            requestedTarget
        }
        navArguments = launchExtras.navigationArguments()
        shellState = MainShellState(app.profile).also { it.target = navTarget }

        enableEdgeToEdge()
        setContent {
            CalmFocusTheme(amoled = app.config.ui.themeBlackMode) {
                CalmFocusMainShell(
                    activity = this,
                    state = shellState,
                    initialRoute = CalmFocusRoute.Screen(navTarget, 0L),
                    initialArguments = navArguments,
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = navigateUp()
        })

        app.db.profileDao().all.observe(this) { profiles ->
            val allArchived = profiles.all { it.archived }
            shellState.profiles = profiles.filter { it.id >= 0 && (!it.archived || allArchived) }
            profiles.firstOrNull { it.id == App.profileId }?.let { currentProfile ->
                shellState.profile = currentProfile
            }
        }
        app.db.metadataDao().unreadCounts.observe(this) {
            shellState.unreadCounters = it
        }

        setupContextActions()
        handleIntent(launchExtras, isInitial = true)
        SyncWorker.scheduleNext(app)
        setAppBackground()
        registerEventReceiversIfNeeded()

        if (app.profile.archived) {
            launch {
                val profile = app.profile.archiveId?.let { archiveId ->
                    withContext(Dispatchers.IO) { app.db.profileDao().getNotArchivedOf(archiveId) }
                }
                if (profile != null) selectProfile(profile) else navigate(profileId = 0)
            }
        }

        if (app.profile.loginStoreType == pl.szczodrzynski.edziennik.data.enums.LoginType.DEMO) {
            launch(Dispatchers.IO) {
                pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder.seed(app, app.profileId)
            }
        }

        updateLoginTimestamps(App.profileId)

        if (app.config.appVersion < BuildConfig.VERSION_CODE) {
            ChangelogDialog(this).show()
            if (app.config.appVersion >= 170) app.config.appVersion = BuildConfig.VERSION_CODE
        }
    }

    fun updateLoginTimestamps(profileId: Int) {
        if (profileId < 0) return
        val ui = app.config[profileId].ui
        val now = System.currentTimeMillis()
        if (ui.lastLoginTime > 0L) {
            if (now - ui.lastLoginTime > 10 * 60 * 1000L) {
                ui.previousLoginTime = ui.lastLoginTime
                ui.lastLoginTime = now
            }
        } else {
            ui.previousLoginTime = now - 7 * 24 * 3600 * 1000L
            ui.lastLoginTime = now
        }
    }

    fun markTargetSeen(target: NavTarget) {
        val pid = App.profileId
        launch(Dispatchers.IO) {
            when (target) {
                NavTarget.GRADES -> app.db.metadataDao().setAllSeen(pid, MetadataType.GRADE, true)
                NavTarget.AGENDA -> {
                    app.db.metadataDao().setAllSeen(pid, MetadataType.EVENT, true)
                    app.db.metadataDao().setAllSeen(pid, MetadataType.HOMEWORK, true)
                }
                NavTarget.TIMETABLE -> app.db.metadataDao().setAllSeen(pid, MetadataType.LESSON_CHANGE, true)
                NavTarget.ATTENDANCE -> app.db.metadataDao().setAllSeen(pid, MetadataType.ATTENDANCE, true)
                NavTarget.BEHAVIOUR -> app.db.metadataDao().setAllSeen(pid, MetadataType.NOTICE, true)
                NavTarget.ANNOUNCEMENTS -> app.db.metadataDao().setAllSeen(pid, MetadataType.ANNOUNCEMENT, true)
                else -> {}
            }
        }
    }

    fun markAllSeen() {
        val pid = App.profileId
        launch(Dispatchers.IO) {
            app.db.metadataDao().setAllSeenExceptMessages(pid, true)
        }
    }

    private fun Bundle.navigationArguments() = Bundle(this).apply {
        remove("profileId")
        remove("fragmentId")
        remove("reloadProfileId")
    }

    private fun setupContextActions() {
        bottomSheet.removeAllItems()
        bottomSheet.appendItems(
            BottomSheetPrimaryItem(false)
                .withTitle(R.string.menu_sync)
                .withIcon(CommunityMaterial.Icon.cmd_download_outline)
                .withOnClickListener {
                    bottomSheet.close()
                    SyncViewListDialog(this, navTarget).show()
                },
            BottomSheetSeparatorItem(false),
        )
        if (App.devMode) {
            bottomSheet += BottomSheetPrimaryItem(false)
                .withTitle(NavTarget.DEBUG.nameRes)
                .withIcon(CommunityMaterial.Icon.cmd_android_debug_bridge)
                .withOnClickListener { navigate(navTarget = NavTarget.DEBUG) }
        }
        bottomSheet.onCloseListener = {
            if (!app.config.ui.bottomSheetOpened) app.config.ui.bottomSheetOpened = true
        }
    }

    fun availableMoreTargets(): List<NavTarget> = listOf(
        NavTarget.AGENDA,
        NavTarget.HOMEWORK,
        NavTarget.BEHAVIOUR,
        NavTarget.ATTENDANCE,
        NavTarget.ANNOUNCEMENTS,
        NavTarget.NOTES,
        NavTarget.TEACHERS,
        NavTarget.NOTIFICATIONS,
        NavTarget.SETTINGS,
        NavTarget.LAB,
        NavTarget.TEMPLATE,
        NavTarget.DEBUG,
    ).filter { target ->
        (!target.devModeOnly || App.devMode) &&
            (target.featureType == null || app.profile.hasUIFeature(target.featureType))
    }

    fun selectProfile(profile: Profile) {
        updateLoginTimestamps(profile.id)
        navigate(profileId = profile.id, navTarget = navTarget)
    }

    fun handleProfileAction(target: NavTarget) {
        when (target) {
            NavTarget.PROFILE_ADD -> requestHandler.requestLogin()
            NavTarget.PROFILE_SYNC_ALL -> EdziennikTask.sync().enqueue(this)
            NavTarget.PROFILE_MARK_AS_READ -> launch {
                withContext(Dispatchers.Default) {
                    app.db.profileDao().allNow.forEach { profile ->
                        if (!profile.getAppData().uiConfig.enableMarkAsReadAnnouncements) {
                            app.db.metadataDao().setAllSeenExceptMessagesAndAnnouncements(profile.id, true)
                        } else {
                            app.db.metadataDao().setAllSeenExceptMessages(profile.id, true)
                        }
                    }
                }
                Toast.makeText(
                    this@MainActivity,
                    R.string.main_menu_mark_as_read_success,
                    Toast.LENGTH_SHORT,
                ).show()
            }
            else -> navigate(navTarget = target)
        }
    }

    fun selectShellTarget(target: NavTarget) {
        val controller = navController ?: return
        val currentEntry = controller.currentBackStackEntry
        if (currentEntry?.destination?.route == CalmFocusRoute.More.value && target == navTarget) {
            controller.popBackStack()
            return
        }
        val currentTargetId = currentEntry?.arguments?.getInt(CalmFocusRoute.TARGET_ID_ARGUMENT)
        if (currentEntry?.destination?.route == CalmFocusRoute.SCREEN_PATTERN && currentTargetId == target.id) {
            return
        }
        navigate(navTarget = target, resetBackStack = true)
    }

    fun selectMoreTarget(target: NavTarget) {
        navigate(navTarget = target)
    }

    fun openMore(skipBeforeNavigate: Boolean = false) {
        if (!skipBeforeNavigate && !canNavigate()) {
            pausedOpenMore = true
            return
        }
        pausedOpenMore = false
        bottomSheet.close()
        navController?.navigate(CalmFocusRoute.More.value) {
            launchSingleTop = true
        }
    }

    internal fun bindNavController(controller: NavController) {
        navController?.removeOnDestinationChangedListener(destinationListener)
        navController = controller
        controller.addOnDestinationChangedListener(destinationListener)
        pendingNavigation?.let { pending ->
            pendingNavigation = null
            navigateController(
                pending.target,
                pending.arguments,
                pending.replaceCurrent,
                pending.resetStack,
            )
        }
    }

    internal fun unbindNavController(controller: NavController) {
        if (navController === controller) {
            controller.removeOnDestinationChangedListener(destinationListener)
            navController = null
        }
    }

    internal fun mountFragment(
        target: NavTarget,
        arguments: Bundle?,
        containerId: Int,
        tag: String,
    ) {
        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing != null && existing.id == containerId) return
        val fragment = target.createFragment(arguments) ?: return
        supportFragmentManager.beginTransaction().apply {
            existing?.let(::remove)
            replace(containerId, fragment, tag)
        }.commitAllowingStateLoss()
    }

    internal fun unmountFragment(tag: String) {
        supportFragmentManager.findFragmentByTag(tag)?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }

    internal fun bindSnackbarHost(coordinator: CoordinatorLayout) {
        mainSnackbar.setCoordinator(coordinator)
        errorSnackbar.setCoordinator(coordinator)
    }

    internal fun refreshCurrentFeature() {
        launch { syncCurrentFeature() }
    }

    internal fun retryProfileSync() {
        if (app.profile.loginStoreType == pl.szczodrzynski.edziennik.data.enums.LoginType.DEMO) {
            val weekDate = TimetableFragment.pageSelection ?: Date.getToday()
            launch(Dispatchers.IO) {
                pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder.seed(app, app.profileId)
                pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder.seedWeek(app, app.profileId, weekDate.weekStart)
            }
        }
        launch { syncCurrentFeature(forceFullSync = true) }
    }

    internal fun syncToday(force: Boolean = false) {
        if (app.profile.loginStoreType == pl.szczodrzynski.edziennik.data.enums.LoginType.DEMO) {
            val weekDate = TimetableFragment.pageSelection ?: Date.getToday()
            launch(Dispatchers.IO) {
                pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder.seed(app, app.profileId)
                pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder.seedWeek(app, app.profileId, weekDate.weekStart)
            }
        }
        launch {
            val features = setOf(
                FeatureType.TIMETABLE,
                FeatureType.GRADES,
                FeatureType.AGENDA,
                FeatureType.ATTENDANCE,
                FeatureType.BEHAVIOUR,
                FeatureType.ANNOUNCEMENTS,
            )
            val arguments = JsonObject(
                "weekStart" to Date.getToday().weekStart.stringY_m_d,
            )
            val syncTask = EdziennikTask.syncProfile(
                profileId = App.profileId,
                featureTypes = features,
                onlyEndpoints = if (force) emptySet() else null,
                arguments = arguments,
            )
            syncTask.enqueue(this@MainActivity)
        }
    }

    private fun canNavigate(): Boolean = onBeforeNavigate?.invoke() != false

    fun resumePausedNavigation(): Boolean {
        if (pausedNavigateUp) {
            pausedNavigateUp = false
            navigateUp(skipBeforeNavigate = true)
            return true
        }
        if (pausedOpenMore) {
            pausedOpenMore = false
            openMore(skipBeforeNavigate = true)
            return true
        }
        val data = pausedNavigationData ?: return false
        val resetBackStack = pausedNavigationResetsStack
        pausedNavigationData = null
        pausedNavigationResetsStack = false
        return navigate(
            profileId = data.profileId,
            navTarget = data.navTarget,
            args = data.args,
            skipBeforeNavigate = true,
            resetBackStack = resetBackStack,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigate(
        profileId: Int? = null,
        profile: Profile? = null,
        navTarget: NavTarget? = null,
        args: Bundle? = null,
        skipBeforeNavigate: Boolean = false,
        skipBottomNavUpdate: Boolean = false,
        resetBackStack: Boolean = false,
    ): Boolean {
        val target = navTarget ?: this.navTarget
        val requestedProfileId = profile?.id ?: profileId
        val profileChanging = requestedProfileId != null && requestedProfileId != App.profileId
        Timber.d("navigate(profileId = $requestedProfileId, target = ${target.name}, args = $args)")
        if (!skipBeforeNavigate && (profileChanging || target != this.navTarget) && !canNavigate()) {
            bottomSheet.close()
            pausedNavigationData = PausedNavigationData(requestedProfileId, target, args)
            pausedNavigationResetsStack = resetBackStack || profileChanging
            return false
        }
        pausedNavigateUp = false
        pausedOpenMore = false

        when {
            profile != null && profile.id != App.profileId -> {
                navigateImpl(
                    profile,
                    target,
                    args,
                    profileChanged = true,
                    resetBackStack = true,
                )
            }
            profileId != null && profileId != App.profileId -> {
                app.profileLoad(profileId) {
                    navigateImpl(
                        it,
                        target,
                        args,
                        profileChanged = true,
                        resetBackStack = true,
                    )
                }
            }
            else -> navigateImpl(
                App.profile,
                target,
                args,
                profileChanged = false,
                resetBackStack = resetBackStack,
            )
        }
        return true
    }

    private fun navigateImpl(
        profile: Profile,
        target: NavTarget,
        args: Bundle?,
        profileChanged: Boolean,
        resetBackStack: Boolean,
    ) {
        if (target.featureType != null && !profile.hasUIFeature(target.featureType)) {
            navigateImpl(profile, NavTarget.HOME, args, profileChanged, resetBackStack)
            return
        }
        if (target == NavTarget.PROFILE_ADD ||
            target == NavTarget.PROFILE_MARK_AS_READ ||
            target == NavTarget.PROFILE_SYNC_ALL
        ) {
            handleProfileAction(target)
            return
        }

        if (profileChanged) {
            if (App.profileId != profile.id) app.profileLoad(profile)
            MessagesFragment.pageSelection = -1
            shellState.profile = app.profile
        }

        val currentEntry = navController?.currentBackStackEntry
        val replacingCurrentTarget = currentEntry?.destination?.route == CalmFocusRoute.More.value ||
            (currentEntry?.destination?.route == CalmFocusRoute.SCREEN_PATTERN &&
                currentEntry.arguments?.getInt(CalmFocusRoute.TARGET_ID_ARGUMENT) == target.id)
        swipeRefreshLayout.isEnabled = false
        bottomSheet.close()
        bottomSheet.removeAllContextual()
        fabController.reset()
        navTarget = target
        navArguments = args ?: Bundle()
        shellState.target = target
        shellState.subtitle = null
        navigateController(
            target,
            navArguments,
            replaceCurrent = replacingCurrentTarget,
            resetStack = resetBackStack || profileChanged,
        )
    }

    private fun navigateController(
        target: NavTarget,
        args: Bundle?,
        replaceCurrent: Boolean,
        resetStack: Boolean,
    ) {
        val controller = navController
        if (controller == null) {
            pendingNavigation = PendingNavigation(target, args, replaceCurrent, resetStack)
            return
        }
        val entryId = nextEntryId++
        controller.navigate(CalmFocusRoute.Screen(target, entryId).value) {
            when {
                resetStack -> popUpTo(controller.graph.id) {
                    inclusive = false
                }
                replaceCurrent -> popUpTo(controller.currentDestination?.route.orEmpty()) {
                    inclusive = true
                }
            }
        }
        controller.currentBackStackEntry
            ?.savedStateHandle
            ?.set(CalmFocusRoute.SCREEN_ARGUMENTS_KEY, args ?: Bundle())
        navArguments = args ?: Bundle()
    }

    fun reloadTarget() = navigate(navTarget = navTarget, args = navArguments)

    fun navigateUp(skipBeforeNavigate: Boolean = false) {
        if (!skipBeforeNavigate && !canNavigate()) {
            pausedNavigateUp = true
            return
        }
        pausedNavigateUp = false
        bottomSheet.close()
        if (navController?.popBackStack() == true) return
        navTarget.popTo?.let {
            navigate(navTarget = it, skipBeforeNavigate = true)
            return
        }
        finishAfterTransition()
    }

    fun gainAttention() = Unit

    fun gainAttentionFAB() {
        fabController.extended = false
        window.decorView.postDelayed({ fabController.extended = true }, 1000)
        window.decorView.postDelayed({ fabController.extended = false }, 3000)
    }

    fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.hideSoftInputFromWindow(currentFocus?.windowToken ?: window.decorView.windowToken, 0)
    }

    fun setAppBackground() {
        try {
            window.decorView.background = app.config.ui.appBackground?.let {
                if (it.endsWith(".gif")) GifDrawable(it) else BitmapDrawable.createFromPath(it)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun syncCurrentFeature(forceFullSync: Boolean = false) {
        if (app.profile.archived) {
            SimpleDialog<Unit>(this) {
                title(R.string.profile_archived_title)
                message(
                    R.string.profile_archived_text,
                    app.profile.studentSchoolYearStart,
                    app.profile.studentSchoolYearStart + 1,
                )
                positive(R.string.ok)
            }.show()
            swipeRefreshLayout.isRefreshing = false
            return
        }
        if (app.profile.shouldArchive()) {
            SimpleDialog<Unit>(this) {
                title(R.string.profile_archiving_title)
                message(R.string.profile_archiving_format, app.profile.dateYearEnd.formattedString)
                positive(R.string.ok)
            }.show()
        }
        if (app.profile.isBeforeYear()) {
            SimpleDialog<Unit>(this) {
                title(R.string.profile_year_not_started_title)
                message(R.string.profile_year_not_started_format, app.profile.dateSemester1Start.formattedString)
                positive(R.string.ok)
            }.show()
            swipeRefreshLayout.isRefreshing = false
            return
        }

        swipeRefreshLayout.isRefreshing = true
        Toast.makeText(
            this,
            if (forceFullSync) R.string.sync_feature_syncing_all else fragmentToSyncName(navTarget),
            Toast.LENGTH_SHORT,
        ).show()
        val featureType = if (forceFullSync) {
            null
        } else {
            when (navTarget) {
                NavTarget.MESSAGES -> if (MessagesFragment.pageSelection == Message.TYPE_SENT) {
                    FeatureType.MESSAGES_SENT
                } else {
                    FeatureType.MESSAGES_INBOX
                }
                else -> navTarget.featureType
            }
        }
        val arguments = when (navTarget) {
            NavTarget.TIMETABLE -> JsonObject(
                "weekStart" to (TimetableFragment.pageSelection ?: Date.getToday()).weekStart.stringY_m_d,
            )
            else -> null
        }
        val syncTask = if (forceFullSync) {
            EdziennikTask.syncProfile(
                App.profileId,
                onlyEndpoints = emptySet(),
                arguments = arguments,
            )
        } else {
            EdziennikTask.syncProfile(
                App.profileId,
                featureType?.let(::setOf),
                arguments = arguments,
            )
        }
        syncTask.enqueue(this)
    }

    private fun fragmentToSyncName(target: NavTarget): Int = when (target) {
        NavTarget.TIMETABLE -> R.string.sync_feature_timetable
        NavTarget.AGENDA -> R.string.sync_feature_agenda
        NavTarget.GRADES -> R.string.sync_feature_grades
        NavTarget.HOMEWORK -> R.string.sync_feature_homework
        NavTarget.BEHAVIOUR -> R.string.sync_feature_notices
        NavTarget.ATTENDANCE -> R.string.sync_feature_attendance
        NavTarget.MESSAGES -> if (MessagesFragment.pageSelection == Message.TYPE_SENT) {
            R.string.sync_feature_messages_outbox
        } else {
            R.string.sync_feature_messages_inbox
        }
        NavTarget.ANNOUNCEMENTS -> R.string.sync_feature_announcements
        else -> R.string.sync_feature_syncing_all
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskStartedEvent(event: ApiTaskStartedEvent) {
        swipeRefreshLayout.isRefreshing = true
        if (event.profileId == App.profileId) {
            shellState.subtitle = getString(R.string.toolbar_subtitle_syncing)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskProgressEvent(event: ApiTaskProgressEvent) {
        if (event.profileId == App.profileId) {
            shellState.subtitle = if (event.progress < 0f) {
                event.progressText.orEmpty()
            } else {
                getString(
                    R.string.toolbar_subtitle_syncing_format,
                    event.progress.roundToInt(),
                    event.progressText.orEmpty(),
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskFinishedEvent(event: ApiTaskFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.profileId == App.profileId) shellState.subtitle = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskAllFinishedEvent(event: ApiTaskAllFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        swipeRefreshLayout.isRefreshing = false
        shellState.subtitle = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskErrorEvent(event: ApiTaskErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.error.errorCode == ERROR_VULCAN_API_DEPRECATED &&
            event.error.profileId == App.profileId
        ) {
            ErrorDetailsDialog(this, listOf(event.error)).show()
        }
        shellState.subtitle = null
        mainSnackbar.dismiss()
        errorSnackbar.addError(event.error).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProfileListEmptyEvent(event: ProfileListEmptyEvent) {
        app.config.loginFinished = false
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAppManagerDetectedEvent(event: AppManagerDetectedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (app.config.sync.dontShowAppManagerDialog) return
        SimpleDialog<Unit>(this) {
            title(R.string.app_manager_dialog_title)
            message(R.string.app_manager_dialog_text)
            positive(R.string.ok) {
                try {
                    appManagerIntentList
                        .filter { packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
                        .forEach(::startActivity)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (inner: Exception) {
                        Timber.e(inner)
                        Toast.makeText(
                            this@MainActivity,
                            R.string.app_manager_open_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
            neutral(R.string.dont_ask_again) {
                app.config.sync.dontShowAppManagerDialog = true
            }
            cancelable(false)
        }.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserActionRequiredEvent(event: UserActionRequiredEvent) {
        app.userActionManager.execute(this, event, UserActionManager.UserActionCallback())
    }

    private val intentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntent(intent?.extras)
        }
    }

    fun handleIntent(extras: Bundle?) = handleIntent(extras, isInitial = false)

    private fun handleIntent(extras: Bundle?, isInitial: Boolean) {
        Timber.d("handleIntent() ${extras?.keySet()}")
        val intentProfileId = extras.getIntOrNull("profileId").takePositive()
        val intentTarget = extras.getEnum<NavTarget>("fragmentId")

        val handledAction = when (extras?.getString("action")) {
            "userActionRequired" -> {
                val type = extras.getEnum<UserActionRequiredEvent.Type>("type") ?: return
                val params = extras.getBundle("params") ?: return
                app.userActionManager.execute(
                    this,
                    UserActionRequiredEvent(
                        profileId = extras.getInt("profileId"),
                        type = type,
                        params = params,
                        errorText = 0,
                    ),
                    UserActionManager.UserActionCallback(),
                )
                true
            }
            "createManualEvent" -> {
                val date = extras.getString("eventDate")?.let(Date::fromY_m_d) ?: Date.getToday()
                EventManualDialog(this, App.profileId, defaultDate = date).show()
                true
            }
            else -> false
        }
        if (handledAction && !isInitial) return

        if (extras?.containsKey("reloadProfileId") == true) {
            val reloadProfileId = extras.getIntOrNull("reloadProfileId").takePositive()
            if (reloadProfileId == null || app.profile.id == reloadProfileId) {
                reloadTarget()
                return
            }
        }

        if (isInitial && (intentProfileId == null || intentProfileId == App.profileId)) return
        val args = extras?.navigationArguments()
        when {
            app.profile.id == 0 -> navigate(
                profileId = intentProfileId ?: app.config.lastProfileId,
                navTarget = intentTarget ?: navTarget,
                args = args,
            )
            intentProfileId != null -> navigate(
                profileId = intentProfileId,
                navTarget = intentTarget ?: navTarget,
                args = args,
            )
            intentTarget != null -> navigate(navTarget = intentTarget, args = args)
        }
    }

    override fun recreate() = recreate(navTarget)

    fun recreate(navTarget: NavTarget) = recreate(navTarget, null)

    fun recreate(navTarget: NavTarget? = null, arguments: Bundle? = null) {
        val restartIntent = Intent(this, MainActivity::class.java)
        arguments?.let(restartIntent::putExtras)
        navTarget?.let { restartIntent.putExtras("fragmentId" to it) }
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        startActivity(restartIntent)
    }

    private fun registerEventReceiversIfNeeded() {
        if (!::shellState.isInitialized || eventReceiversRegistered) return
        ContextCompat.registerReceiver(
            this,
            intentReceiver,
            IntentFilter(Intent.ACTION_MAIN),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        EventBus.getDefault().register(this)
        eventReceiversRegistered = true
    }

    private var lastForegroundSyncTime = 0L

    private fun checkFastForegroundSync() {
        val now = System.currentTimeMillis()
        if (now - lastForegroundSyncTime > 4 * 60 * 1000L && app.profile.id >= 0 && !app.profile.archived) {
            lastForegroundSyncTime = now
            syncToday(force = false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (app.biometricLockManager.isUnlockRequired()) {
            app.biometricLockManager.requestUnlock(
                activity = this,
                onSuccess = {
                    registerEventReceiversIfNeeded()
                    checkFastForegroundSync()
                },
                onCancel = {
                    finish()
                }
            )
            return
        }
        registerEventReceiversIfNeeded()
        checkFastForegroundSync()
    }

    override fun onPause() {
        if (eventReceiversRegistered) {
            unregisterReceiver(intentReceiver)
            EventBus.getDefault().unregister(this)
            eventReceiversRegistered = false
        }
        super.onPause()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::navTarget.isInitialized) return
        outState.putExtras("fragmentId" to navTarget)
        navArguments?.let(outState::putAll)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent.extras)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        requestHandler.handleResult(requestCode, resultCode, data)
    }

    private fun updateTaskDescription(target: NavTarget) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_vision_foreground)
        @Suppress("deprecation")
        setTaskDescription(
            ActivityManager.TaskDescription(
                if (target == NavTarget.HOME) getString(R.string.app_name)
                else getString(R.string.app_task_format, getString(target.nameRes)),
                bitmap,
                R.attr.colorPrimary.resolveAttr(this),
            ),
        )
    }

    fun error(error: ApiError) = errorSnackbar.addError(error).show()

    fun snackbar(
        text: String,
        actionText: String? = null,
        onClick: (() -> Unit)? = null,
    ) = mainSnackbar.snackbar(text, actionText, onClick)

    fun snackbarDismiss() = mainSnackbar.dismiss()
}
