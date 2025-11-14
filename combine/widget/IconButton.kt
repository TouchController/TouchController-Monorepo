package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.theme.LocalTheme
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.layout.BoxScope
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize

val IconButtonDrawableSet
    @Composable get() = LocalTheme.current.drawables.iconButton

val SelectedIconButtonDrawableSet
    @Composable get() = LocalTheme.current.drawables.selectedIconButton

@NonSkippableComposable
@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    selected: Boolean = false,
    drawableSet: DrawableSet = if (selected) {
        SelectedIconButtonDrawableSet
    } else {
        IconButtonDrawableSet
    },
    colorTheme: ColorTheme? = ColorTheme.dark,
    minSize: IntSize = IntSize(0, 0),
    padding: IntPadding = IntPadding(1),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) = Button(
    modifier = modifier,
    focusable = focusable,
    drawableSet = drawableSet,
    colorTheme = colorTheme,
    minSize = minSize,
    padding = padding,
    enabled = enabled,
    onClick = onClick,
    clickSound = clickSound,
    content = content
)
