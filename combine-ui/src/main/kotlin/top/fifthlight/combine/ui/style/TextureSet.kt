package top.fifthlight.combine.ui.style

import androidx.compose.runtime.Immutable
import top.fifthlight.combine.data.NinePatchTexture
import top.fifthlight.combine.data.Texture

@Immutable
data class TextureSet(
    val normal: Texture,
    val focus: Texture,
    val hover: Texture,
    val active: Texture,
    val disabled: Texture,
)

@Immutable
data class NinePatchTextureSet(
    val normal: NinePatchTexture,
    val focus: NinePatchTexture,
    val hover: NinePatchTexture,
    val active: NinePatchTexture,
    val disabled: NinePatchTexture,
)
