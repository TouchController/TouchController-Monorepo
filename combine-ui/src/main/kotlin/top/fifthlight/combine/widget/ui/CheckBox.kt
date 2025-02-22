package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.ui.style.TextureSet
import top.fifthlight.touchcontroller.assets.Textures

data class CheckBoxTextureSet(
    val unchecked: TextureSet,
    val checked: TextureSet,
)

val defaultCheckBoxTextureSet = CheckBoxTextureSet(
    unchecked = TextureSet(
        normal = Textures.WIDGET_CHECKBOX_CHECKBOX,
        focus = Textures.WIDGET_CHECKBOX_CHECKBOX_HOVER,
        hover = Textures.WIDGET_CHECKBOX_CHECKBOX_HOVER,
        active = Textures.WIDGET_CHECKBOX_CHECKBOX_ACTIVE,
        disabled = Textures.WIDGET_CHECKBOX_CHECKBOX,
    ),
    checked = TextureSet(
        normal = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED,
        focus = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_HOVER,
        hover = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_HOVER,
        active = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_ACTIVE,
        disabled = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED,
    )
)

val LocalCheckBoxTextureSet = staticCompositionLocalOf<CheckBoxTextureSet> { defaultCheckBoxTextureSet }

@Composable
fun CheckBox(
    modifier: Modifier = Modifier,
    textureSet: CheckBoxTextureSet = LocalCheckBoxTextureSet.current,
    value: Boolean,
    onValueChanged: ((Boolean) -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val currentTextureSet = if (value) {
        textureSet.checked
    } else {
        textureSet.unchecked
    }
    val texture = currentTextureSet.getByState(state)

    val modifier = if (onValueChanged == null) {
        modifier
    } else {
        Modifier
            .clickable(interactionSource) {
                onValueChanged(!value)
            }
            .focusable(interactionSource)
            .then(modifier)
    }

    Icon(
        modifier = modifier,
        texture = texture
    )
}