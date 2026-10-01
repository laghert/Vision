package pl.szczodrzynski.edziennik.ui.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope

object CalmFocusMotion {
    // Apple Fluid Motion specs:
    // Damping ratio 1.0 (critically damped) prevents unnatural oscillation/jitter.
    // Stiffness 350-400f delivers a snappy ~300ms response.
    const val dampingRatio = Spring.DampingRatioNoBouncy
    const val stiffness = 380f

    // Momentum / flick gestures carry slight elasticity
    const val momentumDampingRatio = 0.82f
    const val momentumStiffness = 320f

    fun <T> springSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold,
    )

    fun <T> momentumSpringSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = momentumDampingRatio,
        stiffness = momentumStiffness,
        visibilityThreshold = visibilityThreshold,
    )

    // Apple-like page enter: subtle scale up from 0.95 + slight slide from offset + smooth fade-in
    val enterTransition: androidx.compose.animation.EnterTransition
        get() = fadeIn(
            animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
        ) + androidx.compose.animation.scaleIn(
            initialScale = 0.96f,
            animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
        ) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { fullHeight -> (fullHeight * 0.035f).toInt() },
            animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness)
        )

    // Apple-like page exit: subtle scale down to 0.98 + smooth fade-out (materializing depth)
    val exitTransition: androidx.compose.animation.ExitTransition
        get() = fadeOut(
            animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness * 1.2f)
        ) + androidx.compose.animation.scaleOut(
            targetScale = 0.98f,
            animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness * 1.2f)
        )

    val contentTransform: ContentTransform
        get() = enterTransition togetherWith exitTransition
}

@Composable
fun <S> CalmFocusAnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    label: String = "CalmFocusAnimatedContent",
    contentKey: (S) -> Any? = { it },
    content: @Composable AnimatedContentScope.(S) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { CalmFocusMotion.contentTransform },
        label = label,
        contentKey = contentKey,
        content = content,
    )
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalmFocusLookaheadScope(
    content: @Composable LookaheadScope.() -> Unit,
) {
    LookaheadScope(content = content)
}
