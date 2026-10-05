package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/**
 * Test-only fidelity recorder hook. Production renders keep only stable
 * semantics testTags; recording is a no-op unless a JVM fidelity test provides
 * a recorder through LocalFidelityRecorder.
 */
interface FidelityRecorder {
    fun recordBox(
        tag: String,
        leftDp: Float,
        topDp: Float,
        widthDp: Float,
        heightDp: Float,
        interactive: Boolean,
    )

    fun recordText(
        tag: String,
        kind: String,
        text: String,
        lineCount: Int,
        didOverflowWidth: Boolean,
        didOverflowHeight: Boolean,
    )
}

internal val LocalFidelityRecorder = staticCompositionLocalOf<FidelityRecorder?> { null }

internal fun Modifier.fidelityTag(
    tag: String,
    interactive: Boolean = false,
): Modifier = composed {
    val recorder = LocalFidelityRecorder.current
    val density = LocalDensity.current.density
    this
        .semantics { testTag = tag }
        .onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInRoot()
            recorder?.recordBox(
                tag = tag,
                leftDp = bounds.left / density,
                topDp = bounds.top / density,
                widthDp = bounds.width / density,
                heightDp = bounds.height / density,
                interactive = interactive,
            )
        }
}

@Composable
internal fun fidelityTextLayout(
    tag: String,
    kind: String,
    text: String,
    delegate: (TextLayoutResult) -> Unit = {},
): (TextLayoutResult) -> Unit {
    val recorder = LocalFidelityRecorder.current
    return { result ->
        recorder?.recordText(
            tag = tag,
            kind = kind,
            text = text,
            lineCount = result.lineCount,
            didOverflowWidth = result.didOverflowWidth,
            didOverflowHeight = result.didOverflowHeight,
        )
        delegate(result)
    }
}


/**
 * Expands the semantics/click target to at least 48 dp while measuring and
 * drawing the single visual child at its prototype size (44/46 dp where used).
 */
@Composable
internal fun FidelityTouchTarget(
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val minTouchPx = with(LocalDensity.current) { 48.dp.roundToPx() }
    Layout(
        content = content,
        modifier = modifier
            .fidelityTag(tag, interactive = true)
            .clickable(enabled = enabled, onClick = onClick),
    ) { measurables, constraints ->
        require(measurables.size == 1) { "FidelityTouchTarget requires one visual child" }
        val visualConstraints = constraints.copy(minHeight = 0)
        val placeable = measurables.single().measure(visualConstraints)
        val width = constraints.constrainWidth(maxOf(placeable.width, minTouchPx))
        val height = constraints.constrainHeight(maxOf(placeable.height, minTouchPx))
        layout(width, height) {
            placeable.placeRelative(
                x = (width - placeable.width) / 2,
                y = (height - placeable.height) / 2,
            )
        }
    }
}
