package top.fifthlight.blazerod.runtime.uniform

import top.fifthlight.blazerod.layout.GpuDataLayout
import top.fifthlight.blazerod.layout.LayoutStrategy

object UnlitDataUniformBuffer : UniformBuffer<UnlitDataUniformBuffer, UnlitDataUniformBuffer.UnlitDataLayout>(
    name = "UnlitDataUniformBuffer",
) {
    override val layout: UnlitDataLayout
        get() = UnlitDataLayout

    object UnlitDataLayout : GpuDataLayout<UnlitDataLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std140LayoutStrategy
        var baseColor by rgba()
    }
}