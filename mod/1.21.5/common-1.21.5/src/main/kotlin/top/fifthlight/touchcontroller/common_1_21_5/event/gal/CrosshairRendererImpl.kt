package top.fifthlight.touchcontroller.common_1_21_5.event.gal

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.platform_1_21_5.CanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.TouchRingConfig
import top.fifthlight.touchcontroller.common.gal.CrosshairRenderer
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val CROSSHAIR_CIRCLE_PARTS = 24
private const val CROSSHAIR_CIRCLE_ANGLE = 2 * PI.toFloat() / CROSSHAIR_CIRCLE_PARTS

private fun point(angle: Float, radius: Float) = Offset(
    x = cos(angle) * radius,
    y = sin(angle) * radius
)

object CrosshairRendererImpl : CrosshairRenderer {
    private var outerBuffer: Triple<TouchRingConfig, GpuBuffer, Int>? = null
    private val outerIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)
    private var innerBuffer: Pair<GpuBuffer, Int>? = null
    private val innerIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLE_FAN)

    private val CROSSHAIR_OUTER_SNIPPET = RenderPipeline.builder()
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("ColorModulator", UniformType.VEC4)
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

    private val CROSSHAIR_OUTER_PIPELINE: RenderPipeline = RenderPipeline.builder(CROSSHAIR_OUTER_SNIPPET)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withLocation(Identifier.of(BuildInfo.MOD_ID, "pipeline/crosshair_outer").toMinecraft())
        .build()

    private val CROSSHAIR_INNER_PIPELINE: RenderPipeline = RenderPipeline.builder(CROSSHAIR_OUTER_SNIPPET)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
        .withLocation(Identifier.of(BuildInfo.MOD_ID, "pipeline/crosshair_inner").toMinecraft())
        .build()

    private fun buildOuter(config: TouchRingConfig): Pair<GpuBuffer, Int> {
        outerBuffer?.let { (lastConfig, buffer, indexCount) ->
            if (lastConfig == config) {
                return Pair(buffer, indexCount)
            } else {
                buffer.close()
            }
        }

        val bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        val innerRadius = config.radius.toFloat()
        val outerRadius = (config.radius + config.outerRadius).toFloat()
        var angle = -PI.toFloat() / 2f

        repeat(CROSSHAIR_CIRCLE_PARTS) {
            val endAngle = angle + CROSSHAIR_CIRCLE_ANGLE
            val point0 = point(angle, outerRadius)
            val point1 = point(endAngle, outerRadius)
            val point2 = point(angle, innerRadius)
            val point3 = point(endAngle, innerRadius)
            angle = endAngle

            with(bufferBuilder) {
                addVertex(point0.x, point0.y, 0f).setColor(Colors.WHITE.value)
                addVertex(point2.x, point2.y, 0f).setColor(Colors.WHITE.value)
                addVertex(point3.x, point3.y, 0f).setColor(Colors.WHITE.value)
                addVertex(point1.x, point1.y, 0f).setColor(Colors.WHITE.value)
            }
        }

        return bufferBuilder.buildOrThrow().let { mesh ->
            val gpuBuffer = RenderSystem.getDevice().createBuffer(
                { "Crosshair Outer Buffer" },
                BufferType.VERTICES,
                BufferUsage.STATIC_WRITE,
                mesh.vertexBuffer()
            )
            val indexCount = mesh.drawState().indexCount
            outerBuffer = Triple(config, gpuBuffer, indexCount)
            Pair(gpuBuffer, indexCount)
        }
    }

    private fun buildInner(): Pair<GpuBuffer, Int> {
        innerBuffer?.let { return it }

        val bufferBuilder =
            Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)
        bufferBuilder.addVertex(0f, 0f, 0f).setColor(Colors.WHITE.value)

        var angle = 0f
        repeat(CROSSHAIR_CIRCLE_PARTS + 1) {
            val point = point(angle, 1f) // Base radius of 1, scaled during rendering
            angle -= CROSSHAIR_CIRCLE_ANGLE
            bufferBuilder.addVertex(point.x, point.y, 0f).setColor(Colors.WHITE.value)
        }

        return bufferBuilder.buildOrThrow().let { mesh ->
            val gpuBuffer = RenderSystem.getDevice().createBuffer(
                { "Crosshair Inner Buffer" },
                BufferType.VERTICES,
                BufferUsage.STATIC_WRITE,
                mesh.vertexBuffer()
            )
            Pair(gpuBuffer, mesh.drawState().indexCount).also { innerBuffer = it }
        }
    }

    private val mainColorTexture: GpuTexture
        get() = Minecraft.getInstance().mainRenderTarget.colorTexture!!
    private val mainDepthTexture: GpuTexture?
        get() = Minecraft.getInstance().mainRenderTarget.depthTexture

    override fun renderOuter(canvas: Canvas, config: TouchRingConfig) {
        val drawContext = (canvas as CanvasImpl).drawContext
        drawContext.flush()

        val matrix = drawContext.pose().last().pose()
        val (vertexBuffer, indexCount) = buildOuter(config)
        val indexBuffer = outerIndexBuffer.getBuffer(indexCount)

        RenderSystem.getModelViewStack().pushMatrix()
        RenderSystem.getModelViewStack().mul(matrix)

        RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            .use { renderPass ->
                renderPass.setPipeline(CROSSHAIR_OUTER_PIPELINE)
                renderPass.setVertexBuffer(0, vertexBuffer)
                renderPass.setIndexBuffer(indexBuffer, outerIndexBuffer.type())
                renderPass.drawIndexed(0, indexCount)
            }

        RenderSystem.getModelViewStack().popMatrix()
    }

    override fun renderInner(canvas: Canvas, config: TouchRingConfig, progress: Float) {
        val drawContext = (canvas as CanvasImpl).drawContext
        val (vertexBuffer, indexCount) = buildInner()
        val indexBuffer = innerIndexBuffer.getBuffer(indexCount)

        RenderSystem.getModelViewStack().pushMatrix()
        RenderSystem.getModelViewStack().mul(drawContext.pose().last().pose())
        RenderSystem.getModelViewStack().scale(config.radius * progress, config.radius * progress, 1f)

        RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(mainColorTexture, OptionalInt.empty(), mainDepthTexture, OptionalDouble.empty())
            .use { renderPass ->
                renderPass.setPipeline(CROSSHAIR_INNER_PIPELINE)
                renderPass.setVertexBuffer(0, vertexBuffer)
                renderPass.setIndexBuffer(indexBuffer, innerIndexBuffer.type())
                renderPass.drawIndexed(0, indexCount)
            }

        RenderSystem.getModelViewStack().popMatrix()
    }
}
