package top.fifthlight.combine.input.text

import kotlinx.coroutines.flow.SharedFlow
import top.fifthlight.data.IntRect

interface InputHandler {
    val events: SharedFlow<TextInputState>
    fun updateInputState(textInputState: TextInputState?, cursorRect: IntRect? = null, areaRect: IntRect? = null)
    fun tryShowKeyboard()
    fun tryHideKeyboard()
}
