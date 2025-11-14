package top.fifthlight.touchcontroller.common.control

import kotlinx.serialization.Serializable
import top.fifthlight.combine.data.Texture
import top.fifthlight.touchcontroller.assets.TextureSet

@Serializable
data class TextureCoordinate(
    val textureSet: TextureSet.TextureSetKey = TextureSet.TextureSetKey.CLASSIC,
    val textureItem: TextureSet.TextureKey = TextureSet.TextureKey.Up,
) {
    val texture: Texture
        get() = textureItem.get(textureSet.textureSet)
}