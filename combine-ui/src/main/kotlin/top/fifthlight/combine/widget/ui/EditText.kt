package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.input.input.LocalClipboard
import top.fifthlight.combine.input.input.TextInputState
import top.fifthlight.combine.input.input.TextRange
import top.fifthlight.combine.input.input.substring
import top.fifthlight.combine.input.key.Key
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.FocusInteraction
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.input.textInput
import top.fifthlight.combine.modifier.key.onKeyEvent
import top.fifthlight.combine.modifier.placement.minHeight
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.node.LocalInputHandler
import top.fifthlight.combine.node.LocalTextMeasurer
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

val defaultEditTextDrawable = DrawableSet(
    normal = Textures.WIDGET_TEXTFIELD_TEXTFIELD,
    focus = Textures.WIDGET_TEXTFIELD_TEXTFIELD_HOVER,
    hover = Textures.WIDGET_TEXTFIELD_TEXTFIELD_HOVER,
    active = Textures.WIDGET_TEXTFIELD_TEXTFIELD_ACTIVE,
    disabled = Textures.WIDGET_TEXTFIELD_TEXTFIELD_DISABLED,
)

val LocalEditTextDrawableSet = staticCompositionLocalOf { defaultEditTextDrawable }

@Composable
fun EditText(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    drawableSet: DrawableSet = LocalEditTextDrawableSet.current,
    value: String,
    onValueChanged: (String) -> Unit,
    onEnter: (() -> Unit)? = {},
    placeholder: Text? = null,
) {
    val clipboard = LocalClipboard.current
    val textMeasurer = LocalTextMeasurer.current
    val textFactory = LocalTextFactory.current
    val inputManager = LocalInputHandler.current
    var textInputState by remember { mutableStateOf(TextInputState(value)) }

    fun updateInputState(block: TextInputState.() -> TextInputState) {
        textInputState = block(textInputState)
        if (value != textInputState.text) {
            onValueChanged(textInputState.text)
        }
    }

    var focused by remember { mutableStateOf(false) }
    var cursorShow by remember { mutableStateOf(false) }
    var cursorRect by remember { mutableStateOf<IntRect?>(null) }
    var areaRect by remember { mutableStateOf<IntRect?>(null) }
    LaunchedEffect(interactionSource) {
        try {
            interactionSource.interactions.collect {
                when (it) {
                    FocusInteraction.Blur -> {
                        inputManager.tryHideKeyboard()
                        focused = false
                    }

                    FocusInteraction.Focus -> {
                        inputManager.tryShowKeyboard()
                        focused = true
                    }
                }
            }
        } finally {
            inputManager.updateInputState(null)
            inputManager.tryHideKeyboard()
        }
    }
    LaunchedEffect(textInputState, focused, cursorRect, areaRect) {
        if (focused) {
            inputManager.updateInputState(textInputState, cursorRect, areaRect)
        } else {
            inputManager.updateInputState(null)
        }
    }
    LaunchedEffect(focused) {
        if (focused) {
            inputManager.events.collect { newState ->
                updateInputState { newState }
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

    val state by widgetState(interactionSource)
    val drawable = drawableSet.getByState(state)

    Canvas(
        modifier = Modifier
            .minHeight(9)
            .border(drawable)
            .clickable(interactionSource) {
                inputManager.tryShowKeyboard()
            }
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

                    Key.ENTER -> {
                        if (onEnter == null) {
                            updateInputState { commitText("\n") }
                        } else {
                            onEnter()
                        }
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
        areaRect = IntRect(offset = node.absolutePosition, size = node.size)
        if (value.isEmpty() && !focused) {
            val textSize = textMeasurer.measure(value)
            val offsetY = (node.height - textSize.height) / 2
            if (placeholder != null) {
                drawText(
                    offset = IntOffset(0, offsetY),
                    width = node.width,
                    text = placeholder,
                    color = Colors.LIGHT_GRAY
                )
                cursorRect = IntRect(node.absolutePosition, IntSize(1, 9))
            }
        } else {
            val fullText = textInputState.text
            val textSize = textMeasurer.measure(fullText)
            val offsetY = (node.height - textSize.height) / 2

            val selectionStartX = textMeasurer.measure(fullText.substring(0, textInputState.selection.start)).width
            val selectionWidth = textMeasurer.measure(textInputState.selectionText).width

            if (selectionWidth > 0) {
                fillRect(
                    offset = IntOffset(selectionStartX, offsetY),
                    size = IntSize(selectionWidth, textSize.height),
                    color = Colors.GRAY,
                )
            }

            // 计算光标位置
            val cursorX = if (textInputState.selectionLeft) {
                selectionStartX
            } else {
                selectionStartX + selectionWidth
            }

            // 记录光标矩形（用于输入法）
            cursorRect = IntRect(
                IntOffset(cursorX, offsetY) + node.absolutePosition,
                IntSize(1, textSize.height)
            )

            // 绘制光标（如果需要）
            if (cursorShow) {
                fillRect(
                    offset = IntOffset(cursorX, offsetY),
                    size = IntSize(1, textSize.height),
                    color = Colors.WHITE,
                )
            }

            // 构建富文本（处理预编辑文本的下划线）
            val styledText = textFactory.build {
                if (textInputState.composition != TextRange.EMPTY) {
                    val beforeComposition = fullText.substring(0, textInputState.composition.start)
                    val compositionText = fullText.substring(textInputState.composition)
                    val afterComposition = fullText.substring(textInputState.composition.end)
                    append(beforeComposition)
                    underline { append(compositionText) }
                    append(afterComposition)
                } else {
                    append(fullText)
                }
            }

            // 绘制整个富文本
            drawText(
                offset = IntOffset(0, offsetY),
                width = node.width,
                text = styledText,
                color = Colors.WHITE
            )
        }
    }
}