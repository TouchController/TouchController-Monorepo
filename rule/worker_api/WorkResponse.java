package top.fifthlight.bazel.worker.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkResponse(@JsonProperty("exitCode") int exitCode, @JsonProperty("output") String output,
                           @JsonProperty("requestId") int requestId) {
}