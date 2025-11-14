package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.minHeight
import top.fifthlight.combine.modifier.placement.minSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.ui.style.ColorSet
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.layout.Box
import top.fifthlight.combine.widget.layout.BoxScope
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize

val defaultTextButtonColorSet = ColorSet(
    normal = Colors.TRANSPARENT,
    focus = Color(0x55FFFFFFu),
    hover = Color(0x55FFFFFFu),
    active = Color(0xFF228207u),
    disabled = Color(0xFF323335u),
)

val LocalTextButtonColorSet = staticCompositionLocalOf<ColorSet> { defaultTextButtonColorSet }

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    colorSet: ColorSet = LocalTextButtonColorSet.current,
    colorTheme: ColorTheme? = null,
    padding: IntPadding = IntPadding(left = 8, right = 8, top = 1),
    minSize: IntSize = IntSize(width = 0, height = 20),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val color = colorSet.getByState(state, enabled = enabled)

    Box(
        modifier = Modifier
            .padding(padding)
            .minSize(minSize)
            .background(color)
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
        val colorTheme = colorTheme ?: ColorTheme.dark
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
        ) {
            content()
        }
    }
}
