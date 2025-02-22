package top.fifthlight.touchcontroller.control

import kotlinx.serialization.Serializable
import top.fifthlight.combine.data.Texture
import top.fifthlight.touchcontroller.assets.TextureSet

@Serializable
data class TextureCoordinate(
    val textureSet: TextureSet.TextureSetKey,
    val textureItem: TextureSet.TextureKey,
) {
    val texture: Texture
        get() = textureItem.get(textureSet.textureSet)
}