package top.fifthlight.combine.paint

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ColorSerializer::class)
@JvmInline
value class Color(val value: Int) {
    val a: Int
        get() = (value shr 24) and 0xFF
    val r: Int
        get() = (value shr 16) and 0xFF
    val g: Int
        get() = (value shr 8) and 0xFF
    val b: Int
        get() = value and 0xFF

    val aFloat: Float
        get() = a / 255f
    val rFloat: Float
        get() = r / 255f
    val gFloat: Float
        get() = g / 255f
    val bFloat: Float
        get() = b / 255f

    fun toHsv(): HsvColor {
        val r = rFloat
        val g = gFloat
        val b = bFloat

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        var h = 0f
        if (delta != 0f) {
            h = when (max) {
                r -> ((g - b) / delta) * 60f
                g -> ((b - r) / delta + 2f) * 60f
                b -> ((r - g) / delta + 4f) * 60f
                else -> 0f
            }
            if (h < 0) h += 360f
        }

        val s = if (max == 0f) 0f else delta / max
        val v = max

        return HsvColor(a, h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    }

    override fun toString(): String {
        fun Int.toHex() = toString(16).uppercase().padStart(2, '0')
        return "#${a.toHex()}${r.toHex()}${g.toHex()}${b.toHex()}"
    }

    operator fun times(other: Color) = Color(
        a = aFloat * other.aFloat,
        r = rFloat * other.rFloat,
        g = gFloat * other.gFloat,
        b = bFloat * other.bFloat,
    )

    companion object {
        private val COLOR_PATTERN = Regex("^(?:0x|#)([A-Fa-f0-9]{3,8})$")

        fun parse(string: String): Color? {
            val matchResult = COLOR_PATTERN.matchEntire(string) ?: return null
            val hex = matchResult.groupValues[1]

            return when (hex.length) {
                3 -> {
                    // RGB
                    val r = hex.substring(0, 1).repeat(2).toInt(16)
                    val g = hex.substring(1, 2).repeat(2).toInt(16)
                    val b = hex.substring(2, 3).repeat(2).toInt(16)
                    Color(r, g, b)
                }

                4 -> {
                    // ARGB
                    val a = hex.substring(0, 1).repeat(2).toInt(16)
                    val r = hex.substring(1, 2).repeat(2).toInt(16)
                    val g = hex.substring(2, 3).repeat(2).toInt(16)
                    val b = hex.substring(3, 4).repeat(2).toInt(16)
                    Color(a, r, g, b)
                }

                6 -> {
                    // RRGGBB
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    Color(r, g, b)
                }

                8 -> {
                    // AARRGGBB
                    val a = hex.substring(0, 2).toInt(16)
                    val r = hex.substring(2, 4).toInt(16)
                    val g = hex.substring(4, 6).toInt(16)
                    val b = hex.substring(6, 8).toInt(16)
                    Color(a, r, g, b)
                }

                else -> null
            }
        }
    }
}

fun Color(value: UInt) = Color(value.toInt())

fun Color(a: Int, r: Int, g: Int, b: Int) = Color(
    (a.coerceIn(0 until 256) shl 24)
            or (r.coerceIn(0 until 256) shl 16)
            or (g.coerceIn(0 until 256) shl 8)
            or b.coerceIn(0 until 256)
)

fun Color(r: Int, g: Int, b: Int) = Color(0xFF, r, g, b)

fun Color(r: Float, g: Float, b: Float) = Color(
    a = 0xFF,
    r = (r.coerceIn(0f, 1f) * 255).toInt(),
    g = (g.coerceIn(0f, 1f) * 255).toInt(),
    b = (b.coerceIn(0f, 1f) * 255).toInt()
)

fun Color(a: Int, r: Float, g: Float, b: Float) = Color(
    a = a.coerceIn(0, 255),
    r = (r.coerceIn(0f, 1f) * 255).toInt(),
    g = (g.coerceIn(0f, 1f) * 255).toInt(),
    b = (b.coerceIn(0f, 1f) * 255).toInt()
)


fun Color(a: Float, r: Float, g: Float, b: Float) = Color(
    a = (a.coerceIn(0f, 1f) * 255).toInt(),
    r = (r.coerceIn(0f, 1f) * 255).toInt(),
    g = (g.coerceIn(0f, 1f) * 255).toInt(),
    b = (b.coerceIn(0f, 1f) * 255).toInt()
)

data class HsvColor(
    val a: Int = 0xFF,
    val h: Float,
    val s: Float,
    val v: Float,
) {
    fun toColor(): Color {
        val h = h % 360f
        val s = s.coerceIn(0f, 1f)
        val v = v.coerceIn(0f, 1f)

        if (s <= 0f) {
            val value = (v * 255).toInt().coerceIn(0, 255)
            return Color(a, value, value, value)
        }

        val sector = (h / 60f).toInt() % 6
        val fraction = (h / 60f) - sector

        val p = v * (1 - s)
        val q = v * (1 - s * fraction)
        val t = v * (1 - s * (1 - fraction))

        val (r, g, b) = when (sector) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            5 -> Triple(v, p, q)
            else -> Triple(0f, 0f, 0f)
        }

        return Color(
            a.coerceIn(0, 255),
            (r * 255).toInt().coerceIn(0, 255),
            (g * 255).toInt().coerceIn(0, 255),
            (b * 255).toInt().coerceIn(0, 255)
        )
    }

    override fun toString(): String {
        return "HsvColor(h=$h, s=$s, v=$v, a=0x${a.toString(16).uppercase().padStart(2, '0')})"
    }
}

fun HsvColor(h: Float, s: Float, v: Float) = HsvColor(255, h, s, v)

private class ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("top.fifthlight.combine.paint.Color", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.value)
    override fun deserialize(decoder: Decoder) = Color(decoder.decodeInt())
}

object Colors {
    val WHITE = Color(0xFFFFFFFFu)
    val ALTERNATE_WHITE = Color(0xFFBABABAu)
    val SECONDARY_WHITE = Color(0xFF8E8E8Eu)
    val TRANSPARENT_WHITE = Color(0x88FFFFFFu)
    val TRANSPARENT_BLACK = Color(0x88000000u)
    val BLACK = Color(0xFF000000u)
    val GRAY = Color(0xFF808080u)
    val LIGHT_GRAY = Color(0xFFA0A0A0u)
    val RED = Color(0xFFFF0000u)
    val GREEN = Color(0xFF00FF00u)
    val BLUE = Color(0xFF0000FFu)
    val YELLOW = Color(0xFFFFFF00u)
    val MAGENTA = Color(0xFFFF00FFu)
    val CYAN = Color(0xFF00FFFFu)
    val TRANSPARENT = Color(0x00000000u)
}
