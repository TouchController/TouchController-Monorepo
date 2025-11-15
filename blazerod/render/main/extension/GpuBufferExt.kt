package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBuffer

val GpuBuffer.extraUsage
    get() = (this as GpuBufferExt).`blazerod$getExtraUsage`()