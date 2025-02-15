package top.fifthlight.combine.ui.style

import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors

val LocalColorTheme = staticCompositionLocalOf<ColorTheme> { ColorTheme.default }

data class ColorTheme(
    val background: Color = Colors.BLACK,
    val border: Color = Colors.WHITE,
    val foreground: Color = Colors.WHITE,
) {
    companion object {
        val default = ColorTheme()
    }
}