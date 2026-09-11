package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun CalmFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = if (darkTheme) {
        CalmFocusDarkColorScheme
    } else {
        CalmFocusLightColorScheme
    }
    val colorScheme = if (darkTheme && amoled) {
        baseColorScheme.withCalmFocusAmoledSurfaces()
    } else {
        baseColorScheme
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
