package top.fifthlight.combine.ui.style

import androidx.compose.runtime.staticCompositionLocalOf

val LocalTextStyle = staticCompositionLocalOf<TextStyle> { TextStyle.default }

data class TextStyle(
    val shadow: Boolean = false,
) {
    companion object {
        val default = TextStyle()
    }
}