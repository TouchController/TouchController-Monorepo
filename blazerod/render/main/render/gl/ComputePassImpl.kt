package top.fifthlight.blazerod.render.gl

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gl.GlCommandEncoder
import top.fifthlight.blazerod.extension.internal.CommandEncoderExtInternal
import top.fifthlight.blazerod.extension.internal.gl.GpuDeviceExtInternal
import top.fifthlight.blazerod.systems.ComputePass
import top.fifthlight.blazerod.systems.ComputePipeline
import top.fifthlight.blazerod.systems.gl.CompiledComputePipeline
import java.util.function.Supplier

class ComputePassImpl(
    private val resourceManager: GlCommandEncoder,
) : ComputePass {
    private val resourceManagerExt
        get() = resourceManager as CommandEncoderExtInternal
    private val backend
        get() = resourceManagerExt.`blazerod$getBackend`()

    private var closed = false
    private var debugGroupPushCount = 0
    var pipeline: CompiledComputePipeline? = null
        private set

    @JvmField
    val simpleUniforms = mutableMapOf<String, GpuBufferSlice>()

    @JvmField
    val samplerUniforms = mutableMapOf<String, GpuTextureView>()

    @JvmField
    val setSimpleUniforms = mutableSetOf<String>()

    @JvmField
    val storageBuffers = mutableMapOf<String, GpuBufferSlice>()

    override fun pushDebugGroup(label: Supplier<String>) {
        debugGroupPushCount++
        backend.debugLabelManager.pushDebugGroup(label)
    }

    override fun popDebugGroup() {
        debugGroupPushCount--
        backend.debugLabelManager.popDebugGroup()
    }

    override fun setPipeline(pipeline: ComputePipeline) {
        if (this.pipeline?.info() != pipeline) {
            this.setSimpleUniforms.addAll(this.simpleUniforms.keys)
            this.setSimpleUniforms.addAll(this.samplerUniforms.keys)
        }

        val backendExt = backend as GpuDeviceExtInternal
        this.pipeline = backendExt.`blazerod$compilePipelineCached`(pipeline)
    }

    override fun bindSampler(name: String, view: GpuTextureView?) {
        if (view == null) {
            samplerUniforms.remove(name)
        } else {
            samplerUniforms.put(name, view)
        }

        setSimpleUniforms.add(name)
    }

    override fun setUniform(name: String, buffer: GpuBuffer) {
        simpleUniforms.put(name, buffer.slice())
        setSimpleUniforms.add(name)
    }

    override fun setUniform(name: String, slice: GpuBufferSlice) {
        val alignment = backend.uniformOffsetAlignment
        require(slice.offset() % alignment == 0) { "Uniform buffer offset must be aligned to $alignment" }
        simpleUniforms.put(name, slice)
        setSimpleUniforms.add(name)
    }

    override fun setStorageBuffer(name: String, buffer: GpuBufferSlice) {
        storageBuffers.put(name, buffer)
    }

    private fun requireNotClosed() = require(!closed) { "Can't use a closed compute pass" }

    override fun dispatch(x: Int, y: Int, z: Int) {
        requireNotClosed()
        resourceManagerExt.`blazerod$dispatchCompute`(this, x, y, z)
    }

    override fun close() {
        if (closed) {
            return
        }

        require(debugGroupPushCount == 0) { "Compute pass had debug groups left open!" }
        closed = true
        resourceManager.closePass()
    }
}