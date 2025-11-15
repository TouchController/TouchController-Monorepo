package top.fifthlight.blazerod.runtime.resource

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import top.fifthlight.blazerod.api.refcount.AbstractRefCount
import top.fifthlight.blazerod.extension.GpuBufferExt
import top.fifthlight.blazerod.extension.extraUsage
import top.fifthlight.blazerod.render.GpuIndexBuffer
import top.fifthlight.blazerod.render.RefCountedGpuBuffer
import java.nio.ByteBuffer

class RenderPrimitive(
    val vertices: Int,
    val vertexFormatMode: VertexFormat.DrawMode,
    val gpuVertexBuffer: RefCountedGpuBuffer?,
    val cpuVertexBuffer: ByteBuffer?,
    val indexBuffer: GpuIndexBuffer?,
    val material: RenderMaterial<*>,
    val targets: Targets?,
    val targetGroups: List<MorphTargetGroup>,
) : AbstractRefCount() {
    override val typeId: String
        get() = "primitive"

    init {
        gpuVertexBuffer?.increaseReferenceCount()
        indexBuffer?.increaseReferenceCount()
        material.increaseReferenceCount()
        if (targetGroups.isEmpty()) {
            require(targets == null) { "Empty target groups with non-empty targets" }
        } else {
            require(targets != null) { "Non-empty target groups with empty targets" }
        }
    }

    val gpuComplete = gpuVertexBuffer != null && targets?.gpuComplete != false
    val cpuComplete = cpuVertexBuffer != null && targets?.cpuComplete != false

    class Target(
        val gpuBuffer: GpuBuffer?,
        val cpuBuffer: ByteBuffer?,
        val targetsCount: Int,
    ) : AutoCloseable {
        val slice = gpuBuffer?.slice()

        init {
            gpuBuffer?.let {
                val tbo = gpuBuffer.usage() and GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER != 0
                val ssbo = gpuBuffer.extraUsage and GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER != 0
                require(tbo || ssbo) {
                    "RenderPrimitive's target should have buffer with usage USAGE_UNIFORM_TEXEL_BUFFER or EXTRA_USAGE_STORAGE_BUFFER"
                }
            }
        }

        override fun close() {
            gpuBuffer?.close()
        }
    }

    class Targets(
        val position: Target,
        val color: Target,
        val texCoord: Target,
    ) {
        val gpuComplete = position.gpuBuffer != null && color.gpuBuffer != null && texCoord.gpuBuffer != null
        val cpuComplete = position.cpuBuffer != null && color.cpuBuffer != null && texCoord.cpuBuffer != null
    }

    override fun onClosed() {
        gpuVertexBuffer?.decreaseReferenceCount()
        indexBuffer?.decreaseReferenceCount()
        material.decreaseReferenceCount()
        targets?.apply {
            position.close()
            color.close()
            texCoord.close()
        }
    }
}
