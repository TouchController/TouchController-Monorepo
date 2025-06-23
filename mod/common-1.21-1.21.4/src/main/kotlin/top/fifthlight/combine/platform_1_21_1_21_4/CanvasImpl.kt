package top.fifthlight.combine.platform_1_21_1_21_4

import net.minecraft.client.gui.GuiGraphics
import org.joml.Quaternionf
import top.fifthlight.combine.platform_1_21_1_21_5.AbstractCanvasImpl

abstract class AbstractCanvasImpl(
    drawContext: GuiGraphics,
) : AbstractCanvasImpl(drawContext) {
    override fun rotate(degrees: Float) {
        Quaternionf().apply {
            rotateZ(Math.toRadians(degrees.toDouble()).toFloat())
            drawContext.pose().mulPose(this)
        }
    }
}