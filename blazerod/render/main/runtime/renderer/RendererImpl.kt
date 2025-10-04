package top.fifthlight.blazerod.runtime.renderer

import com.mojang.blaze3d.textures.GpuTextureView
import top.fifthlight.blazerod.api.render.Renderer
import top.fifthlight.blazerod.api.render.RendererTypeHolder
import top.fifthlight.blazerod.api.render.ScheduledRenderer
import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.api.resource.RenderTask
import top.fifthlight.blazerod.runtime.RenderSceneImpl
import top.fifthlight.blazerod.runtime.RenderTaskImpl
import top.fifthlight.blazerod.runtime.TaskMap
import top.fifthlight.blazerod.runtime.data.MorphTargetBuffer
import top.fifthlight.blazerod.runtime.data.RenderSkinBuffer
import top.fifthlight.blazerod.runtime.node.component.PrimitiveComponent
import top.fifthlight.blazerod.runtime.resource.RenderPrimitive
import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl

@ActualImpl(Renderer::class)
abstract class RendererImpl<R : RendererImpl<R, T>, T : Renderer.Type<R, T>> : Renderer<R, T> {
    override fun render(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
        task: RenderTask,
        scene: RenderScene,
    ) {
        val instance = (task as RenderTaskImpl).instance
        val scene = (scene as RenderSceneImpl)
        for (component in scene.primitiveComponents) {
            render(
                colorFrameBuffer = colorFrameBuffer,
                depthFrameBuffer = depthFrameBuffer,
                scene = scene,
                primitive = component.primitive,
                primitiveIndex = component.primitiveIndex,
                task = task,
                skinBuffer = component.skinIndex?.let {
                    (instance.modelData.skinBuffers.getOrNull(it)?.content
                        ?: error("Has skin but no skin buffer"))
                },
                targetBuffer = component.morphedPrimitiveIndex?.let {
                    (instance.modelData.targetBuffers.getOrNull(it)?.content
                        ?: error("Has morph target but no morph target buffer"))
                },
            )
        }
    }

    abstract fun render(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
        scene: RenderSceneImpl,
        primitive: RenderPrimitive,
        primitiveIndex: Int,
        task: RenderTaskImpl,
        skinBuffer: RenderSkinBuffer?,
        targetBuffer: MorphTargetBuffer?,
    )

    companion object {
        @JvmStatic
        @ActualConstructor
        fun createVertexShaderTransform() = VertexShaderTransformRenderer.create()

        @JvmStatic
        @ActualConstructor
        fun createCpuTransform() = VertexShaderTransformRenderer.create()

        @JvmStatic
        @ActualConstructor
        fun createComputeShaderTransform() = VertexShaderTransformRenderer.create()
    }
}

@ActualImpl(RendererTypeHolder::class)
object RendererTypeHolderImpl : RendererTypeHolder {
    @JvmStatic
    @ActualConstructor
    fun of(): RendererTypeHolder = this

    override val types = listOf(
        VertexShaderTransformRenderer.Type,
        CpuTransformRenderer.Type,
        ComputeShaderTransformRenderer.Type,
    )

    override val vertexShaderTransform: Renderer.Type<*, *>
        get() = VertexShaderTransformRenderer.Type
    override val cpuTransform: Renderer.Type<*, *>
        get() = CpuTransformRenderer.Type
    override val computeShaderTransform: Renderer.Type<*, *>
        get() = ComputeShaderTransformRenderer.Type
}

abstract class ScheduledRendererImpl<R, T : Renderer.Type<R, T>> : RendererImpl<R, T>(), ScheduledRenderer<R, T>
        where R : ScheduledRenderer<R, T>, R : RendererImpl<R, T>

abstract class TaskMapScheduledRenderer<R, T : Renderer.Type<R, T>> :
    ScheduledRendererImpl<R, T>()
        where R : ScheduledRenderer<R, T>, R : ScheduledRendererImpl<R, T> {
    private val taskMap = TaskMap()

    override fun schedule(task: RenderTask) = taskMap.addTask(task as RenderTaskImpl)

    override fun executeTasks(colorFrameBuffer: GpuTextureView, depthFrameBuffer: GpuTextureView?) {
        taskMap.executeTasks { scene, tasks ->
            when (tasks.size) {
                0 -> {}
                1 -> {
                    render(colorFrameBuffer, depthFrameBuffer, tasks[0], scene)
                }

                else -> {
                    for (component in scene.primitiveComponents) {
                        renderInstanced(
                            colorFrameBuffer = colorFrameBuffer,
                            depthFrameBuffer = depthFrameBuffer,
                            tasks = tasks,
                            scene = scene,
                            component = component,
                        )
                    }
                }
            }
        }
    }

    abstract fun renderInstanced(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
        tasks: List<RenderTaskImpl>,
        scene: RenderSceneImpl,
        component: PrimitiveComponent,
    )
}
