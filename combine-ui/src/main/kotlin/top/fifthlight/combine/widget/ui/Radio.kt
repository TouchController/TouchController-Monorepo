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

data class RadioTextureSet(
    val unchecked: TextureSet,
    val checked: TextureSet,
)

val defaultRadioTextureSet = RadioTextureSet(
    unchecked = TextureSet(
        normal = Textures.GUI_WIDGET_RADIO_RADIO,
        focus = Textures.GUI_WIDGET_RADIO_RADIO_HOVER,
        hover = Textures.GUI_WIDGET_RADIO_RADIO_HOVER,
        active = Textures.GUI_WIDGET_RADIO_RADIO_ACTIVE,
        disabled = Textures.GUI_WIDGET_RADIO_RADIO,
    ),
    checked = TextureSet(
        normal = Textures.GUI_WIDGET_RADIO_RADIO_CHECKED,
        focus = Textures.GUI_WIDGET_RADIO_RADIO_CHECKED_HOVER,
        hover = Textures.GUI_WIDGET_RADIO_RADIO_CHECKED_HOVER,
        active = Textures.GUI_WIDGET_RADIO_RADIO_CHECKED_ACTIVE,
        disabled = Textures.GUI_WIDGET_RADIO_RADIO_CHECKED,
    )
)

val LocalRadioTextureSet = staticCompositionLocalOf<RadioTextureSet> { defaultRadioTextureSet }

@Composable
fun Radio(
    modifier: Modifier = Modifier,
    textureSet: RadioTextureSet = LocalRadioTextureSet.current,
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