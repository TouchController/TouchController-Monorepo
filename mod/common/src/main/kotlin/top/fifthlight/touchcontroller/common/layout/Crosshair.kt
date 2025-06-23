package top.fifthlight.touchcontroller.common.layout

import org.koin.core.component.get
import top.fifthlight.combine.paint.*
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.gal.CrosshairRenderer

data class CrosshairStatus(
    val position: Offset,
    val breakPercent: Float,
) {
    val positionX
        get() = position.x

    val positionY
        get() = position.y
}

fun Context.Crosshair() {
    val status = result.crosshairStatus ?: return
    val crosshairRenderer: CrosshairRenderer = get()

    val config = config.touchRing
    drawQueue.enqueue { canvas ->
        canvas.withTranslate(status.position * windowScaledSize) {
            if (status.breakPercent > 0f) {
                val progress = status.breakPercent * (1f - config.initialProgress) + config.initialProgress
                crosshairRenderer.renderInner(canvas, config, progress)
            }
            crosshairRenderer.renderOuter(canvas, config)
        }
    }
}