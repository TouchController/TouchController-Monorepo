package top.fifthlight.blazerod.extension.internal.gl;

import top.fifthlight.blazerod.extension.GpuDeviceExt;
import top.fifthlight.blazerod.systems.ComputePipeline;
import top.fifthlight.blazerod.systems.gl.CompiledComputePipeline;

public interface GpuDeviceExtInternal extends GpuDeviceExt {
    CompiledComputePipeline blazerod$compilePipelineCached(ComputePipeline pipeline);
}
