package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.data.NinePatchTexture
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.anchor
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.data.IntRect
import top.fifthlight.touchcontroller.assets.Textures

data class SelectDrawableSet(
    val menuBox: DrawableSet,
    val floatPanel: NinePatchTexture,
    val itemUnselected: DrawableSet,
    val itemSelected: DrawableSet,
)

val defaultSelectDrawableSet = SelectDrawableSet(
    menuBox = DrawableSet(
        normal = Textures.WIDGET_SELECT_SELECT,
        focus = Textures.WIDGET_SELECT_SELECT_HOVER,
        hover = Textures.WIDGET_SELECT_SELECT_HOVER,
        active = Textures.WIDGET_SELECT_SELECT_ACTIVE,
        disabled = Textures.WIDGET_SELECT_SELECT_DISABLED,
    ),
    floatPanel = Textures.WIDGET_BACKGROUND_FLOAT_WINDOW,
    itemUnselected = DrawableSet(
        normal = Textures.WIDGET_LIST_LIST,
        focus = Textures.WIDGET_LIST_LIST_HOVER,
        hover = Textures.WIDGET_LIST_LIST_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    ),
    itemSelected = DrawableSet(
        normal = Textures.WIDGET_LIST_LIST_PRESSLOCK,
        focus = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        hover = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    )
)

val LocalSelectDrawableSet = staticCompositionLocalOf<SelectDrawableSet> { defaultSelectDrawableSet }

@Composable
fun SelectIcon(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier,
        drawable = if (expanded) {
            Textures.ICON_UP
        } else {
            Textures.ICON_DOWN
        }
    )
}

@Composable
fun Select(
    modifier: Modifier = Modifier,
    drawableSet: SelectDrawableSet = LocalSelectDrawableSet.current,
    colorTheme: ColorTheme? = null,
    expanded: Boolean = false,
    onExpandedChanged: (Boolean) -> Unit,
    dropDownContent: @Composable DropdownMenuScope.() -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val menuTexture = drawableSet.menuBox.getByState(state)

    var anchor by remember { mutableStateOf<IntRect?>(null) }
    val colorTheme = colorTheme ?: ColorTheme.light

    Row(
        modifier = Modifier
            .border(menuTexture)
            .anchor {
                anchor = it
            }
            .clickable(interactionSource) {
                onExpandedChanged(!expanded)
            }
            .focusable(interactionSource)
            .then(modifier),
    ) {
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
            LocalWidgetState provides state,
        ) {
            content()
        }
    }

    val currentAnchor = anchor
    if (expanded && currentAnchor != null) {
        DropDownMenu(
            border = drawableSet.floatPanel,
            anchor = currentAnchor,
            onDismissRequest = { onExpandedChanged(false) }
        ) {
            CompositionLocalProvider(
                LocalColorTheme provides colorTheme
            ) {
                dropDownContent()
            }
        }
    }
}