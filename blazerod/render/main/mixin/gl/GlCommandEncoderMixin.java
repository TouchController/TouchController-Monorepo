package top.fifthlight.blazerod.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.GlTextureView;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.blazerod.extension.CommandEncoderExt;
import top.fifthlight.blazerod.extension.GpuBufferExt;
import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.extension.internal.CommandEncoderExtInternal;
import top.fifthlight.blazerod.extension.internal.RenderPassExtInternal;
import top.fifthlight.blazerod.extension.internal.gl.ShaderProgramExtInternal;
import top.fifthlight.blazerod.render.gl.ComputePassImpl;
import top.fifthlight.blazerod.systems.ComputePass;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtInternal {
    @Shadow
    @Final
    private GlBackend backend;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    private boolean renderPassOpen;

    @Shadow
    @Nullable
    private ShaderProgram currentProgram;

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormat()Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private VertexFormat onDrawObjectWithRenderPassGetVertexFormat(RenderPipeline instance, Operation<VertexFormat> original, RenderPassImpl pass) {
        var vertexFormat = ((RenderPassExtInternal) pass).blazerod$getVertexFormat();
        if (vertexFormat != null) {
            return vertexFormat;
        } else {
            return original.call(instance);
        }
    }

    @WrapOperation(method = "drawObjectWithRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$DrawMode;"))
    private VertexFormat.DrawMode onDrawObjectWithRenderPassGetVertexMode(RenderPipeline instance, Operation<VertexFormat.DrawMode> original, RenderPassImpl pass) {
        var vertexFormatMode = ((RenderPassExtInternal) pass).blazerod$getVertexFormatMode();
        if (vertexFormatMode != null) {
            return vertexFormatMode;
        } else {
            return original.call(instance);
        }
    }

    @Unique
    private static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Lnet/minecraft/client/texture/NativeImage;IIIIIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
    private int onSetTextureUnpackAlignmentNativeImage(int param) {
        if (!isPowerOfTwo(param)) {
            return 1;
        }
        return param;
    }

    @ModifyArg(method = "writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Ljava/nio/IntBuffer;Lnet/minecraft/client/texture/NativeImage$Format;IIIIII)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_pixelStore(II)V", ordinal = 3), index = 1)
    private int onSetTextureUnpackAlignmentIntBuffer(int param) {
        if (!isPowerOfTwo(param)) {
            return 1;
        }
        return param;
    }

    @ModifyExpressionValue(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$UniformDescription;type()Lnet/minecraft/client/gl/UniformType;", ordinal = 2))
    private UniformType onCheckTexelBufferUniformSlice(UniformType original, @Local GpuBufferSlice gpuBufferSlice, @Local RenderPipeline.UniformDescription uniformDescription) {
        if (original != UniformType.TEXEL_BUFFER) {
            return original;
        }
        if (!((GpuDeviceExt) backend).blazerod$supportTextureBufferSlice()) {
            if (gpuBufferSlice.offset() != 0 || gpuBufferSlice.length() != gpuBufferSlice.buffer().size()) {
                LOGGER.error("Unsupported uniform slice {}", uniformDescription.name());
            }
            return original;
        }
        return null;
    }

    @WrapWithCondition(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glTexBuffer(III)V"))
    private boolean onSetTexelBufferUniformSlice(int target, int internalformat, int buffer, @Local GpuBufferSlice slice) {
        if (slice.offset() != 0 || slice.length() != slice.buffer().size()) {
            ARBTextureBufferRange.glTexBufferRange(target, internalformat, buffer, slice.offset(), slice.length());
            return false;
        }
        return true;
    }

    @Inject(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"))
    private void afterClearSimpleUniforms(RenderPassImpl pass, Collection<String> validationSkippedUniforms, CallbackInfoReturnable<Boolean> cir) {
        var renderPipeline = pass.pipeline.info();
        var shaderProgram = pass.pipeline.program();
        var passExt = (RenderPassExtInternal) pass;
        var shaderProgramExt = (ShaderProgramExtInternal) shaderProgram;
        var shaderStorageBuffers = shaderProgramExt.blazerod$getStorageBuffers();
        var passStorageBuffers = passExt.blazerod$getStorageBuffers();

        if (RenderPassImpl.IS_DEVELOPMENT) {
            for (var name : shaderStorageBuffers.keySet()) {
                var entry = passStorageBuffers.get(name);
                if (entry == null) {
                    throw new IllegalStateException("Missing ssbo " + name);
                }
                var glBuffer = entry.buffer();
                var glBufferExt = (GpuBufferExt) glBuffer;
                if ((glBufferExt.blazerod$getExtraUsage() & GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER) == 0) {
                    throw new IllegalStateException("Storage buffer " + name + " must have GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER");
                }
            }
        }

        for (var entry : passStorageBuffers.entrySet()) {
            var name = entry.getKey();
            var info = shaderStorageBuffers.get(name);
            if (info == null) {
                throw new IllegalStateException("Missing ssbo " + name + " for pipeline" + renderPipeline.toString());
            }
            var slice = entry.getValue();
            var glBuffer = (GlGpuBuffer) slice.buffer();
            GL32.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, info.binding(), glBuffer.id, slice.offset(), slice.length());
        }
    }

    @Override
    public GlBackend blazerod$getBackend() {
        return backend;
    }

    @Override
    public ComputePass blazerod$createComputePass(Supplier<String> label) {
        if (!((GpuDeviceExt) backend).blazerod$supportComputeShader()) {
            throw new IllegalStateException("Compute shader is not supported");
        }
        if (renderPassOpen) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        renderPassOpen = true;
        backend.getDebugLabelManager().pushDebugGroup(label);
        return new ComputePassImpl((GlCommandEncoder) (Object) this);
    }

    @Unique
    private boolean setupComputePass(ComputePass computePass, Collection<String> validationSkippedUniforms) {
        var pass = (ComputePassImpl) computePass;
        var pipeline = pass.getPipeline();
        if (RenderPassImpl.IS_DEVELOPMENT) {
            if (pipeline == null) {
                throw new IllegalStateException("Can't dispatch without a compute pipeline");
            }

            var shaderProgram = pipeline.program();
            if (shaderProgram == ShaderProgram.INVALID) {
                throw new IllegalStateException("Pipeline contains invalid shader program");
            }

            var pipelineInfo = pipeline.info();
            for (var uniformDesc : pipelineInfo.getUniforms()) {
                var uniformName = uniformDesc.name();
                var bufferSlice = pass.simpleUniforms.get(uniformName);
                if (!validationSkippedUniforms.contains(uniformName)) {
                    if (bufferSlice == null) {
                        throw new IllegalStateException("Missing uniform " + uniformName + " (should be " + uniformDesc.type() + ")");
                    }

                    var buffer = bufferSlice.buffer();
                    if (uniformDesc.type() == UniformType.UNIFORM_BUFFER) {
                        if (buffer.isClosed()) {
                            throw new IllegalStateException("Uniform buffer " + uniformName + " is already closed");
                        }
                        if ((buffer.usage() & GpuBuffer.USAGE_UNIFORM) == 0) {
                            throw new IllegalStateException("Uniform buffer " + uniformName + " must have GpuBuffer.USAGE_UNIFORM");
                        }
                    }

                    if (uniformDesc.type() == UniformType.TEXEL_BUFFER) {
                        var tboSliceSupported = ((GpuDeviceExt) backend).blazerod$supportTextureBufferSlice();
                        if (!tboSliceSupported && (bufferSlice.offset() != 0 || bufferSlice.length() != buffer.size())) {
                            throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                        }
                        if (uniformDesc.textureFormat() == null) {
                            throw new IllegalStateException("Invalid uniform texel buffer " + uniformName + " (missing a texture format)");
                        }
                    }
                }
            }

            var programUniforms = shaderProgram.getUniforms();
            for (var entry : programUniforms.entrySet()) {
                if (entry.getValue() instanceof GlUniform.Sampler) {
                    var samplerName = entry.getKey();
                    var textureView = (GlTextureView) pass.samplerUniforms.get(samplerName);
                    if (textureView == null) {
                        throw new IllegalStateException("Missing sampler " + samplerName);
                    }

                    var texture = textureView.texture();
                    if (textureView.isClosed()) {
                        throw new IllegalStateException("Sampler " + samplerName + " (" + texture.getLabel() + ") has been closed!");
                    }
                    if ((texture.usage() & GpuTexture.USAGE_TEXTURE_BINDING) == 0) {
                        throw new IllegalStateException("Sampler " + samplerName + " (" + texture.getLabel() + ") must have USAGE_TEXTURE_BINDING!");
                    }
                }
            }

            var storageBufferInfos = ((ShaderProgramExtInternal) shaderProgram).blazerod$getStorageBuffers();
            for (var name : storageBufferInfos.keySet()) {
                var bufferEntry = pass.storageBuffers.get(name);
                if (bufferEntry == null) {
                    throw new IllegalStateException("Missing ssbo " + name);
                }
                var glBuffer = bufferEntry.buffer();
                var bufferExt = (GpuBufferExt) glBuffer;
                if ((bufferExt.blazerod$getExtraUsage() & GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER) == 0) {
                    throw new IllegalStateException("Storage buffer " + name + " must have GpuBufferExt.EXTRA_USAGE_STORAGE_BUFFER");
                }
            }
        } else if (pipeline == null || pipeline.program() == ShaderProgram.INVALID) {
            return false;
        }

        var currentShaderProgram = pipeline.program();
        var programChanged = this.currentProgram != currentShaderProgram;
        if (programChanged) {
            var programRef = currentShaderProgram.getGlRef();
            GlStateManager._glUseProgram(programRef);
            this.currentProgram = currentShaderProgram;
        }

        var shaderUniforms = currentShaderProgram.getUniforms();
        for (var uniformEntry : shaderUniforms.entrySet()) {
            var uniformName = uniformEntry.getKey();
            var isSimpleUniform = pass.setSimpleUniforms.contains(uniformName);

            switch ((GlUniform) uniformEntry.getValue()) {
                case GlUniform.UniformBuffer(var blockBinding) -> {
                    if (isSimpleUniform) {
                        var bufferSlice = pass.simpleUniforms.get(uniformName);
                        var glBuffer = (GlGpuBuffer) bufferSlice.buffer();
                        GL32.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, blockBinding,
                                glBuffer.id, bufferSlice.offset(), bufferSlice.length());
                    }
                }
                case GlUniform.TexelBuffer(var location, var samplerIndex, var format, var texture) -> {
                    if (programChanged || isSimpleUniform) {
                        GlStateManager._glUniform1i(location, samplerIndex);
                    }
                    var activeTextureUnit = GL13C.GL_TEXTURE0 + samplerIndex;
                    GlStateManager._activeTexture(activeTextureUnit);
                    GL11C.glBindTexture(GL31C.GL_TEXTURE_BUFFER, texture);
                    if (isSimpleUniform) {
                        var bufferSlice = pass.simpleUniforms.get(uniformName);
                        var buffer = bufferSlice.buffer();
                        var glBuffer = (GlGpuBuffer) buffer;
                        var glInternalFormat = GlConst.toGlInternalId(format);

                        if (bufferSlice.offset() != 0 || bufferSlice.length() != buffer.size()) {
                            ARBTextureBufferRange.glTexBufferRange(GL31C.GL_TEXTURE_BUFFER,
                                    glInternalFormat, glBuffer.id, bufferSlice.offset(), bufferSlice.length());
                        } else {
                            GL31.glTexBuffer(GL31C.GL_TEXTURE_BUFFER, glInternalFormat, glBuffer.id);
                        }
                    }
                }
                case GlUniform.Sampler(var location, var samplerIndex) -> {
                    var textureView = (GlTextureView) pass.samplerUniforms.get(uniformName);
                    if (textureView == null) break;

                    if (programChanged || isSimpleUniform) {
                        GlStateManager._glUniform1i(location, samplerIndex);
                    }
                    var activeTextureUnit = GL13C.GL_TEXTURE0 + samplerIndex;
                    GlStateManager._activeTexture(activeTextureUnit);
                    var texture = textureView.texture();
                    var textureTarget = (texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0
                            ? GL13C.GL_TEXTURE_CUBE_MAP
                            : GL11C.GL_TEXTURE_2D;

                    if (textureTarget == GL13C.GL_TEXTURE_CUBE_MAP) {
                        GL11.glBindTexture(textureTarget, texture.getGlId());
                    } else {
                        GlStateManager._bindTexture(texture.getGlId());
                    }

                    var baseMip = textureView.baseMipLevel();
                    var maxLevel = baseMip + textureView.mipLevels() - 1;
                    GlStateManager._texParameter(textureTarget, GL12C.GL_TEXTURE_BASE_LEVEL, baseMip);
                    GlStateManager._texParameter(textureTarget, GL12C.GL_TEXTURE_MAX_LEVEL, maxLevel);
                    texture.checkDirty(textureTarget);
                }
                default -> throw new MatchException(null, null);
            }
        }

        var ssboProgram = pipeline.program();
        var ssboBindings = ((ShaderProgramExtInternal) ssboProgram).blazerod$getStorageBuffers();
        for (var bufferEntry : pass.storageBuffers.entrySet()) {
            var bufferName = bufferEntry.getKey();
            var bindingInfo = ssboBindings.get(bufferName);
            if (bindingInfo == null) {
                throw new IllegalStateException("Missing ssbo " + bufferName + " for pipeline" + pipeline);
            }
            var bufferSlice = bufferEntry.getValue();
            var glBuffer = (GlGpuBuffer) bufferSlice.buffer();
            GL32.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER,
                    bindingInfo.binding(), glBuffer.id, bufferSlice.offset(), bufferSlice.length());
        }

        pass.setSimpleUniforms.clear();
        return true;
    }

    @Override
    public void blazerod$dispatchCompute(ComputePass pass, int x, int y, int z) {
        if (!setupComputePass(pass, Collections.emptyList())) {
            return;
        }
        if (RenderPassImpl.IS_DEVELOPMENT) {
            if (x <= 0) {
                throw new IllegalArgumentException("work group x must be positive");
            }
            if (y <= 0) {
                throw new IllegalArgumentException("work group y must be positive");
            }
            if (z <= 0) {
                throw new IllegalArgumentException("work group z must be positive");
            }
        }
        ARBComputeShader.glDispatchCompute(x, y, z);
    }

    @Override
    public void blazerod$memoryBarrier(int barriers) {
        if (!((GpuDeviceExt) backend).blazerod$supportMemoryBarrier()) {
            throw new IllegalStateException("Memory barrier is not supported");
        }
        var bits = 0;
        if ((barriers & CommandEncoderExt.BARRIER_STORAGE_BUFFER_BIT) != 0) {
            if (!((GpuDeviceExt) backend).blazerod$supportSsbo()) {
                throw new IllegalStateException("Shader storage buffer is not supported");
            }
            bits |= ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BARRIER_BIT;
        }
        if ((barriers & CommandEncoderExt.BARRIER_VERTEX_BUFFER_BIT) != 0) {
            bits |= ARBShaderImageLoadStore.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT;
        }
        if ((barriers & CommandEncoderExt.BARRIER_INDEX_BUFFER_BIT) != 0) {
            bits |= ARBShaderImageLoadStore.GL_ELEMENT_ARRAY_BARRIER_BIT;
        }
        if ((barriers & CommandEncoderExt.BARRIER_TEXTURE_FETCH_BIT) != 0) {
            bits |= ARBShaderImageLoadStore.GL_TEXTURE_FETCH_BARRIER_BIT;
        }
        if ((barriers & CommandEncoderExt.BARRIER_UNIFORM_BIT) != 0) {
            bits |= ARBShaderImageLoadStore.GL_UNIFORM_BARRIER_BIT;
        }
        ARBShaderImageLoadStore.glMemoryBarrier(bits);
    }
}
