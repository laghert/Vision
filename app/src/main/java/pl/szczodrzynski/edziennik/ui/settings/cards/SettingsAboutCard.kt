/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.dialogs.ChangelogDialog
import pl.szczodrzynski.edziennik.ui.dialogs.DevModeDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsLicenseActivity
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsAboutCard(util: SettingsUtil) : SettingsCard(util) {

    private var clickCounter = 0
    private val mediaPlayer by lazy {
        MediaPlayer.create(activity, R.raw.ogarnij_sie)
    }

    override fun buildCard() = util.createCard(
        null,
        items = ::getItems,
        itemsMore = ::getItemsMore,
        backgroundColor = R.attr.colorPrimaryContainer.resolveAttr(activity),
    )

    private val versionDetailsItem by lazy {
        util.createActionItem(
            text = R.string.settings_about_version_details_text,
            subText = R.string.settings_about_version_details_subtext,
            icon = CommunityMaterial.Icon.cmd_cellphone_information,
        ) {
            app.buildManager.showVersionDialog(activity)
        }
    }

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        util.createTitleItem(),

        util.createActionItem(
            text = R.string.settings_about_version_text,
            icon = CommunityMaterial.Icon2.cmd_information_outline,
            onClick = { item ->
                if (!card.items.contains(versionDetailsItem)) {
                    card.items.after(item, versionDetailsItem)
                    util.refresh()
                }
                clickCounter++
                item.subText = BuildConfig.VERSION_NAME
                if (clickCounter >= 7) {
                    mediaPlayer.start()
                    clickCounter = 0
                    if (app.buildManager.devModeEasy) {
                        DevModeDialog(activity).show()
                    }
                }
            },
        ).also { it.subText = BuildConfig.VERSION_NAME },

        util.createActionItem(
            text = R.string.settings_about_maintainer_text,
            subText = R.string.settings_about_maintainer_subtext,
            icon = CommunityMaterial.Icon.cmd_account_outline,
        ) {},

        util.createActionItem(
            text = R.string.settings_about_changelog_text,
            icon = CommunityMaterial.Icon3.cmd_radar,
        ) {
            ChangelogDialog(activity).show()
        },

        util.createActionItem(
            text = R.string.settings_about_licenses_text,
            icon = CommunityMaterial.Icon.cmd_code_braces,
        ) {
            activity.startActivity(Intent(activity, SettingsLicenseActivity::class.java))
        },

        if (App.devMode) {
            util.createActionItem(
                text = R.string.settings_about_crash_text,
                subText = R.string.settings_about_crash_subtext,
                icon = CommunityMaterial.Icon.cmd_bug_outline,
            ) {
                Toast.makeText(activity, R.string.settings_about_crash_text, Toast.LENGTH_SHORT).show()
                throw RuntimeException("MANUAL CRASH")
            }
        } else {
            null
        },
    )
}
