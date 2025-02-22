package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.NinePatchTexture
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Layout
import top.fifthlight.combine.modifier.Constraints
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.modifier.pointer.consumePress
import top.fifthlight.combine.node.LocalTextMeasurer
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.widget.base.Popup
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures
import kotlin.math.max

data class SelectTextureSet(
    val menuBox: NinePatchTextureSet,
    val floatPanel: NinePatchTexture,
    val itemUnselected: NinePatchTextureSet,
    val itemSelected: NinePatchTextureSet,
)

val defaultSelectTextureSet = SelectTextureSet(
    menuBox = NinePatchTextureSet(
        normal = Textures.WIDGET_SELECT_SELECT,
        focus = Textures.WIDGET_SELECT_SELECT_HOVER,
        hover = Textures.WIDGET_SELECT_SELECT_HOVER,
        active = Textures.WIDGET_SELECT_SELECT_ACTIVE,
        disabled = Textures.WIDGET_SELECT_SELECT_DISABLED,
    ),
    floatPanel = Textures.WIDGET_SELECT_FLOAT_WINDOW,
    itemUnselected = NinePatchTextureSet(
        normal = Textures.WIDGET_LIST_LIST,
        focus = Textures.WIDGET_LIST_LIST_HOVER,
        hover = Textures.WIDGET_LIST_LIST_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    ),
    itemSelected = NinePatchTextureSet(
        normal = Textures.WIDGET_LIST_LIST_PRESSLOCK,
        focus = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        hover = Textures.WIDGET_LIST_LIST_PRESSLOCK_HOVER,
        active = Textures.WIDGET_LIST_LIST_ACTIVE,
        disabled = Textures.WIDGET_LIST_LIST_DISABLED,
    )
)

val LocalSelectTextureSet = staticCompositionLocalOf<SelectTextureSet> { defaultSelectTextureSet }

@Composable
fun SelectIcon(
    modifier: Modifier = Modifier,
    expanded: Boolean
) {
    Icon(
        modifier = modifier,
        texture = if (expanded) {
            Textures.ICON_UP
        } else {
            Textures.ICON_DOWN
        }
    )
}

@JvmName("DropdownMenuListString")
@Composable
fun <T> SelectScope.SelectItemList(
    modifier: Modifier = Modifier,
    items: List<T>,
    stringProvider: (T) -> String,
    selectedIndex: Int = -1,
    onItemSelected: (Int) -> Unit = {},
) {
    val textFactory = LocalTextFactory.current
    SelectItemList(
        modifier = modifier,
        items = items,
        textProvider = { textFactory.literal(stringProvider(it)) },
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected,
    )
}

@Composable
fun <T> SelectScope.SelectItemList(
    modifier: Modifier = Modifier,
    textureSet: SelectTextureSet = LocalSelectTextureSet.current,
    items: List<T>,
    textProvider: (T) -> Text,
    selectedIndex: Int = -1,
    onItemSelected: (Int) -> Unit = {},
) {
    val itemTextureWidth = textureSet.itemUnselected.normal.padding.width
    val itemTextureHeight = textureSet.itemUnselected.normal.padding.height
    val textMeasurer = LocalTextMeasurer.current
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            var itemWidth = anchor.size.width - textureSet.floatPanel.padding.width
            var itemHeight = 0
            var itemHeights = IntArray(measurables.size)
            for ((index, item) in items.withIndex()) {
                val textSize = textMeasurer.measure(textProvider(item))
                val width = textSize.width + itemTextureWidth
                val height = textSize.height + itemTextureHeight
                itemHeights[index] = height
                itemWidth = max(width, itemWidth)
                itemHeight += height
            }

            val width = itemWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = itemHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(
                    Constraints(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = itemHeights[index],
                        maxHeight = itemHeights[index],
                    )
                )
            }
            layout(width, height) {
                var yPos = 0
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeAt(0, yPos)
                    yPos += placeable.height
                }
            }
        },
    ) {
        for ((index, item) in items.withIndex()) {
            val text = textProvider(item)
            val interactionSource = remember { MutableInteractionSource() }
            val state by widgetState(interactionSource)
            val texture = if (index == selectedIndex) {
                textureSet.itemSelected
            } else {
                textureSet.itemUnselected
            }.getByState(state)
            Text(
                modifier = Modifier
                    .border(texture)
                    .clickable(interactionSource) {
                        onItemSelected(index)
                    }
                    .focusable(interactionSource),
                color = if (index == selectedIndex) {
                    Colors.BLACK
                } else {
                    Colors.WHITE
                },
                text = text,
            )
        }
    }
}

interface SelectScope {
    val anchor: IntRect
}

private fun SelectScope(anchor: IntRect) = object : SelectScope {
    override val anchor: IntRect = anchor
}

@Composable
fun Select(
    modifier: Modifier = Modifier,
    textureSet: SelectTextureSet = LocalSelectTextureSet.current,
    colorTheme: ColorTheme? = null,
    expanded: Boolean = false,
    onExpandedChanged: (Boolean) -> Unit,
    dropDownContent: @Composable SelectScope.() -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val menuTexture = textureSet.menuBox.getByState(state)

    var anchor by remember { mutableStateOf<IntRect?>(null) }
    val colorTheme = colorTheme ?: ColorTheme.light

    @Composable
    fun ContentStyle(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
            content = content,
        )
    }

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
        ContentStyle {
            content()
        }
    }

    val currentAnchor = anchor
    if (expanded && currentAnchor != null) {
        val scope = SelectScope(currentAnchor)
        Popup(
            onDismissRequest = {
                onExpandedChanged(false)
            }
        ) {
            var screenSize by remember { mutableStateOf<IntSize?>(null) }
            var contentSize by remember { mutableStateOf(IntSize.ZERO) }
            val currentScreenSize = screenSize ?: IntSize.ZERO
            val top = if (currentAnchor.bottom + contentSize.height > currentScreenSize.height) {
                currentAnchor.top - contentSize.height
            } else {
                currentAnchor.bottom
            }
            val left = if (currentAnchor.left + contentSize.width > currentScreenSize.width) {
                currentScreenSize.width - contentSize.width
            } else {
                currentAnchor.left
            }

            Layout(
                modifier = Modifier
                    .fillMaxSize()
                    .onPlaced { screenSize = it.size },
                measurePolicy = { measurables, _ ->
                    val constraints = Constraints()
                    val placeables = measurables.map { it.measure(constraints) }
                    val width = placeables.maxOfOrNull { it.width } ?: 0
                    val height = placeables.maxOfOrNull { it.height } ?: 0
                    layout(width, height) {
                        placeables.forEach { it.placeAt(left, top) }
                    }
                }
            ) {
                screenSize?.let { screenSize ->
                    Box(
                        modifier = Modifier
                            .border(Textures.WIDGET_SELECT_FLOAT_WINDOW)
                            .minWidth(currentAnchor.size.width - 2)
                            .maxHeight(screenSize.height / 2)
                            .onPlaced { contentSize = it.size }
                            .consumePress()
                    ) {
                        ContentStyle {
                            dropDownContent(scope)
                        }
                    }
                }
            }
        }
    }
}