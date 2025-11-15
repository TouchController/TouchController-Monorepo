package top.fifthlight.blazerod.extension.internal;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fifthlight.blazerod.extension.RenderPassExt;

import java.util.Map;

public interface RenderPassExtInternal extends RenderPassExt {
    @Nullable
    VertexFormat blazerod$getVertexFormat();

    @Nullable
    VertexFormat.DrawMode blazerod$getVertexFormatMode();

    @NotNull
    Map<String, GpuBufferSlice> blazerod$getStorageBuffers();
}
