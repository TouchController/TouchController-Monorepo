package top.fifthlight.blazerod.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.blazerod.extension.internal.RenderPipelineSnippetExtInternal;

import java.util.Set;

@Mixin(RenderPipeline.Snippet.class)
public abstract class RenderPipelineSnippetMixin implements RenderPipelineSnippetExtInternal {
    @Unique
    Set<String> storageBuffers;

    @Override
    public void blazerod$setStorageBuffers(@NotNull Set<String> storageBuffers) {
        this.storageBuffers = storageBuffers;
    }

    @Override
    @NotNull
    public Set<String> blazerod$getStorageBuffers() {
        return storageBuffers;
    }
}
