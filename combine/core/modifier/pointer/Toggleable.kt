package top.fifthlight.combine.modifier.pointer

import androidx.compose.runtime.Composable
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier

@Composable
fun Modifier.toggleable(
    interactionSource: MutableInteractionSource? = null,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
) = clickable(interactionSource) {
    onValueChanged(!value)
}
