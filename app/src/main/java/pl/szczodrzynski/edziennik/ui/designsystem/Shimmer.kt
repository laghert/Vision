package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache

fun Modifier.calmFocusShimmer(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "CalmFocusShimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250),
            repeatMode = RepeatMode.Restart,
        ),
        label = "CalmFocusShimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.primaryContainer

    drawWithCache {
        val startX = size.width * (progress - 0.35f)
        val endX = size.width * (progress + 0.35f)
        val shimmer = Brush.linearGradient(
            colors = listOf(
                base.copy(alpha = 0.35f),
                highlight.copy(alpha = 0.72f),
                base.copy(alpha = 0.35f),
            ),
            start = Offset(startX, 0f),
            end = Offset(endX, size.height),
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = shimmer)
        }
    }
}
