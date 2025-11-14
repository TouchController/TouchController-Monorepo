package top.fifthlight.bazel.worker.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WorkRequest(@JsonProperty("arguments") List<String> arguments, @JsonProperty("inputs") List<Input> inputs,
                          @JsonProperty("requestId") int requestId) {
    public record Input(@JsonProperty("path") String path, @JsonProperty("digest") String digest) {
    }
}
