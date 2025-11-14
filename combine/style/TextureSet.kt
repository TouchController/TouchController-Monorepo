package top.fifthlight.combine.ui.style

import top.fifthlight.combine.paint.Texture

data class TextureSet(
    val normal: Texture? = null,
    val focus: Texture? = normal,
    val hover: Texture? = focus,
    val active: Texture? = hover,
    val disabled: Texture? = normal,
) {
    companion object {
        val Empty = TextureSet()
    }
}
