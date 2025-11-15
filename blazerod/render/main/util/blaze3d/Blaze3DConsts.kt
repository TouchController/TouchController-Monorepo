package top.fifthlight.blazerod.util.blaze3d

import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import top.fifthlight.blazerod.extension.AddressModeExt
import top.fifthlight.blazerod.model.Accessor.ComponentType
import top.fifthlight.blazerod.model.Primitive
import top.fifthlight.blazerod.model.Texture

val Primitive.Mode.blaze3d
    get() = when (this) {
        Primitive.Mode.POINTS -> null
        Primitive.Mode.LINE_STRIP -> VertexFormat.DrawMode.LINE_STRIP
        Primitive.Mode.LINE_LOOP -> null
        Primitive.Mode.LINES -> VertexFormat.DrawMode.LINES
        Primitive.Mode.TRIANGLES -> VertexFormat.DrawMode.TRIANGLES
        Primitive.Mode.TRIANGLE_STRIP -> VertexFormat.DrawMode.TRIANGLE_STRIP
        Primitive.Mode.TRIANGLE_FAN -> VertexFormat.DrawMode.TRIANGLE_FAN
    }

val Primitive.Attributes.Key.usageName
    get() = when (this) {
        Primitive.Attributes.Key.POSITION -> "Position"
        Primitive.Attributes.Key.NORMAL -> "Normal"
        Primitive.Attributes.Key.TANGENT -> "Tangent"
        Primitive.Attributes.Key.TEXCOORD -> "UV0"
        Primitive.Attributes.Key.COLORS -> "Color"
        Primitive.Attributes.Key.JOINTS -> "Joint"
        Primitive.Attributes.Key.WEIGHTS -> "Weight"
    }

val ComponentType.blaze3d: VertexFormatElement.Type
    get() = when (this) {
        ComponentType.BYTE -> VertexFormatElement.Type.BYTE
        ComponentType.UNSIGNED_BYTE -> VertexFormatElement.Type.UBYTE
        ComponentType.SHORT -> VertexFormatElement.Type.SHORT
        ComponentType.UNSIGNED_SHORT -> VertexFormatElement.Type.USHORT
        ComponentType.UNSIGNED_INT -> VertexFormatElement.Type.UINT
        ComponentType.FLOAT -> VertexFormatElement.Type.FLOAT
    }

val Texture.Sampler.WrapMode.blaze3d: AddressMode
    get() = when (this) {
        Texture.Sampler.WrapMode.REPEAT -> AddressMode.REPEAT
        Texture.Sampler.WrapMode.MIRRORED_REPEAT -> AddressModeExt.MIRRORED_REPEAT
        Texture.Sampler.WrapMode.CLAMP_TO_EDGE -> AddressMode.CLAMP_TO_EDGE
    }

val Texture.Sampler.MinFilter.blaze3d: FilterMode
    get() = when (this) {
        Texture.Sampler.MinFilter.NEAREST -> FilterMode.NEAREST
        Texture.Sampler.MinFilter.LINEAR -> FilterMode.LINEAR
        Texture.Sampler.MinFilter.NEAREST_MIPMAP_NEAREST -> FilterMode.NEAREST
        Texture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST -> FilterMode.LINEAR
        Texture.Sampler.MinFilter.NEAREST_MIPMAP_LINEAR -> FilterMode.NEAREST
        Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR -> FilterMode.LINEAR
    }

val Texture.Sampler.MinFilter.useMipmap
    get() = when (this) {
        Texture.Sampler.MinFilter.NEAREST_MIPMAP_NEAREST,
        Texture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST,
        Texture.Sampler.MinFilter.NEAREST_MIPMAP_LINEAR,
        Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR,
            -> true

        else -> false
    }

val Texture.Sampler.MagFilter.blaze3d: FilterMode
    get() = when (this) {
        Texture.Sampler.MagFilter.NEAREST -> FilterMode.NEAREST
        Texture.Sampler.MagFilter.LINEAR -> FilterMode.LINEAR
    }
