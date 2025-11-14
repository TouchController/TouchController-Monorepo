package top.fifthlight.combine.widget.ui.item

import androidx.compose.runtime.Composable
import top.fifthlight.combine.item.Item
import top.fifthlight.combine.item.ItemCanvas
import top.fifthlight.combine.item.ItemStack
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.size
import top.fifthlight.combine.widget.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize

@Composable
fun Item(
    modifier: Modifier = Modifier,
    size: Int = 16,
    item: Item?
) = Item(modifier, size, item?.toStack())

@Composable
fun Item(
    modifier: Modifier = Modifier,
    size: Int = 16,
    itemStack: ItemStack?,
) {
    Canvas(
        modifier = Modifier
            .size(size)
            .then(modifier),
    ) {
        require(this is ItemCanvas)
        if (itemStack != null) {
            drawItemStack(IntOffset.ZERO, IntSize(size), itemStack)
        }
    }
}