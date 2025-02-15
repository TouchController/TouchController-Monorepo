package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.size
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.ui.style.TextureSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.Offset
import top.fifthlight.data.Rect
import top.fifthlight.touchcontroller.assets.Textures

data class SwitchTextureSet(
    val off: TextureSet,
    val on: TextureSet,
)

val defaultSwitchTexture = SwitchTextureSet(
    off = TextureSet(
        normal = Textures.GUI_WIDGET_SWITCH_SWITCH_OFF,
        focus = Textures.GUI_WIDGET_SWITCH_SWITCH_OFF_HOVER,
        hover = Textures.GUI_WIDGET_SWITCH_SWITCH_OFF_HOVER,
        active = Textures.GUI_WIDGET_SWITCH_SWITCH_OFF_ACTIVE,
        disabled = Textures.GUI_WIDGET_SWITCH_SWITCH_OFF_DISABLED,
    ),
    on = TextureSet(
        normal = Textures.GUI_WIDGET_SWITCH_SWITCH_ON,
        focus = Textures.GUI_WIDGET_SWITCH_SWITCH_ON_HOVER,
        hover = Textures.GUI_WIDGET_SWITCH_SWITCH_ON_HOVER,
        active = Textures.GUI_WIDGET_SWITCH_SWITCH_ON_ACTIVE,
        disabled = Textures.GUI_WIDGET_SWITCH_SWITCH_ON_DISABLED,
    ),
)

val LocalSwitchTexture = staticCompositionLocalOf<SwitchTextureSet> { defaultSwitchTexture }

@Composable
fun Switch(
    modifier: Modifier = Modifier,
    textureSet: SwitchTextureSet = LocalSwitchTexture.current,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    clickSound: Boolean = true,
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val texture = if (checked) {
        textureSet.on.getByState(state)
    } else {
        textureSet.off.getByState(state)
    }

    Canvas(
        modifier = Modifier
            .size(texture.size)
            .focusable(interactionSource)
            .clickable(interactionSource) {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onChanged(!checked)
            }
            .then(modifier),
    ) {
        canvas.drawTexture(
            texture,
            dstRect = Rect(
                offset = Offset.ZERO,
                size = texture.size.toSize(),
            ),
        )
    }
}