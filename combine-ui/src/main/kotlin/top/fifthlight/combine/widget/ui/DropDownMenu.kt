package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Layout
import top.fifthlight.combine.modifier.Constraints
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.maxHeight
import top.fifthlight.combine.modifier.placement.minWidth
import top.fifthlight.combine.modifier.placement.onPlaced
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.modifier.pointer.consumePress
import top.fifthlight.combine.node.LocalTextMeasurer
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.paint.Drawable
import top.fifthlight.combine.widget.base.Popup
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import kotlin.math.max

@JvmName("DropdownMenuListString")
@Composable
fun DropdownMenuScope.DropdownItemList(
    modifier: Modifier = Modifier,
    items: List<Pair<Text, () -> Unit>>,
) {
    DropdownItemList(
        modifier = modifier,
        items = items,
        textProvider = { it.first },
        onItemSelected = { items[it].second() },
    )
}

@Composable
fun <T> DropdownMenuScope.DropdownItemList(
    modifier: Modifier = Modifier,
    drawableSet: SelectDrawableSet = LocalSelectDrawableSet.current,
    items: List<T>,
    textProvider: (T) -> Text,
    selectedIndex: Int = -1,
    onItemSelected: (Int) -> Unit = {},
) {
    val itemTextureWidth = drawableSet.itemUnselected.normal.padding.width
    val itemTextureHeight = drawableSet.itemUnselected.normal.padding.height
    val textMeasurer = LocalTextMeasurer.current
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            var itemWidth = contentWidth
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
            val drawable = if (index == selectedIndex) {
                drawableSet.itemSelected
            } else {
                drawableSet.itemUnselected
            }.getByState(state)
            Text(
                modifier = Modifier
                    .border(drawable)
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

interface DropdownMenuScope {
    val anchor: IntRect
    val panelBorder: Drawable
    val contentWidth: Int
}

private data class DropdownMenuScopeImpl(
    override val anchor: IntRect,
    override val panelBorder: Drawable
) : DropdownMenuScope {
    override val contentWidth = anchor.size.width - panelBorder.padding.width
}

@Composable
fun DropDownMenu(
    anchor: IntRect,
    border: Drawable = LocalSelectDrawableSet.current.floatPanel,
    onDismissRequest: () -> Unit,
    content: @Composable DropdownMenuScope.() -> Unit,
) {
    Popup(onDismissRequest = onDismissRequest) {
        var screenSize by remember { mutableStateOf<IntSize?>(null) }
        var contentSize by remember { mutableStateOf(IntSize.ZERO) }
        val currentScreenSize = screenSize ?: IntSize.ZERO
        val anchorCenter = anchor.offset + anchor.size / 2
        val topSide = anchorCenter.top > currentScreenSize.height / 2
        val top = if (topSide) {
            anchor.top - contentSize.height
        } else {
            anchor.bottom
        }
        val left = if (anchor.left + contentSize.width > currentScreenSize.width) {
            currentScreenSize.width - contentSize.width
        } else {
            anchor.left
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
            val scope = DropdownMenuScopeImpl(anchor, border)
            screenSize?.let { screenSize ->
                Box(
                    modifier = Modifier
                        .border(border)
                        .minWidth(anchor.size.width - 2)
                        .maxHeight(
                            if (topSide) {
                                anchor.top
                            } else {
                                screenSize.height - anchor.bottom
                            }
                        )
                        .onPlaced { contentSize = it.size }
                        .consumePress()
                ) {
                    content(scope)
                }
            }
        }
    }
}