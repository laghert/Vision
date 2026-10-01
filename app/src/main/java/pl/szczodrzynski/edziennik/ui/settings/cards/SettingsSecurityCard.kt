package pl.szczodrzynski.edziennik.ui.settings.cards

import android.widget.Toast
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsSecurityCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_security_card_title,
        items = ::getItems,
        itemsMore = { emptyList() },
    )

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        util.createPropertyItem(
            text = R.string.settings_security_biometric_text,
            subText = R.string.settings_security_biometric_subtext,
            icon = CommunityMaterial.Icon2.cmd_fingerprint,
            value = configGlobal.security.biometricLockEnabled,
        ) { _, isChecked ->
            if (isChecked && !app.biometricLockManager.isBiometricAvailable()) {
                Toast.makeText(
                    activity,
                    "Biometria lub blokada ekranu nie jest skonfigurowana w telefonie",
                    Toast.LENGTH_LONG
                ).show()
                util.onRefresh()
                return@createPropertyItem
            }
            configGlobal.security.biometricLockEnabled = isChecked
            if (isChecked) {
                app.biometricLockManager.markUnlocked()
            }
            util.onRefresh()
        },
        util.createPropertyItem(
            text = R.string.settings_security_dnd_text,
            subText = R.string.settings_security_dnd_subtext,
            icon = CommunityMaterial.Icon.cmd_bell_off,
            value = configGlobal.security.dndDuringLessonsEnabled,
        ) { _, isChecked ->
            configGlobal.security.dndDuringLessonsEnabled = isChecked
            util.onRefresh()
        },
    )
}
