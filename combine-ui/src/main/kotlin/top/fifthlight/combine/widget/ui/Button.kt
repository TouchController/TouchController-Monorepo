package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.minSize
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.ui.style.TextStyle
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.touchcontroller.assets.Textures

val defaultButtonTexture = NinePatchTextureSet(
    normal = Textures.WIDGET_BUTTON_BUTTON,
    focus = Textures.WIDGET_BUTTON_BUTTON_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_HOVER,
    active = Textures.WIDGET_BUTTON_BUTTON_ACTIVE,
    disabled = Textures.WIDGET_BUTTON_BUTTON_DISABLED,
)

val guideButtonTexture = defaultButtonTexture.copy(
    normal = Textures.WIDGET_BUTTON_BUTTON_GUIDE,
    focus = Textures.WIDGET_BUTTON_BUTTON_GUIDE_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_GUIDE_HOVER,
)

val warningButtonTexture = defaultButtonTexture.copy(
    normal = Textures.WIDGET_BUTTON_BUTTON_WARNING,
    focus = Textures.WIDGET_BUTTON_BUTTON_WARNING_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_WARNING_HOVER,
)

val LocalButtonTexture = staticCompositionLocalOf<NinePatchTextureSet> { defaultButtonTexture }

@NonSkippableComposable
@Composable
fun GuideButton(
    modifier: Modifier = Modifier,
    textureSet: NinePatchTextureSet = guideButtonTexture,
    colorTheme: ColorTheme? = ColorTheme.dark,
    textStyle: TextStyle? = null,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Button(
        modifier = modifier,
        textureSet = textureSet,
        colorTheme = colorTheme,
        textStyle = textStyle,
        onClick = onClick,
        clickSound = clickSound,
        content = content
    )
}

@NonSkippableComposable
@Composable
fun WarningButton(
    modifier: Modifier = Modifier,
    textureSet: NinePatchTextureSet = warningButtonTexture,
    colorTheme: ColorTheme? = ColorTheme.dark,
    textStyle: TextStyle? = null,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Button(
        modifier = modifier,
        textureSet = textureSet,
        colorTheme = colorTheme,
        textStyle = textStyle,
        onClick = onClick,
        clickSound = clickSound,
        content = content
    )
}

@Composable
fun Button(
    modifier: Modifier = Modifier,
    textureSet: NinePatchTextureSet = LocalButtonTexture.current,
    colorTheme: ColorTheme? = null,
    textStyle: TextStyle? = null,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val texture = textureSet.getByState(state)

    Box(
        modifier = Modifier
            .border(texture)
            .minSize(48, 20)
            .clickable(interactionSource) {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onClick()
            }
            .focusable(interactionSource)
            .then(modifier),
        alignment = Alignment.Center,
    ) {
        val colorTheme = colorTheme ?: ColorTheme.light
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
        ) {
            content()
        }
    }
}
