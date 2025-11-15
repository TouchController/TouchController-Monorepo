package top.fifthlight.blazerod.model

import org.joml.Vector4f

data class RgbaColor(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float,
) {
    constructor(rgba: FloatArray): this(rgba[0], rgba[1], rgba[2], rgba[3])
    constructor(rgbColor: RgbColor): this(rgbColor.r, rgbColor.g, rgbColor.b, 1f)
}

fun RgbaColor.toVector4f(vector4f: Vector4f): Vector4f = vector4f.set(r, g, b, a)

data class RgbColor(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    constructor(rgba: FloatArray): this(rgba[0], rgba[1], rgba[2])
}
