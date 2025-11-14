package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.*
import org.koin.compose.koinInject
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.onPlaced
import top.fifthlight.combine.modifier.placement.size
import top.fifthlight.combine.paint.*
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.common.config.GlobalConfig
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider
import top.fifthlight.touchcontroller.common.layout.Context
import top.fifthlight.touchcontroller.common.layout.ContextResult
import top.fifthlight.touchcontroller.common.layout.DrawQueue

@Composable
fun ControllerWidget(
    modifier: Modifier = Modifier,
    widget: ControllerWidget,
    scale: Float = 1f,
) {
    val itemListProvider: DefaultItemListProvider = koinInject()
    val widgetSize = widget.size()
    val renderSize = (widgetSize.toSize() * scale).toIntSize()
    val drawQueue = remember(widget, itemListProvider) {
        val queue = DrawQueue()
        val context = Context(
            windowSize = IntSize.ZERO,
            windowScaledSize = IntSize.ZERO,
            drawQueue = queue,
            size = widgetSize,
            screenOffset = IntOffset.ZERO,
            pointers = mutableMapOf(),
            result = ContextResult(),
            config = GlobalConfig.default(itemListProvider),
            opacity = widget.opacity,
        )
        widget.layout(context)
        queue
    }
    Canvas(
        modifier = Modifier
            .size(renderSize)
            .then(modifier)
    ) {
        withScale(scale) {
            drawQueue.execute(this)
        }
    }
}

@Composable
fun AutoScaleControllerWidget(
    modifier: Modifier = Modifier,
    widget: ControllerWidget,
) {
    var entrySize by remember { mutableStateOf(IntSize.ZERO) }
    val itemListProvider: DefaultItemListProvider = koinInject()
    val (drawQueue, componentScaleFactor, offset) = remember(widget, entrySize, itemListProvider) {
        val queue = DrawQueue()

        val widgetSize = widget.size()
        val widthFactor = if (widgetSize.width > entrySize.width) {
            entrySize.width.toFloat() / widgetSize.width.toFloat()
        } else 1f
        val heightFactor = if (widgetSize.height > entrySize.height) {
            entrySize.height.toFloat() / widgetSize.height.toFloat()
        } else 1f
        val componentScaleFactor = widthFactor.coerceAtMost(heightFactor)
        val displaySize = (widgetSize.toSize() * componentScaleFactor).toIntSize()
        val offset = (entrySize - displaySize) / 2

        val context = Context(
            windowSize = IntSize.ZERO,
            windowScaledSize = IntSize.ZERO,
            drawQueue = queue,
            size = widget.size(),
            screenOffset = IntOffset.ZERO,
            pointers = mutableMapOf(),
            result = ContextResult(),
            config = GlobalConfig.default(itemListProvider),
            opacity = widget.opacity,
        )
        widget.layout(context)
        Triple(queue, componentScaleFactor, offset)
    }
    Canvas(
        modifier = Modifier
            .onPlaced { entrySize = it.size }
            .then(modifier),
    ) {
        withTranslate(offset) {
            withScale(componentScaleFactor) {
                drawQueue.execute(this)
            }
        }
    }
}