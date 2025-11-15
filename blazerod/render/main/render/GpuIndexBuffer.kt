package top.fifthlight.blazerod.render

import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.VertexFormat
import top.fifthlight.blazerod.api.refcount.AbstractRefCount

class GpuIndexBuffer(
    val type: VertexFormat.IndexType,
    val length: Int,
    val buffer: RefCountedGpuBuffer,
) : AbstractRefCount() {
    override val typeId: String
        get() = "index_buffer"

    init {
        buffer.increaseReferenceCount()
        buffer.inner.size == length * type.size
    }

    override fun onClosed() {
        buffer.decreaseReferenceCount()
    }
}

fun RenderPass.setIndexBuffer(indexBuffer: GpuIndexBuffer) = setIndexBuffer(indexBuffer.buffer.inner, indexBuffer.type)
