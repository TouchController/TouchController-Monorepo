package top.fifthlight.combine.theme

import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.paint.BackgroundTexture
import top.fifthlight.combine.paint.Drawable
import top.fifthlight.combine.paint.Texture
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.ui.style.TextureSet

val LocalTheme = staticCompositionLocalOf { SimpleTheme }

data class Theme(
    val drawables: Drawables = Drawables(),
) {
    data class Drawables(
        val button: DrawableSet = DrawableSet.Empty,
        val guideButton: DrawableSet = button,
        val warningButton: DrawableSet = button,

        val alertDialogBackground: Drawable = Drawable.Empty,

        val uncheckedCheckBox: DrawableSet = DrawableSet.Empty,
        val checkboxChecked: DrawableSet = uncheckedCheckBox,

        val checkBoxButton: DrawableSet = DrawableSet.Empty,

        val colorPickerHandleChoice: Drawable = Drawable.Empty,
        val colorPickerSliderHandleHollow: DrawableSet = DrawableSet.Empty,

        val sliderActiveTrack: DrawableSet = DrawableSet.Empty,
        val sliderInactiveTrack: DrawableSet = DrawableSet.Empty,
        val sliderHandle: DrawableSet = DrawableSet.Empty,

        val switchFrame: DrawableSet = DrawableSet.Empty,
        val switchBackground: TextureSet = TextureSet.Empty,

        val editText: DrawableSet = DrawableSet.Empty,

        val iconButton: DrawableSet = DrawableSet.Empty,
        val selectedIconButton: DrawableSet = iconButton,

        val selectMenuBox: DrawableSet = DrawableSet.Empty,
        val selectFloatPanel: Drawable = Drawable.Empty,
        val selectItemUnselected: DrawableSet = DrawableSet.Empty,
        val selectItemSelected: DrawableSet = DrawableSet.Empty,

        val selectIconUp: Drawable = Drawable.Empty,
        val selectIconDown: Drawable = Drawable.Empty,

        val radioUnchecked: DrawableSet = DrawableSet.Empty,
        val radioChecked: DrawableSet = DrawableSet.Empty,

        val radioBoxBorder: Drawable = Drawable.Empty,

        val tab: DrawableSet = DrawableSet.Empty,

        val itemGridBackground: BackgroundTexture? = null,
    )
}