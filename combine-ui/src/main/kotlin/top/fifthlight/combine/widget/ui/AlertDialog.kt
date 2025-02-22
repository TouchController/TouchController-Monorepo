package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.pointer.consumePress
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.Dialog
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.touchcontroller.assets.Textures

@Composable
fun AlertDialog(
    onDismissRequest: (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .border(Textures.WIDGET_BACKGROUND_BACKGROUND_LIGHTGRAY)
                .verticalScroll()
                .consumePress(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8)
        ) {
            title()
            content()
            Row(
                modifier = Modifier.alignment(Alignment.Right),
                horizontalArrangement = Arrangement.spacedBy(8),
            ) {
                action()
            }
        }
    }
}