package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexFormat

fun RenderPipeline.Builder.withVertexFormat(format: VertexFormat) = also {
    (this as RenderPipelineBuilderExt).`blazerod$withVertexFormat`(format)
}

fun RenderPipeline.Builder.withStorageBuffer(name: String) = also {
    (this as RenderPipelineBuilderExt).`blazerod$withStorageBuffer`(name)
}
