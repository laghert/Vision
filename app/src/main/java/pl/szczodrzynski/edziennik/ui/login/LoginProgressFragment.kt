/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.UserActionManager
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUIRES_USER_ACTION
import pl.szczodrzynski.edziennik.data.api.LOGIN_NO_ARGUMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginProgressFragmentBinding
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ext.joinNotNullStrings
import pl.szczodrzynski.edziennik.ext.mergeWith
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

class LoginProgressFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginProgressFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginProgressFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private var timeoutJob: Job? = null

    private data class ReconciledLogin(
        val profiles: List<Profile>,
        val loginStores: List<LoginStore>,
        val emptyDuplicateProfileIds: Set<Int>,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginProgressFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        EventBus.getDefault().removeStickyEvent(FirstLoginFinishedEvent::class.java)

        val args = arguments ?: run {
            activity.error(ApiError(TAG, LOGIN_NO_ARGUMENTS))
            nav.navigateUp()
            return
        }

        doFirstLogin(args)
    }

    private fun doFirstLogin(args: Bundle) {
        timeoutJob?.cancel()
        timeoutJob = launch {
            kotlinx.coroutines.delay(45_000L) // 45s failsafe timeout
            if (isAdded && !isDetached) {
                activity.error(ApiError(TAG, pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_TIMEOUT))
                nav.navigateUp()
            }
        }

        launch {
            activity.errorSnackbar.dismiss()

            val maxProfileId = max(
                    app.db.profileDao().lastId ?: 0,
                    activity.profiles.maxByOrNull { it.profile.id }?.profile?.id ?: 0
            )
            val loginType = args.getEnum<LoginType>("loginType") ?: return@launch
            val loginMode = args.getEnum<LoginMode>("loginMode") ?: return@launch

            val loginStore = LoginStore(
                    id = maxProfileId + 1,
                    type = loginType,
                    mode = loginMode
            )
            loginStore.copyFrom(args)
            loginStore.removeLoginData("loginType")
            loginStore.removeLoginData("loginMode")
            EdziennikTask.firstLogin(loginStore).enqueue(activity)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onFirstLoginFinishedEvent(event: FirstLoginFinishedEvent) {
        timeoutJob?.cancel()
        EventBus.getDefault().removeStickyEvent(event)
        if (event.profileList.isEmpty()) {
            SimpleDialog<Unit>(activity) {
                title(R.string.login_account_no_students)
                message(R.string.login_account_no_students_text)
                positive(R.string.ok) {
                    nav.navigateUp()
                }
                cancelable(false)
            }.show()
            return
        }

        val sessionProfiles = activity.profiles.map { it.profile }
        val sessionLoginStores = activity.loginStores.toList()
        viewLifecycleOwner.lifecycleScope.launch {
            val reconciled = withContext(Dispatchers.IO) {
                reconcileLibrusLogin(event, sessionProfiles, sessionLoginStores)
            }

            // update subnames with school years and class name
            for (profile in reconciled.profiles) {
                val schoolYearName = "${profile.studentSchoolYearStart}/${profile.studentSchoolYearStart + 1}"
                profile.subname = joinNotNullStrings(
                        " - ",
                        profile.studentClassName,
                        schoolYearName
                )
            }

            val reconciledProfileIds = reconciled.profiles.map(Profile::id).toSet()
            val reconciledLoginStoreIds = reconciled.loginStores.map(LoginStore::id).toSet()
            activity.profiles.removeAll { it.profile.id in reconciledProfileIds }
            activity.loginStores.removeAll { it.id in reconciledLoginStoreIds }
            activity.profiles += reconciled.profiles.map { LoginSummaryAdapter.Item(it) }
            activity.loginStores += reconciled.loginStores
            activity.emptyDuplicateProfileIds += reconciled.emptyDuplicateProfileIds
            activity.errorSnackbar.dismiss()
            nav.navigate(R.id.loginSummaryFragment, null, activity.navOptions)
        }
    }

    private fun reconcileLibrusLogin(
        event: FirstLoginFinishedEvent,
        sessionProfiles: List<Profile>,
        sessionLoginStores: List<LoginStore>,
    ): ReconciledLogin {
        if (event.loginStore.type != LoginType.LIBRUS) {
            return ReconciledLogin(event.profileList, listOf(event.loginStore), emptySet())
        }

        val existingProfiles = (sessionProfiles + app.db.profileDao().allNow)
            .filter { it.loginStoreType == LoginType.LIBRUS && !it.archived }
            .distinctBy(Profile::id)
        val existingLoginStores = app.db.loginStoreDao().getAllNow()
            .associateBy(LoginStore::id)
            .toMutableMap()
            .apply { sessionLoginStores.forEach { put(it.id, it) } }
        val claimedProfileIds = mutableSetOf<Int>()
        val emptyDuplicateProfileIds = mutableSetOf<Int>()
        val reconciledProfiles = mutableListOf<Profile>()
        val reconciledLoginStores = linkedMapOf<Int, LoginStore>()

        val reconciliations = event.profileList.map { freshProfile ->
            val candidates = existingProfiles.filter {
                it.id !in claimedProfileIds && isSameLibrusAccount(it, freshProfile)
            }
            val canonicalProfile = candidates
                .filterNot(Profile::empty)
                .ifEmpty { candidates }
                .minByOrNull(Profile::id)
            canonicalProfile?.let { claimedProfileIds += it.id }
            Triple(freshProfile, canonicalProfile, candidates)
        }
        val reusedLoginStoreIds = reconciliations
            .mapNotNull { it.second?.loginStoreId }
            .distinct()
        val sharedLoginStoreId = reusedLoginStoreIds
            .singleOrNull()
            ?.takeIf { event.loginStore.mode == LoginMode.LIBRUS_EMAIL || event.loginStore.mode == LoginMode.LIBRUS_OAUTH }

        fun refreshedLoginStore(loginStoreId: Int): LoginStore {
            val oldLoginStore = existingLoginStores[loginStoreId]
            val refreshedLoginData = oldLoginStore?.data?.deepCopy()
                ?.mergeWith(event.loginStore.data.deepCopy())
                ?: event.loginStore.data.deepCopy()
            return LoginStore(
                id = loginStoreId,
                type = event.loginStore.type,
                mode = event.loginStore.mode,
                data = refreshedLoginData,
            )
        }

        reconciliations.forEach { (freshProfile, canonicalProfile, candidates) ->
            if (canonicalProfile == null) {
                val reconciledProfile = sharedLoginStoreId?.let {
                    rebindFreshProfile(freshProfile, it)
                } ?: freshProfile
                reconciledProfiles += reconciledProfile
                reconciledLoginStores[reconciledProfile.loginStoreId] =
                    if (reconciledProfile.loginStoreId == event.loginStore.id) event.loginStore
                    else refreshedLoginStore(reconciledProfile.loginStoreId)
                return@forEach
            }

            emptyDuplicateProfileIds += candidates
                .filter { it.id != canonicalProfile.id && it.empty }
                .map(Profile::id)

            mergeFreshLibrusProfile(canonicalProfile, freshProfile)
            reconciledProfiles += canonicalProfile
            reconciledLoginStores[canonicalProfile.loginStoreId] =
                refreshedLoginStore(canonicalProfile.loginStoreId)
        }

        return ReconciledLogin(
            profiles = reconciledProfiles,
            loginStores = reconciledLoginStores.values.toList(),
            emptyDuplicateProfileIds = emptyDuplicateProfileIds,
        )
    }

    private fun rebindFreshProfile(fresh: Profile, loginStoreId: Int) = Profile(
        id = fresh.id,
        loginStoreId = loginStoreId,
        loginStoreType = fresh.loginStoreType,
        name = fresh.name,
        subname = fresh.subname,
        studentNameLong = fresh.studentNameLong,
        studentNameShort = fresh.studentNameShort,
        accountName = fresh.accountName,
        studentData = fresh.studentData.deepCopy(),
    )

    private fun isSameLibrusAccount(first: Profile, second: Profile): Boolean {
        val firstAccountId = first.getStudentData("accountId", 0)
        val secondAccountId = second.getStudentData("accountId", 0)
        if (firstAccountId > 0 && secondAccountId > 0) {
            return firstAccountId == secondAccountId
        }

        val firstLogin = first.getStudentData("accountLogin", null)?.trim()
        val secondLogin = second.getStudentData("accountLogin", null)?.trim()
        return !firstLogin.isNullOrEmpty() &&
            !secondLogin.isNullOrEmpty() &&
            firstLogin.equals(secondLogin, ignoreCase = true)
    }

    private fun mergeFreshLibrusProfile(existing: Profile, fresh: Profile) {
        if (fresh.name.isNotBlank()) existing.name = fresh.name
        if (fresh.studentNameLong.isNotBlank()) existing.studentNameLong = fresh.studentNameLong
        if (fresh.studentNameShort.isNotBlank()) existing.studentNameShort = fresh.studentNameShort
        existing.accountName = fresh.accountName
        existing.studentData.mergeWith(fresh.studentData.deepCopy())
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSyncErrorEvent(event: ApiTaskErrorEvent) {
        timeoutJob?.cancel()
        EventBus.getDefault().removeStickyEvent(event)
        activity.error(event.error)
        nav.navigateUp()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserActionRequiredEvent(event: UserActionRequiredEvent) {
        val args = arguments ?: run {
            activity.error(ApiError(TAG, LOGIN_NO_ARGUMENTS))
            nav.navigateUp()
            return
        }

        val callback = UserActionManager.UserActionCallback(
            onSuccess = { data ->
                timeoutJob?.cancel()
                args.putAll(data)
                doFirstLogin(args)
            },
            onFailure = {
                timeoutJob?.cancel()
                activity.error(ApiError(TAG, ERROR_REQUIRES_USER_ACTION))
                nav.navigateUp()
            },
            onCancel = {
                timeoutJob?.cancel()
                nav.navigateUp()
            },
        )
        app.userActionManager.execute(activity, event, callback)
    }

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}
