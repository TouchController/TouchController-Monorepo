package top.fifthlight.blazerod.extension.internal;

import top.fifthlight.blazerod.extension.RenderPipelineExt;

import java.util.Set;

public interface RenderPipelineExtInternal extends RenderPipelineExt {
    void blazerod$setStorageBuffers(Set<String> storageBuffers);
}
