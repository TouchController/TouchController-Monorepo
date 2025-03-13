package top.fifthlight.combine.widget.base

import androidx.compose.runtime.Composable
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.screen.DismissHandler
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.BoxScope

@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    DismissHandler {
        onDismissRequest?.invoke()
    }
    Popup(
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier),
            alignment = Alignment.Center,
        ) {
            content()
        }
    }
}