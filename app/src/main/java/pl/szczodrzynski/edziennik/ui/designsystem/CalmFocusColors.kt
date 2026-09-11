package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CalmFocusSeed = Color(0xFF63A002)

private val LightPrimary = Color(0xFF3F6900)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFB8F474)
private val LightOnPrimaryContainer = Color(0xFF102000)
private val LightSecondary = Color(0xFF56624B)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFDAE7C9)
private val LightOnSecondaryContainer = Color(0xFF141E0D)
private val LightTertiary = Color(0xFF386666)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFBCEBEC)
private val LightOnTertiaryContainer = Color(0xFF002021)
private val LightBackground = Color(0xFFF9FAEF)
private val LightOnBackground = Color(0xFF1A1C16)
private val LightSurface = Color(0xFFF9FAEF)
private val LightOnSurface = Color(0xFF1A1C16)
private val LightSurfaceVariant = Color(0xFFE1E4D6)
private val LightOnSurfaceVariant = Color(0xFF45483E)
private val LightOutline = Color(0xFF75786D)
private val LightOutlineVariant = Color(0xFFC5C8BA)

private val DarkPrimary = Color(0xFF97D945)
private val DarkOnPrimary = Color(0xFF1D3700)
private val DarkPrimaryContainer = Color(0xFF2E5000)
private val DarkOnPrimaryContainer = Color(0xFFB8F474)
private val DarkSecondary = Color(0xFFBECBAE)
private val DarkOnSecondary = Color(0xFF293420)
private val DarkSecondaryContainer = Color(0xFF3F4A35)
private val DarkOnSecondaryContainer = Color(0xFFDAE7C9)
private val DarkTertiary = Color(0xFFA0CFCF)
private val DarkOnTertiary = Color(0xFF003737)
private val DarkTertiaryContainer = Color(0xFF1E4E4E)
private val DarkOnTertiaryContainer = Color(0xFFBCEBEC)
private val DarkBackground = Color(0xFF11140D)
private val DarkOnBackground = Color(0xFFE2E3D8)
private val DarkSurface = Color(0xFF11140D)
private val DarkOnSurface = Color(0xFFE2E3D8)
private val DarkSurfaceVariant = Color(0xFF45483E)
private val DarkOnSurfaceVariant = Color(0xFFC5C8BA)
private val DarkOutline = Color(0xFF8F9285)
private val DarkOutlineVariant = Color(0xFF45483E)

val CalmFocusLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

val CalmFocusDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

internal fun ColorScheme.withCalmFocusAmoledSurfaces() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
)
