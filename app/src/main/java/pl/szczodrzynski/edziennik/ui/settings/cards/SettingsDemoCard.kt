package pl.szczodrzynski.edziennik.ui.settings.cards

import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDataSeeder
import pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDayManager
import pl.szczodrzynski.edziennik.data.api.edziennik.demo.DemoDayOption
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsDemoCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_demo_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    override fun getItems(card: MaterialAboutCard) = listOf(
        MaterialAboutActionItem.Builder()
            .text("Symulacja dnia tygodnia")
            .subText("Aktualnie: ${DemoDayManager.currentOption.value.emoji} ${DemoDayManager.currentOption.value.title}")
            .icon(CommunityMaterial.Icon.cmd_calendar_sync_outline.toDrawable(activity, null))
            .setOnClickAction {
                showDemoDayDialog()
            }
            .build()
    )

    private fun showDemoDayDialog() {
        val options = DemoDayOption.entries.toTypedArray()
        val currentIndex = options.indexOf(DemoDayManager.currentOption.value).coerceAtLeast(0)
        val titles = options.map { "${it.emoji} ${it.title}" }.toTypedArray()

        MaterialAlertDialogBuilder(activity)
            .setTitle("Symulacja dnia tygodnia")
            .setSingleChoiceItems(titles, currentIndex) { dialog, which ->
                dialog.dismiss()
                val selected = options[which]
                DemoDayManager.setOption(selected)
                activity.launch(Dispatchers.IO) {
                    DemoDataSeeder.seed(app, app.profileId)
                    withContext(Dispatchers.Main) {
                        util.refresh()
                        activity.retryProfileSync()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
