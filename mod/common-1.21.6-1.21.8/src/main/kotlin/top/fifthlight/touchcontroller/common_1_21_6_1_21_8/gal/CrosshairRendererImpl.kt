package top.fifthlight.touchcontroller.common_1_21_6_1_21_8.gal

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import org.joml.Matrix3x2f
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.platform_1_21_6_1_21_8.CanvasImpl
import top.fifthlight.combine.platform_1_21_6_1_21_8.SubmittableDrawContext
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.TouchRingConfig
import top.fifthlight.touchcontroller.common.gal.CrosshairRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val CROSSHAIR_CIRCLE_PARTS = 24
private const val CROSSHAIR_CIRCLE_ANGLE = 2 * PI.toFloat() / CROSSHAIR_CIRCLE_PARTS

private fun point(angle: Float, radius: Float) = Offset(
    x = cos(angle) * radius,
    y = sin(angle) * radius
)

private val CROSSHAIR_SNIPPET = RenderPipeline.builder()
    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
    .withUniform("Projection", UniformType.UNIFORM_BUFFER)
    .withVertexShader("core/gui")
    .withFragmentShader("core/gui")
    .withBlend(
        BlendFunction(
            SourceFactor.ONE_MINUS_DST_COLOR,
            DestFactor.ONE_MINUS_SRC_COLOR,
            SourceFactor.ONE,
            DestFactor.ZERO
        )
    )
    .buildSnippet()

private class CrosshairOuterGuiElementRenderState(
    val pose: Matrix3x2f,
    val radius: Int = 36,
    val outerRadius: Int = 2,
) : GuiElementRenderState {
    companion object {
        private val CROSSHAIR_OUTER_PIPELINE: RenderPipeline = RenderPipeline.builder(CROSSHAIR_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(Identifier.of(BuildInfo.MOD_ID, "pipeline/crosshair_outer").toMinecraft())
            .build()
    }

    override fun buildVertices(vertexConsumer: VertexConsumer, traverseRange: Float) {
        val innerRadius = radius.toFloat()
        val outerRadius = (radius + outerRadius).toFloat()
        var angle = -PI.toFloat() / 2f
        repeat(CROSSHAIR_CIRCLE_PARTS) {
            val endAngle = angle + CROSSHAIR_CIRCLE_ANGLE
            val point0 = point(angle, outerRadius)
            val point1 = point(endAngle, outerRadius)
            val point2 = point(angle, innerRadius)
            val point3 = point(endAngle, innerRadius)
            angle = endAngle

            with(vertexConsumer) {
                addVertexWith2DPose(pose, point0.x, point0.y, traverseRange).setColor(Colors.WHITE.value)
                addVertexWith2DPose(pose, point2.x, point2.y, traverseRange).setColor(Colors.WHITE.value)
                addVertexWith2DPose(pose, point3.x, point3.y, traverseRange).setColor(Colors.WHITE.value)
                addVertexWith2DPose(pose, point1.x, point1.y, traverseRange).setColor(Colors.WHITE.value)
            }
        }
    }

    override fun pipeline() = CROSSHAIR_OUTER_PIPELINE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea() = null

    private val bounds by lazy {
        val totalRadius = radius + outerRadius
        ScreenRectangle(-totalRadius, -totalRadius, totalRadius, totalRadius)
    }
    override fun bounds() = bounds
}

private class CrosshairInnerGuiElementRenderState(
    val pose: Matrix3x2f,
    val radius: Int = 36,
    val progress: Float,
) : GuiElementRenderState {
    companion object {
        private val CROSSHAIR_INNER_PIPELINE: RenderPipeline = RenderPipeline.builder(CROSSHAIR_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(Identifier.of(BuildInfo.MOD_ID, "pipeline/crosshair_inner").toMinecraft())
            .build()
    }

    override fun buildVertices(vertexConsumer: VertexConsumer, traverseRange: Float) {
        val scale = radius * progress
        for (i in 0 until CROSSHAIR_CIRCLE_PARTS) {
            val angle0 = -i * CROSSHAIR_CIRCLE_ANGLE
            val angle1 = - (i+1) * CROSSHAIR_CIRCLE_ANGLE
            val p0 = point(angle0, scale)
            val p1 = point(angle1, scale)
            vertexConsumer.addVertexWith2DPose(pose, 0f, 0f, traverseRange).setColor(Colors.WHITE.value)
            vertexConsumer.addVertexWith2DPose(pose, p0.x, p0.y, traverseRange).setColor(Colors.WHITE.value)
            vertexConsumer.addVertexWith2DPose(pose, p1.x, p1.y, traverseRange).setColor(Colors.WHITE.value)
            vertexConsumer.addVertexWith2DPose(pose, 0f, 0f, traverseRange).setColor(Colors.WHITE.value)
        }
    }

    override fun pipeline() = CROSSHAIR_INNER_PIPELINE

    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun scissorArea() = null

    private val bounds by lazy {
        ScreenRectangle(-radius, -radius, radius, radius)
    }
    override fun bounds() = bounds
}

object CrosshairRendererImpl : CrosshairRenderer {
    private fun GuiGraphics.submitElement(guiElementRenderState: GuiElementRenderState) =
        (this as SubmittableDrawContext).`touchcontroller$submitElement`(guiElementRenderState)

    override fun renderOuter(canvas: Canvas, config: TouchRingConfig) {
        val drawContext = (canvas as CanvasImpl).drawContext
        drawContext.submitElement(
            CrosshairOuterGuiElementRenderState(
                Matrix3x2f(drawContext.pose()),
                config.radius,
                config.outerRadius
            )
        )
    }

    override fun renderInner(canvas: Canvas, config: TouchRingConfig, progress: Float) {
        val drawContext = (canvas as CanvasImpl).drawContext
        drawContext.submitElement(CrosshairInnerGuiElementRenderState(Matrix3x2f(drawContext.pose()), config.radius, progress))
    }
}
