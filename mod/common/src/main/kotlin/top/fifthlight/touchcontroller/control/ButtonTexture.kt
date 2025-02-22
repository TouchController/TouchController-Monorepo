package top.fifthlight.touchcontroller.control

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.data.IntPadding
import top.fifthlight.touchcontroller.assets.EmptyTexture

@Serializable
sealed class ButtonTexture {
    @Serializable
    @SerialName("empty")
    data class Empty(
        val extraPadding: IntPadding = IntPadding.ZERO,
    ) : ButtonTexture()

    @Serializable
    @SerialName("color")
    data class Fill(
        val borderWidth: Int = 0,
        val extraPadding: IntPadding = IntPadding.ZERO,
        val borderColor: Color = Colors.WHITE,
        val backgroundColor: Color = Colors.BLACK,
    ) : ButtonTexture()

    @Serializable
    @SerialName("fixed")
    data class Fixed(
        val texture: TextureCoordinate,
        val scale: Float = 2f,
    ) : ButtonTexture()

    @Serializable
    @SerialName("nine-patch")
    data class NinePatch(
        val texture: EmptyTexture = EmptyTexture.EMPTY_1,
        val extraPadding: IntPadding = IntPadding.ZERO,
    ) : ButtonTexture()
}

@Serializable
sealed class ButtonActiveTexture {
    @Serializable
    @SerialName("same")
    data object Same : ButtonActiveTexture()

    @Serializable
    @SerialName("gray")
    data object Gray : ButtonActiveTexture()

    @Serializable
    @SerialName("texture")
    data class Texture(
        val texture: ButtonTexture
    ) : ButtonActiveTexture()
}