package top.fifthlight.blazerod.render

import com.mojang.blaze3d.vertex.VertexFormatElement
import top.fifthlight.mergetools.api.ExpectFactory

@Suppress("PropertyName")
interface IrisApiWrapper {
    val ENTITY_ID_ELEMENT: VertexFormatElement
    val MID_TEXTURE_ELEMENT: VertexFormatElement
    val TANGENT_ELEMENT: VertexFormatElement
    val shaderPackInUse: Boolean

    @ExpectFactory
    interface Factory {
        fun create(): IrisApiWrapper
    }
}
