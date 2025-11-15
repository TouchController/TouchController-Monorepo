package top.fifthlight.blazerod.runtime.uniform

import top.fifthlight.blazerod.layout.GpuDataLayout
import top.fifthlight.blazerod.layout.LayoutStrategy

object MorphDataUniformBuffer : UniformBuffer<MorphDataUniformBuffer, MorphDataUniformBuffer.MorphDataLayout>(
    name = "MorphDataUniformBuffer",
) {
    override val layout: MorphDataLayout
        get() = MorphDataLayout

    object MorphDataLayout : GpuDataLayout<MorphDataLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std140LayoutStrategy
        var totalVertices by int()
        var posTargets by int()
        var colorTargets by int()
        var texCoordTargets by int()
        var totalTargets by int()
    }
}