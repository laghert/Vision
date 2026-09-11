/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginPlatformListFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class LoginPlatformListFragment : Fragment() {
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginPlatformListFragmentBinding
    private val nav by lazy { activity.nav }
    private lateinit var adapter: LoginPlatformAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        b = LoginPlatformListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return
        b.backButton.onClick { nav.navigateUp() }

        val loginType = arguments?.getEnum<LoginType>("loginType") ?: return
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = arguments?.getEnum<LoginMode>("loginMode") ?: return
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        adapter = LoginPlatformAdapter(activity) { platform ->
            nav.navigate(
                R.id.loginFormFragment,
                Bundle(
                    "loginType" to loginType,
                    "loginMode" to loginMode,
                    "platformName" to platform.name,
                    "platformDescription" to platform.description,
                    "platformFormFields" to platform.formFields.joinToString(";"),
                    "platformData" to platform.data.toString(),
                    "platformStoreKey" to platform.storeKey,
                ),
                activity.navOptions,
            )
        }

        val platforms = LoginInfo.platformList[mode.name].orEmpty()
        adapter.items = platforms
        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }

        b.loadingLayout.isVisible = false
        b.list.isVisible = platforms.isNotEmpty()
        b.timeoutText.isVisible = platforms.isEmpty()
        b.reloadButton.isVisible = false
    }
}
