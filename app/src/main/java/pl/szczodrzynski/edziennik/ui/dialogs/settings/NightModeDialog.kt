/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class NightModeDialog(
    activity: AppCompatActivity,
) : BaseDialog<Int>(activity) {

    companion object {
        const val MODE_SYSTEM = 0
        const val MODE_DARK = 1
        const val MODE_LIGHT = 2
    }

    override fun getTitleRes() = R.string.settings_theme_night_mode_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Int> = mapOf(
        activity.getString(R.string.settings_theme_theme_system) to MODE_SYSTEM,
        activity.getString(R.string.theme_dark) to MODE_DARK,
        activity.getString(R.string.theme_light) to MODE_LIGHT,
    )

    override fun getDefaultSelectedItem(): Int = when (app.config.ui.themeNightMode) {
        null -> MODE_SYSTEM
        true -> MODE_DARK
        false -> MODE_LIGHT
    }

    override suspend fun onPositiveClick(): Boolean {
        val selection = getSingleSelection() ?: return DISMISS
        val newNightMode = when (selection) {
            MODE_DARK -> true
            MODE_LIGHT -> false
            else -> null
        }
        if (app.config.ui.themeNightMode != newNightMode) {
            app.config.ui.themeNightMode = newNightMode
            app.uiManager.applyNightMode()
            activity.recreate()
        }
        return DISMISS
    }
}
