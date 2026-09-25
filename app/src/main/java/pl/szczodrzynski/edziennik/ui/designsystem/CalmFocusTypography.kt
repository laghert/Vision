package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.unit.sp

private fun TextStyle.withOpticalTracking(trackingSp: Float): TextStyle =
    copy(
        fontFeatureSettings = "tnum",
        letterSpacing = trackingSp.sp,
    )

private val MaterialTypography = Typography()

// Apple WWDC (The Details of UI Typography):
// Display/Headline text needs negative tracking (-0.02 to -0.01 em/sp) as size increases.
// Body text stays neutral (0). Small labels/captions need slightly positive tracking (+0.02) for legibility.
val CalmFocusTypography = Typography(
    displayLarge = MaterialTypography.displayLarge.withOpticalTracking(-0.8f),
    displayMedium = MaterialTypography.displayMedium.withOpticalTracking(-0.6f),
    displaySmall = MaterialTypography.displaySmall.withOpticalTracking(-0.4f),
    headlineLarge = MaterialTypography.headlineLarge.withOpticalTracking(-0.4f),
    headlineMedium = MaterialTypography.headlineMedium.withOpticalTracking(-0.3f),
    headlineSmall = MaterialTypography.headlineSmall.withOpticalTracking(-0.2f),
    titleLarge = MaterialTypography.titleLarge.withOpticalTracking(-0.15f),
    titleMedium = MaterialTypography.titleMedium.withOpticalTracking(0f),
    titleSmall = MaterialTypography.titleSmall.withOpticalTracking(0f),
    bodyLarge = MaterialTypography.bodyLarge.withOpticalTracking(0f),
    bodyMedium = MaterialTypography.bodyMedium.withOpticalTracking(0f),
    bodySmall = MaterialTypography.bodySmall.withOpticalTracking(0.1f),
    labelLarge = MaterialTypography.labelLarge.withOpticalTracking(0.1f),
    labelMedium = MaterialTypography.labelMedium.withOpticalTracking(0.15f),
    labelSmall = MaterialTypography.labelSmall.withOpticalTracking(0.2f),
)
