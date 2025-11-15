package top.fifthlight.blazerod.mixin.gl;

import net.minecraft.client.gl.GlGpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.blazerod.extension.internal.GpuBufferExtInternal;

@Mixin(GlGpuBuffer.class)
public abstract class GlGpuBufferMixin implements GpuBufferExtInternal {
    @Unique
    private int extraUsage;

    @Override
    public int blazerod$getExtraUsage() {
        return extraUsage;
    }

    @Override
    public void blazerod$setExtraUsage(int extraUsage) {
        this.extraUsage = extraUsage;
    }
}