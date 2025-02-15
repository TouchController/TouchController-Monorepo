package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.minSize
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.ui.style.LocalTextStyle
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.touchcontroller.assets.Textures

val defaultButtonTexture = NinePatchTextureSet(
    normal = Textures.GUI_WIDGET_BUTTON_BUTTON,
    focus = Textures.GUI_WIDGET_BUTTON_BUTTON_HOVER,
    hover = Textures.GUI_WIDGET_BUTTON_BUTTON_HOVER,
    active = Textures.GUI_WIDGET_BUTTON_BUTTON_ACTIVE,
    disabled = Textures.GUI_WIDGET_BUTTON_BUTTON_DISABLED,
)

val LocalButtonTexture = staticCompositionLocalOf<NinePatchTextureSet> { defaultButtonTexture }

@Composable
fun Button(
    modifier: Modifier = Modifier,
    textureSet: NinePatchTextureSet = LocalButtonTexture.current,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val texture = textureSet.getByState(state)

    Box(
        modifier = Modifier
            .border(texture)
            .minSize(48, 20)
            .focusable(interactionSource)
            .clickable(interactionSource) {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onClick()
            }
            .then(modifier),
        alignment = Alignment.Center,
    ) {
        val colorTheme = LocalColorTheme.current.copy(
            background = Colors.WHITE,
            foreground = Colors.BLACK,
        )
        val textStyle = LocalTextStyle.current.copy(
            //shadow = true,
        )
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
            LocalTextStyle provides textStyle,
        ) {
            content()
        }
    }
}