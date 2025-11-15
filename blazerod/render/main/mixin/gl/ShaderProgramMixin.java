package top.fifthlight.blazerod.mixin.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.extension.internal.gl.ShaderProgramExtInternal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(ShaderProgram.class)
public abstract class ShaderProgramMixin implements ShaderProgramExtInternal {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private int glRef;
    @Shadow
    @Final
    private String debugLabel;

    @Unique
    private Map<String, GlStorageBuffer> storageBuffers;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void afterInit(int glRef, String debugLabel, CallbackInfo ci) {
        // Iris extends ShaderProgram, so fallback here
        this.storageBuffers = Map.of();
    }

    @Override
    public void blazerod$setStorageBuffers(Set<String> storageBuffers) {
        var device = (GpuDeviceExt) RenderSystem.getDevice();
        if (!device.blazerod$supportSsbo() && !storageBuffers.isEmpty()) {
            throw new UnsupportedOperationException("Storage buffer is not supported");
        }

        var nextBinding = 0;
        var buffers = new HashMap<String, GlStorageBuffer>(storageBuffers.size());
        for (var name : storageBuffers) {
            var index = ARBProgramInterfaceQuery.glGetProgramResourceIndex(this.glRef, ARBProgramInterfaceQuery.GL_SHADER_STORAGE_BLOCK, name);
            if (index == GL43C.GL_INVALID_INDEX) {
                LOGGER.warn("{} shader program does not use storage buffer {} defined in the pipeline. This might be a bug.", this.debugLabel, name);
                continue;
            }
            var binding = nextBinding++;
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(this.glRef, index, binding);
            buffers.put(name, new GlStorageBuffer(name, binding));
        }

        int ssboSize;
        try (var stack = MemoryStack.stackPush()) {
            var ssbos = stack.mallocInt(1);
            ARBProgramInterfaceQuery.glGetProgramInterfaceiv(this.glRef, ARBProgramInterfaceQuery.GL_SHADER_STORAGE_BLOCK, ARBProgramInterfaceQuery.GL_ACTIVE_RESOURCES, ssbos);
            ssboSize = ssbos.get(0);
        }
        for (var i = 0; i < ssboSize; i++) {
            var name = ARBProgramInterfaceQuery.glGetProgramResourceName(this.glRef, ARBProgramInterfaceQuery.GL_SHADER_STORAGE_BLOCK, i);
            if (!buffers.containsKey(name)) {
                LOGGER.warn("Found unknown ssbo {} in {}", name, this.debugLabel);
            }
        }

        this.storageBuffers = buffers;
    }

    @Override
    public Map<String, GlStorageBuffer> blazerod$getStorageBuffers() {
        return storageBuffers;
    }
}
