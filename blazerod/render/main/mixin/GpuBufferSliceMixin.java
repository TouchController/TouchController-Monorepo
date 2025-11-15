package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GpuBufferSlice.class)
public abstract class GpuBufferSliceMixin {
    // Fix bug: newOffset + newLength should be less or *equals* to original length
    @Redirect(method = "slice", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;length:I"))
    public int getLength(GpuBufferSlice instance) {
        return instance.length() + 1;
    }
}