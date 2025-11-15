package top.fifthlight.blazerod.extension.internal;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.extension.RenderPipelineSnippetExt;

import java.util.Set;

public interface RenderPipelineSnippetExtInternal extends RenderPipelineSnippetExt {
    void blazerod$setStorageBuffers(@NotNull Set<String> storageBuffers);
}
