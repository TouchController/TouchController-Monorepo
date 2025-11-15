package top.fifthlight.blazerod.model.pmx.format

import top.fifthlight.blazerod.model.RgbColor
import top.fifthlight.blazerod.model.RgbaColor

internal data class PmxMaterial(
    val nameLocal: String,
    val nameUniversal: String,
    val diffuseColor: RgbaColor,
    val specularColor: RgbColor,
    val specularStrength: Float,
    val ambientColor: RgbColor,
    val drawingFlags: DrawingFlags,
    val edgeColor: RgbaColor,
    val edgeScale: Float,
    val textureIndex: Int,
    val environmentIndex: Int,
    val environmentBlendMode: EnvironmentBlendMode,
    val toonReference: ToonReference,
    val metadata: String,
    val surfaceCount: Int,
) {
    data class DrawingFlags(
        val noCull: Boolean,
        val groundShadow: Boolean,
        val drawShadow: Boolean,
        val receiveShadow: Boolean,
        val hasEdge: Boolean,
        val vertexColor: Boolean,
        val pointDrawing: Boolean,
        val lineDrawing: Boolean,
    )

    enum class EnvironmentBlendMode {
        DISABLED,
        MULTIPLY,
        ADDICTIVE,
        ADDITIONAL_VEC4,
    }

    sealed class ToonReference {
        data class Internal(val index: UByte): ToonReference()
        data class Texture(val index: Int): ToonReference()
    }
}
