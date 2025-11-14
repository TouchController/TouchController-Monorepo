package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.defaultButtonDrawable
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

data class CheckButtonDrawables(
    val unchecked: DrawableSet,
    val checked: DrawableSet,
)

data class CheckButtonColors(
    val unchecked: ColorTheme,
    val checked: ColorTheme,
)

val tabButtonDrawable = CheckButtonDrawables(
    unchecked = DrawableSet(
        normal = Textures.WIDGET_TAB_TAB,
        focus = Textures.WIDGET_TAB_TAB_HOVER,
        hover = Textures.WIDGET_TAB_TAB_HOVER,
        active = Textures.WIDGET_TAB_TAB_ACTIVE,
        disabled = Textures.WIDGET_TAB_TAB_DISABLED,
    ),
    checked = DrawableSet(
        normal = Textures.WIDGET_TAB_TAB_PRESSLOCK,
        focus = Textures.WIDGET_TAB_TAB_PRESSLOCK_HOVER,
        hover = Textures.WIDGET_TAB_TAB_PRESSLOCK_HOVER,
        active = Textures.WIDGET_TAB_TAB_ACTIVE,
        disabled = Textures.WIDGET_TAB_TAB_DISABLED,
    ),
)

val tabButtonColors = CheckButtonColors(
    unchecked = ColorTheme.dark,
    checked = ColorTheme.light,
)

val listButtonDrawable = CheckButtonDrawables(
    unchecked = DrawableSet(
        normal = Textures.WIDGET_LIST_LIST,
        focus = Textures.WIDGET_LIST_LIST_HOVER,
        hover = Textures.WIDGET_LIST_LIST_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    ),
    checked = DrawableSet(
        normal = Textures.WIDGET_LIST_LIST_PRESSLOCK,
        focus = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        hover = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    ),
)

val listButtonColors = CheckButtonColors(
    unchecked = ColorTheme.dark,
    checked = ColorTheme.light,
)

val checkButtonDrawable = CheckButtonDrawables(
    unchecked = defaultButtonDrawable,
    checked = defaultButtonDrawable.copy(
        normal = Textures.WIDGET_BUTTON_BUTTON_PRESSLOCK,
        hover = Textures.WIDGET_BUTTON_BUTTON_PRESSLOCK_HOVER,
        focus = Textures.WIDGET_BUTTON_BUTTON_PRESSLOCK_HOVER,
        active = Textures.WIDGET_BUTTON_BUTTON_PRESSLOCK_ACTIVE,
    ),
)

val checkButtonColors = CheckButtonColors(
    unchecked = ColorTheme.light,
    checked = ColorTheme.light,
)

val LocalTabButtonDrawable = staticCompositionLocalOf { tabButtonDrawable }
val LocalTabButtonColors = staticCompositionLocalOf { tabButtonColors }
val LocalListButtonDrawable = staticCompositionLocalOf { listButtonDrawable }
val LocalListButtonColors = staticCompositionLocalOf { listButtonColors }
val LocalCheckButtonDrawable = staticCompositionLocalOf { checkButtonDrawable }
val LocalCheckButtonColors = staticCompositionLocalOf { checkButtonColors }

@Composable
fun TabButton(
    modifier: Modifier = Modifier,
    drawableSet: CheckButtonDrawables = LocalTabButtonDrawable.current,
    colors: CheckButtonColors = LocalTabButtonColors.current,
    checked: Boolean = false,
    minSize: IntSize = IntSize(48, 20),
    padding: IntPadding = IntPadding(left = 4, right = 4, top = 1),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) = CheckButton(
    modifier = modifier,
    drawableSet = drawableSet,
    colors = colors,
    checked = checked,
    padding = padding,
    enabled = enabled,
    minSize = minSize,
    onClick = onClick,
    clickSound = clickSound,
    content = content,
)

@Composable
fun ListButton(
    modifier: Modifier = Modifier,
    drawableSet: CheckButtonDrawables = LocalListButtonDrawable.current,
    colors: CheckButtonColors = LocalListButtonColors.current,
    checked: Boolean = false,
    minSize: IntSize = IntSize(48, 20),
    padding: IntPadding = IntPadding(left = 4, right = 4, top = 1),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) = CheckButton(
    modifier = modifier,
    drawableSet = drawableSet,
    colors = colors,
    checked = checked,
    minSize = minSize,
    padding = padding,
    enabled = enabled,
    onClick = onClick,
    clickSound = clickSound,
    content = content,
)

@Composable
fun CheckButton(
    modifier: Modifier = Modifier,
    drawableSet: CheckButtonDrawables = LocalCheckButtonDrawable.current,
    colors: CheckButtonColors = LocalCheckButtonColors.current,
    checked: Boolean = false,
    minSize: IntSize = IntSize(48, 20),
    padding: IntPadding = IntPadding(left = 4, right = 4, top = 1),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) = Button(
    modifier = modifier,
    drawableSet = if (checked) {
        drawableSet.checked
    } else {
        drawableSet.unchecked
    },
    colorTheme = if (checked) {
        colors.checked
    } else {
        colors.unchecked
    },
    minSize = minSize,
    padding = padding,
    enabled = enabled,
    onClick = onClick,
    clickSound = clickSound,
    content = content,
)