package top.fifthlight.combine.widget

import androidx.compose.runtime.*
import top.fifthlight.combine.node.LocalCombineOwner

@Composable
fun Popup(
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val owner = LocalCombineOwner.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(owner, parentComposition, onDismissRequest, currentContent) {
        val layer = owner.addLayer(
            parentContext = parentComposition,
            onDismissRequest = onDismissRequest,
            content = currentContent,
        )
        onDispose {
            layer.dispose()
        }
    }
}
