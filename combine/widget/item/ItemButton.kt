package top.fifthlight.combine.widget.ui.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.item.ItemStack
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.widget.ui.WidgetState
import top.fifthlight.combine.widget.ui.widgetState

@Composable
fun ItemButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    itemStack: ItemStack,
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)

    Item(
        modifier = Modifier
            .background(
                when (state) {
                    WidgetState.NORMAL, WidgetState.FOCUS -> Colors.TRANSPARENT
                    WidgetState.HOVER, WidgetState.ACTIVE -> Colors.TRANSPARENT_WHITE
                }
            )
            .clickable(interactionSource) {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onClick()
            }
            .then(modifier),
        itemStack = itemStack,
    )
}
