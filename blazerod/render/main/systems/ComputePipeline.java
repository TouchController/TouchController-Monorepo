package top.fifthlight.blazerod.systems;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;

import java.util.*;

public class ComputePipeline {
    private final Identifier location;
    private final Identifier computeShader;
    private final Defines shaderDefines;
    private final List<String> samplers;
    private final List<RenderPipeline.UniformDescription> uniforms;
    private final Set<String> storageBuffers;

    protected ComputePipeline(
            Identifier location,
            Identifier computeShader,
            Defines shaderdefines,
            List<String> samplers,
            List<RenderPipeline.UniformDescription> uniforms,
            Set<String> storageBuffers
    ) {
        this.location = location;
        this.computeShader = computeShader;
        this.shaderDefines = shaderdefines;
        this.samplers = samplers;
        this.uniforms = uniforms;
        this.storageBuffers = storageBuffers;
    }

    public static ComputePipeline.Builder builder(ComputePipeline.Snippet... snippets) {
        var computepipeline$builder = new ComputePipeline.Builder();

        for (var snippet : snippets) {
            computepipeline$builder.withSnippet(snippet);
        }

        return computepipeline$builder;
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    public Identifier getLocation() {
        return this.location;
    }

    public Identifier getComputeShader() {
        return this.computeShader;
    }

    public Defines getShaderDefines() {
        return this.shaderDefines;
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<RenderPipeline.UniformDescription> getUniforms() {
        return this.uniforms;
    }

    public Set<String> getStorageBuffers() {
        return this.storageBuffers;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder {
        private Optional<Identifier> location = Optional.empty();
        private Optional<Identifier> computeShader = Optional.empty();
        private Optional<Defines.Builder> definesBuilder = Optional.empty();
        private Optional<List<String>> samplers = Optional.empty();
        private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
        private Optional<Set<String>> storageBuffers = Optional.empty();

        private Builder() {
        }

        public ComputePipeline.Builder withLocation(Identifier Identifier) {
            this.location = Optional.of(Identifier);
            return this;
        }

        public ComputePipeline.Builder withComputeShader(Identifier Identifier) {
            this.computeShader = Optional.of(Identifier);
            return this;
        }

        public ComputePipeline.Builder withShaderDefine(String flag) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            this.definesBuilder.get().flag(flag);
            return this;
        }

        public ComputePipeline.Builder withShaderDefine(String name, int value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            this.definesBuilder.get().define(name, value);
            return this;
        }

        public ComputePipeline.Builder withShaderDefine(String name, float value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            this.definesBuilder.get().define(name, value);
            return this;
        }

        public ComputePipeline.Builder withSampler(String sampler) {
            if (this.samplers.isEmpty()) {
                this.samplers = Optional.of(new ArrayList<>());
            }

            this.samplers.get().add(sampler);
            return this;
        }

        public ComputePipeline.Builder withUniform(String name, UniformType uniformtype) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList<>());
            }

            if (uniformtype == UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
            } else {
                this.uniforms.get().add(new RenderPipeline.UniformDescription(name, uniformtype));
                return this;
            }
        }

        public ComputePipeline.Builder withUniform(String name, UniformType uniformtype, TextureFormat textureformat) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList<>());
            }

            if (uniformtype != UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Only texel buffer can specify texture format");
            } else {
                this.uniforms.get().add(new RenderPipeline.UniformDescription(name, textureformat));
                return this;
            }
        }

        public ComputePipeline.Builder withStorageBuffer(String name) {
            if (this.storageBuffers.isEmpty()) {
                this.storageBuffers = Optional.of(new HashSet<>());
            }

            this.storageBuffers.get().add(name);
            return this;
        }

        void withSnippet(ComputePipeline.Snippet snippet) {
            if (snippet.computeShader.isPresent()) {
                this.computeShader = snippet.computeShader;
            }

            if (snippet.shaderDefines.isPresent()) {
                if (this.definesBuilder.isEmpty()) {
                    this.definesBuilder = Optional.of(Defines.builder());
                }

                var shaderdefines = snippet.shaderDefines.get();

                for (var entry : shaderdefines.values().entrySet()) {
                    this.definesBuilder.get().define(entry.getKey(), entry.getValue());
                }

                for (var flag : shaderdefines.flags()) {
                    this.definesBuilder.get().flag(flag);
                }
            }

            snippet.samplers.ifPresent(list -> {
                if (this.samplers.isPresent()) {
                    this.samplers.get().addAll(list);
                } else {
                    this.samplers = Optional.of(new ArrayList<>(list));
                }
            });
            snippet.uniforms.ifPresent(list -> {
                if (this.uniforms.isPresent()) {
                    this.uniforms.get().addAll(list);
                } else {
                    this.uniforms = Optional.of(new ArrayList<>(list));
                }
            });
            snippet.storageBuffers.ifPresent(list -> {
                if (this.storageBuffers.isPresent()) {
                    this.storageBuffers.get().addAll(list);
                } else {
                    this.storageBuffers = Optional.of(new HashSet<>(list));
                }
            });
        }

        public ComputePipeline.Snippet buildSnippet() {
            return new ComputePipeline.Snippet(
                    this.computeShader,
                    this.definesBuilder.map(Defines.Builder::build),
                    this.samplers.map(Collections::unmodifiableList),
                    this.uniforms.map(Collections::unmodifiableList),
                    this.storageBuffers.map(Collections::unmodifiableSet)
            );
        }

        public ComputePipeline build() {
            if (this.location.isEmpty()) {
                throw new IllegalStateException("Missing location");
            } else if (this.computeShader.isEmpty()) {
                throw new IllegalStateException("Missing compute shader");
            } else {
                return new ComputePipeline(
                        this.location.get(),
                        this.computeShader.get(),
                        this.definesBuilder.orElse(Defines.builder()).build(),
                        List.copyOf(this.samplers.orElse(new ArrayList<>())),
                        this.uniforms.orElse(Collections.emptyList()),
                        Set.copyOf(this.storageBuffers.orElse(new HashSet<>()))
                );
            }
        }
    }

    public record Snippet(
            Optional<Identifier> computeShader,
            Optional<Defines> shaderDefines,
            Optional<List<String>> samplers,
            Optional<List<RenderPipeline.UniformDescription>> uniforms,
            Optional<Set<String>> storageBuffers
    ) {
    }
}