package top.fifthlight.touchcontroller.resource

import kotlinx.serialization.Serializable
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import java.nio.file.Path

@Serializable
data class PlacedTexture(
    val relativePath: Path,
    val position: IntOffset,
    val size: IntSize,
    val ninePatch: NinePatch?,
)
