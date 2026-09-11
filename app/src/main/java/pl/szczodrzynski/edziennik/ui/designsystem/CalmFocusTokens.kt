package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CalmFocusSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val screenHorizontal: Dp = 20.dp,
)

val LocalCalmFocusSpacing = staticCompositionLocalOf { CalmFocusSpacing() }

object CalmFocusTokens {
    val spacing = CalmFocusSpacing()

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

    object Elevation {
        val flat = 0.dp
        val raised = 2.dp
        val floating = 6.dp
    }
}
