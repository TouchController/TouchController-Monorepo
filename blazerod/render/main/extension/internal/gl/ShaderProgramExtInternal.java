package top.fifthlight.blazerod.extension.internal.gl;

import java.util.Map;
import java.util.Set;

public interface ShaderProgramExtInternal {
    void blazerod$setStorageBuffers(Set<String> storageBuffers);

    Map<String, GlStorageBuffer> blazerod$getStorageBuffers();

    record GlStorageBuffer(String name, int binding) {
    }
}
