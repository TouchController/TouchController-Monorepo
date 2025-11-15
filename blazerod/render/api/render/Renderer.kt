package top.fifthlight.blazerod.api.render

import com.mojang.blaze3d.textures.GpuTextureView
import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.api.resource.RenderTask
import top.fifthlight.mergetools.api.ExpectFactory

interface Renderer<R : Renderer<R, T>, T : Renderer.Type<R, T>> : AutoCloseable {
    abstract class Type<R : Renderer<R, T>, T : Type<R, T>> {
        abstract val id: String
        abstract val isAvailable: Boolean
        abstract val supportScheduling: Boolean
        abstract fun create(): R
    }

    val type: T

    fun render(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
        task: RenderTask,
        scene: RenderScene,
    )

    fun rotate()

    @ExpectFactory
    interface Factory {
        fun createVertexShaderTransform(): Renderer<*, *>
        fun createCpuTransform(): Renderer<*, *>
        fun createComputeShaderTransform(): Renderer<*, *>
    }
}

interface RendererTypeHolder {
    val types: List<Renderer.Type<*, *>>

    val vertexShaderTransform: Renderer.Type<*, *>
    val cpuTransform: Renderer.Type<*, *>
    val computeShaderTransform: Renderer.Type<*, *>

    @ExpectFactory
    interface Factory {
        fun of(): RendererTypeHolder
    }
}

interface ScheduledRenderer<R : ScheduledRenderer<R, T>, T : Renderer.Type<R, T>> : Renderer<R, T> {
    fun schedule(task: RenderTask)

    fun executeTasks(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
    )
}
