package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.combine.paint.Canvas
import top.fifthlight.touchcontroller.common.config.TouchRingConfig

interface CrosshairRenderer {
    fun renderOuter(canvas: Canvas, config: TouchRingConfig)
    fun renderInner(canvas: Canvas, config: TouchRingConfig, progress: Float)
}
