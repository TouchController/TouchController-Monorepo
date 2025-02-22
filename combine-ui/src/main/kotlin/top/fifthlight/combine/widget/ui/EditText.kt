package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.input.input.LocalClipboard
import top.fifthlight.combine.input.input.TextInputState
import top.fifthlight.combine.input.key.Key
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.FocusInteraction
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.input.textInput
import top.fifthlight.combine.modifier.key.onKeyEvent
import top.fifthlight.combine.modifier.placement.minHeight
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.node.LocalTextMeasurer
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

val defaultEditTextTexture = NinePatchTextureSet(
    normal = Textures.WIDGET_TEXTFIELD_TEXTFIELD,
    focus = Textures.WIDGET_TEXTFIELD_TEXTFIELD_HOVER,
    hover = Textures.WIDGET_TEXTFIELD_TEXTFIELD_HOVER,
    active = Textures.WIDGET_TEXTFIELD_TEXTFIELD_ACTIVE,
    disabled = Textures.WIDGET_TEXTFIELD_TEXTFIELD_DISABLED,
)

val LocalEditTextTextureSet = staticCompositionLocalOf<NinePatchTextureSet> { defaultEditTextTexture }

@Composable
fun EditText(
    modifier: Modifier = Modifier,
    textureSet: NinePatchTextureSet = LocalEditTextTextureSet.current,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: Text? = null,
) {
    val clipboard = LocalClipboard.current
    val textMeasurer = LocalTextMeasurer.current
    var textInputState by remember { mutableStateOf(TextInputState(value)) }

    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    var cursorShow by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            when (it) {
                FocusInteraction.Blur -> focused = false
                FocusInteraction.Focus -> focused = true
            }
        }
    }
    LaunchedEffect(value) {
        if (value == textInputState.text) {
            return@LaunchedEffect
        }
        textInputState = TextInputState(value)
    }
    LaunchedEffect(focused) {
        if (focused) {
            while (true) {
                cursorShow = !cursorShow
                delay(500)
            }
        } else {
            cursorShow = false
        }
    }

    fun updateInputState(block: TextInputState.() -> TextInputState) {
        textInputState = block(textInputState)
        onValueChanged(textInputState.text)
    }

    val state by widgetState(interactionSource)
    val texture = textureSet.getByState(state)

    Canvas(
        modifier = Modifier
            .minHeight(9)
            .border(texture)
            .clickable(interactionSource) {}
            .focusable(interactionSource)
            .textInput { updateInputState { commitText(it) } }
            .onKeyEvent { event ->
                if (!event.pressed) {
                    return@onKeyEvent
                }
                when (event.key) {
                    Key.DELETE -> updateInputState { doDelete() }
                    Key.BACKSPACE -> updateInputState { doBackspace() }

                    Key.HOME -> if (event.modifier.onlyShift) {
                        updateInputState { doShiftHome() }
                    } else if (event.modifier.empty) {
                        updateInputState { doHome() }
                    }

                    Key.END -> if (event.modifier.onlyShift) {
                        updateInputState { doShiftEnd() }
                    } else if (event.modifier.empty) {
                        updateInputState { doEnd() }
                    }

                    Key.ARROW_LEFT -> if (event.modifier.onlyShift) {
                        updateInputState { doShiftLeft() }
                    } else if (event.modifier.empty) {
                        updateInputState { doArrowLeft() }
                    }

                    Key.ARROW_RIGHT -> if (event.modifier.onlyShift) {
                        updateInputState { doShiftRight() }
                    } else if (event.modifier.empty) {
                        updateInputState { doArrowRight() }
                    }

                    Key.C -> if (event.modifier.onlyControl) {
                        val selectionText = textInputState.selectionText
                        clipboard.text = selectionText
                    }

                    Key.V -> if (event.modifier.onlyControl) {
                        updateInputState { commitText(clipboard.text) }
                    }

                    Key.X -> if (event.modifier.onlyControl) {
                        val selectionText = textInputState.selectionText
                        clipboard.text = selectionText
                        updateInputState { removeSelection() }
                    }

                    else -> {}
                }
            }
            .then(modifier),
        measurePolicy = { _, constraints ->
            val textSize = textMeasurer.measure(value, constraints.maxHeight)
            layout(
                width = textSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                height = textSize.height.coerceIn(constraints.minHeight, constraints.maxHeight),
            ) {}
        }
    ) { node ->
        with(canvas) {
            val textWidth = node.width
            if (value.isEmpty() && !focused) {
                if (placeholder != null) {
                    drawText(
                        offset = IntOffset.ZERO,
                        width = textWidth,
                        text = placeholder,
                        color = Colors.LIGHT_GRAY
                    )
                }
            } else {
                // TODO handle composition region
                var textCursor = 0
                val beforeSelectionText = textInputState.text.substring(0, textInputState.selection.start)
                drawText(
                    offset = IntOffset.ZERO,
                    text = beforeSelectionText,
                    color = Colors.WHITE
                )
                textCursor += textMeasurer.measure(beforeSelectionText).width

                val selectionText = textInputState.selectionText
                val selectionWidth = textMeasurer.measure(selectionText).width
                val selectionOffset = IntOffset(textCursor, 0)
                fillRect(
                    offset = selectionOffset,
                    size = IntSize(selectionWidth, 9),
                    color = Colors.GRAY,
                )
                drawText(
                    offset = selectionOffset,
                    text = selectionText,
                    color = Colors.WHITE
                )

                if (cursorShow && textInputState.selectionLeft) {
                    fillRect(
                        offset = IntOffset(textCursor, 0),
                        size = IntSize(1, 9),
                        color = Colors.WHITE,
                    )
                }

                textCursor += selectionWidth

                if (cursorShow && !textInputState.selectionLeft) {
                    fillRect(
                        offset = IntOffset(textCursor, 0),
                        size = IntSize(1, 9),
                        color = Colors.WHITE,
                    )
                }

                val afterSelectionText = textInputState.text.substring(textInputState.selection.end)
                drawText(
                    offset = IntOffset(textCursor, 0),
                    text = afterSelectionText,
                    color = Colors.WHITE
                )
            }
        }
    }
}