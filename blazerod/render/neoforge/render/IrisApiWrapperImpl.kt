package top.fifthlight.blazerod.render

import com.mojang.blaze3d.vertex.VertexFormatElement
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.vertices.IrisVertexFormats
import net.neoforged.fml.ModList
import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl

@ActualImpl(IrisApiWrapper::class)
class IrisApiWrapperImpl @ActualConstructor("create") constructor() : IrisApiWrapper {
    override val ENTITY_ID_ELEMENT: VertexFormatElement
        get() = IrisVertexFormats.ENTITY_ID_ELEMENT
    override val MID_TEXTURE_ELEMENT: VertexFormatElement
        get() = IrisVertexFormats.MID_TEXTURE_ELEMENT
    override val TANGENT_ELEMENT: VertexFormatElement
        get() = IrisVertexFormats.TANGENT_ELEMENT

    private val irisApi = if (ModList.get().isLoaded("iris")) {
        IrisApi.getInstance()
    } else {
        null
    }

    override val shaderPackInUse: Boolean
        get() = irisApi?.isShaderPackInUse == true
}
