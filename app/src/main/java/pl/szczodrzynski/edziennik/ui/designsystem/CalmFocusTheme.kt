package pl.szczodrzynski.edziennik.ui.designsystem

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import pl.szczodrzynski.edziennik.ext.isNightMode
import com.google.android.material.R as MaterialR

@Composable
fun CalmFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = darkTheme || amoled || context.isNightMode
    val appliedColorScheme = remember(context, isDark) {
        context.colorSchemeFromAppliedTheme(isDark)
    }
    val colorScheme = if (isDark && amoled) {
        appliedColorScheme.withCalmFocusAmoledSurfaces()
    } else {
        appliedColorScheme
    }

    CompositionLocalProvider(LocalCalmFocusSpacing provides CalmFocusTokens.spacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalmFocusTypography,
            shapes = CalmFocusTokens.shapes,
            content = content,
        )
    }
}

private fun Context.colorSchemeFromAppliedTheme(darkTheme: Boolean): ColorScheme {
    val fallback = if (darkTheme) CalmFocusDarkColorScheme else CalmFocusLightColorScheme
    fun color(@AttrRes attribute: Int, default: Color) = resolveThemeColor(attribute, default)

    return fallback.copy(
        primary = color(MaterialR.attr.colorPrimary, fallback.primary),
        onPrimary = color(MaterialR.attr.colorOnPrimary, fallback.onPrimary),
        primaryContainer = color(MaterialR.attr.colorPrimaryContainer, fallback.primaryContainer),
        onPrimaryContainer = color(MaterialR.attr.colorOnPrimaryContainer, fallback.onPrimaryContainer),
        inversePrimary = color(MaterialR.attr.colorPrimaryInverse, fallback.inversePrimary),
        secondary = color(MaterialR.attr.colorSecondary, fallback.secondary),
        onSecondary = color(MaterialR.attr.colorOnSecondary, fallback.onSecondary),
        secondaryContainer = color(MaterialR.attr.colorSecondaryContainer, fallback.secondaryContainer),
        onSecondaryContainer = color(MaterialR.attr.colorOnSecondaryContainer, fallback.onSecondaryContainer),
        tertiary = color(MaterialR.attr.colorTertiary, fallback.tertiary),
        onTertiary = color(MaterialR.attr.colorOnTertiary, fallback.onTertiary),
        tertiaryContainer = color(MaterialR.attr.colorTertiaryContainer, fallback.tertiaryContainer),
        onTertiaryContainer = color(MaterialR.attr.colorOnTertiaryContainer, fallback.onTertiaryContainer),
        background = color(android.R.attr.colorBackground, fallback.background),
        onBackground = color(MaterialR.attr.colorOnBackground, fallback.onBackground),
        surface = color(MaterialR.attr.colorSurface, fallback.surface),
        onSurface = color(MaterialR.attr.colorOnSurface, fallback.onSurface),
        surfaceVariant = color(MaterialR.attr.colorSurfaceVariant, fallback.surfaceVariant),
        onSurfaceVariant = color(MaterialR.attr.colorOnSurfaceVariant, fallback.onSurfaceVariant),
        surfaceTint = color(MaterialR.attr.colorPrimary, fallback.surfaceTint),
        inverseSurface = color(MaterialR.attr.colorSurfaceInverse, fallback.inverseSurface),
        inverseOnSurface = color(MaterialR.attr.colorOnSurfaceInverse, fallback.inverseOnSurface),
        error = color(MaterialR.attr.colorError, fallback.error),
        onError = color(MaterialR.attr.colorOnError, fallback.onError),
        errorContainer = color(MaterialR.attr.colorErrorContainer, fallback.errorContainer),
        onErrorContainer = color(MaterialR.attr.colorOnErrorContainer, fallback.onErrorContainer),
        outline = color(MaterialR.attr.colorOutline, fallback.outline),
        outlineVariant = color(MaterialR.attr.colorOutlineVariant, fallback.outlineVariant),
        surfaceDim = color(MaterialR.attr.colorSurfaceDim, fallback.surfaceDim),
        surfaceBright = color(MaterialR.attr.colorSurfaceBright, fallback.surfaceBright),
        surfaceContainerLowest = color(MaterialR.attr.colorSurfaceContainerLowest, fallback.surfaceContainerLowest),
        surfaceContainerLow = color(MaterialR.attr.colorSurfaceContainerLow, fallback.surfaceContainerLow),
        surfaceContainer = color(MaterialR.attr.colorSurfaceContainer, fallback.surfaceContainer),
        surfaceContainerHigh = color(MaterialR.attr.colorSurfaceContainerHigh, fallback.surfaceContainerHigh),
        surfaceContainerHighest = color(MaterialR.attr.colorSurfaceContainerHighest, fallback.surfaceContainerHighest),
    )
}

private fun Context.resolveThemeColor(@AttrRes attribute: Int, fallback: Color): Color {
    val value = TypedValue()
    if (!theme.resolveAttribute(attribute, value, true)) return fallback
    val color = if (value.resourceId != 0) {
        ContextCompat.getColor(this, value.resourceId)
    } else {
        value.data
    }
    return Color(color)
}

object CalmFocusDesign {
    val colors
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCalmFocusSpacing.current
}
