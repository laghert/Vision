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
    const val dampingRatio = Spring.DampingRatioLowBouncy
    const val stiffness = Spring.StiffnessMediumLow

    fun <T> springSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold,
    )

    val contentTransform: ContentTransform
        get() = fadeIn(springSpec()) togetherWith fadeOut(springSpec())
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
