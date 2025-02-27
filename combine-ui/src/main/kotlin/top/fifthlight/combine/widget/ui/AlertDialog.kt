package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.pointer.consumePress
import top.fifthlight.combine.widget.base.Dialog
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.ColumnScope
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.touchcontroller.assets.Textures

@Composable
fun AlertDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    action: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(8)
                .border(Textures.WIDGET_BACKGROUND_BACKGROUND_GRAY)
                .consumePress()
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8)
        ) {
            title()
            content()
            action?.let {
                Row(
                    modifier = Modifier.alignment(Alignment.Right),
                    horizontalArrangement = Arrangement.spacedBy(8),
                ) {
                    action()
                }
            }
        }
    }
}