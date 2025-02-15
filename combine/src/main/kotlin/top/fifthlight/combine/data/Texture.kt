package top.fifthlight.combine.data

import androidx.compose.runtime.Immutable
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize

@Immutable
interface Texture {
    val size: IntSize
    val atlasOffset: IntOffset
}

@Immutable
interface NinePatchTexture : Texture {
    val scaleArea: IntRect
    val padding: IntPadding
}

private data class TextureImpl(
    override val size: IntSize,
    override val atlasOffset: IntOffset,
) : Texture

private data class NinePatchTextureImpl(
    override val size: IntSize,
    override val atlasOffset: IntOffset,
    override val scaleArea: IntRect,
    override val padding: IntPadding,
) : NinePatchTexture

fun Texture(
    size: IntSize,
    atlasOffset: IntOffset
): Texture = TextureImpl(
    size = size,
    atlasOffset = atlasOffset,
)

fun NinePatchTexture(
    size: IntSize,
    atlasOffset: IntOffset,
    scaleArea: IntRect,
    padding: IntPadding,
): NinePatchTexture = NinePatchTextureImpl(
    size = size,
    atlasOffset = atlasOffset,
    scaleArea = scaleArea,
    padding = padding,
)
