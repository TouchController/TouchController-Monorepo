package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.VertexFormat

fun RenderPass.setVertexFormat(mode: VertexFormat) =
    (this as RenderPassExt).`blazerod$setVertexFormat`(mode)

fun RenderPass.setVertexFormatMode(mode: VertexFormat.DrawMode) =
    (this as RenderPassExt).`blazerod$setVertexFormatMode`(mode)

fun RenderPass.setStorageBuffer(name: String, buffer: GpuBufferSlice) =
    (this as RenderPassExt).`blazerod$setStorageBuffer`(name, buffer)

fun RenderPass.draw(baseVertex: Int, firstIndex: Int, count: Int, instanceCount: Int) =
    (this as RenderPassExt).`blazerod$draw`(baseVertex, firstIndex, count, instanceCount)