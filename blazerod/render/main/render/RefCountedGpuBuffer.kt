package top.fifthlight.blazerod.render

import com.mojang.blaze3d.buffers.GpuBuffer
import top.fifthlight.blazerod.api.refcount.AbstractRefCount

class RefCountedGpuBuffer(val inner: GpuBuffer) : AbstractRefCount() {
    override val typeId: String
        get() = "gpu_buffer"

    override fun onClosed() {
        inner.close()
    }
}