package pl.szczodrzynski.edziennik.ui.settings.cards

import android.widget.Toast
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil

class SettingsSecurityCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        "Prywatność i bezpieczeństwo",
        items = ::getItems,
    )

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        util.createPropertyItem(
            text = "Blokada biometryczna",
            subText = "Wymagaj odcisku palca / twarzy przy otwieraniu aplikacji",
            icon = CommunityMaterial.Icon2.cmd_fingerprint,
            value = configGlobal.security.biometricLockEnabled,
        ) { _, isChecked ->
            if (isChecked && !app.biometricLockManager.isBiometricAvailable()) {
                Toast.makeText(
                    activity,
                    "Biometria lub blokada ekranu nie jest skonfigurowana w telefonie",
                    Toast.LENGTH_LONG
                ).show()
                refresh()
                return@createPropertyItem
            }
            configGlobal.security.biometricLockEnabled = isChecked
            if (isChecked) {
                app.biometricLockManager.markUnlocked()
            }
            refresh()
        },
        util.createPropertyItem(
            text = "Wyciszanie podczas lekcji (DND)",
            subText = "Automatycznie przełączaj w tryb Nie przeszkadzać w godzinach lekcji z planu",
            icon = CommunityMaterial.Icon.cmd_bell_off,
            value = configGlobal.security.dndDuringLessonsEnabled,
        ) { _, isChecked ->
            configGlobal.security.dndDuringLessonsEnabled = isChecked
            refresh()
        },
    )
}
