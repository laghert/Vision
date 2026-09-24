/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AppLanguageDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ThemeChooserDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil
import pl.szczodrzynski.edziennik.utils.BigNightUtil
import pl.szczodrzynski.edziennik.utils.models.Date

class SettingsThemeCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_theme_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        if (Date.getToday().month / 3 % 4 == 0)
            util.createPropertyItem(
                text = R.string.settings_theme_snowfall_text,
                subText = R.string.settings_theme_snowfall_subtext,
                icon = CommunityMaterial.Icon3.cmd_snowflake,
                value = configGlobal.ui.snowfall,
            ) { _, it ->
                configGlobal.ui.snowfall = it
                activity.recreate()
            }
        else null,
        if (BigNightUtil().isDataWielkanocyNearDzisiaj())
            util.createPropertyItem(
                text = R.string.settings_theme_eggfall_text,
                subText = R.string.settings_theme_eggfall_subtext,
                icon = CommunityMaterial.Icon.cmd_egg_easter,
                value = configGlobal.ui.eggfall,
            ) { _, it ->
                configGlobal.ui.eggfall = it
                activity.recreate()
            }
        else null,
        util.createActionItem(
            text = R.string.settings_theme_theme_text,
            subText = app.uiManager.themeColor.nameRes,
            icon = CommunityMaterial.Icon3.cmd_palette_outline,
        ) {
            ThemeChooserDialog(activity).show()
        },
        util.createActionItem(
            text = R.string.settings_theme_night_mode_text,
            subText = when (configGlobal.ui.themeNightMode) {
                true -> R.string.theme_dark
                false -> R.string.theme_light
                null -> R.string.settings_theme_theme_system
            },
            icon = CommunityMaterial.Icon3.cmd_theme_light_dark,
        ) {
            pl.szczodrzynski.edziennik.ui.dialogs.settings.NightModeDialog(activity).show()
        },
        util.createPropertyItem(
            text = R.string.settings_theme_amoled_text,
            subText = R.string.settings_theme_amoled_subtext,
            icon = CommunityMaterial.Icon.cmd_brightness_6,
            value = configGlobal.ui.themeBlackMode,
        ) { _, it ->
            configGlobal.ui.themeBlackMode = it
            if (it && configGlobal.ui.themeNightMode == false) {
                configGlobal.ui.themeNightMode = true
            }
            app.uiManager.applyNightMode()
            activity.recreate()
        },
        util.createActionItem(
            text = R.string.settings_about_language_text,
            subText = R.string.settings_about_language_subtext,
            icon = CommunityMaterial.Icon3.cmd_translate,
        ) {
            AppLanguageDialog(activity).show()
        },
    )

    override fun getItemsMore(card: MaterialAboutCard) = listOf(
        util.createActionItem(
            text = R.string.settings_theme_app_background_text,
            subText = R.string.settings_theme_app_background_subtext,
            icon = CommunityMaterial.Icon2.cmd_image_filter_hdr,
        ) {
            setAppBackground()
        },
    )

    private fun setAppBackground() = activity.requestHandler.requestAppBackground {
        activity.setAppBackground()
    }
}
