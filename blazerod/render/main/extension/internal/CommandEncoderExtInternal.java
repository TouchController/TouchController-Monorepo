package top.fifthlight.blazerod.extension.internal;

import net.minecraft.client.gl.GlBackend;
import top.fifthlight.blazerod.extension.CommandEncoderExt;
import top.fifthlight.blazerod.systems.ComputePass;

public interface CommandEncoderExtInternal extends CommandEncoderExt {
    GlBackend blazerod$getBackend();

    void blazerod$dispatchCompute(ComputePass pass, int x, int y, int z);
}
