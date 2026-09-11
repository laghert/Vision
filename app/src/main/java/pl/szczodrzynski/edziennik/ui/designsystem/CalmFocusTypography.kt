package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private fun TextStyle.withTabularNumerals() = copy(fontFeatureSettings = "tnum")

private val MaterialTypography = Typography()

val CalmFocusTypography = Typography(
    displayLarge = MaterialTypography.displayLarge.withTabularNumerals(),
    displayMedium = MaterialTypography.displayMedium.withTabularNumerals(),
    displaySmall = MaterialTypography.displaySmall.withTabularNumerals(),
    headlineLarge = MaterialTypography.headlineLarge.withTabularNumerals(),
    headlineMedium = MaterialTypography.headlineMedium.withTabularNumerals(),
    headlineSmall = MaterialTypography.headlineSmall.withTabularNumerals(),
    titleLarge = MaterialTypography.titleLarge.withTabularNumerals(),
    titleMedium = MaterialTypography.titleMedium.withTabularNumerals(),
    titleSmall = MaterialTypography.titleSmall.withTabularNumerals(),
    bodyLarge = MaterialTypography.bodyLarge.withTabularNumerals(),
    bodyMedium = MaterialTypography.bodyMedium.withTabularNumerals(),
    bodySmall = MaterialTypography.bodySmall.withTabularNumerals(),
    labelLarge = MaterialTypography.labelLarge.withTabularNumerals(),
    labelMedium = MaterialTypography.labelMedium.withTabularNumerals(),
    labelSmall = MaterialTypography.labelSmall.withTabularNumerals(),
)
