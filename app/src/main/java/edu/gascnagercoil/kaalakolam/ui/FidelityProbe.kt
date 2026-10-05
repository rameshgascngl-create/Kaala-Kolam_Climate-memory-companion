package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult

/**
 * Test-only fidelity recorder hook. Production renders keep only stable
 * semantics testTags; recording is a no-op unless a JVM fidelity test provides
 * a recorder through LocalFidelityRecorder.
 */
internal interface FidelityRecorder {
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
