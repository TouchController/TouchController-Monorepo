package top.fifthlight.combine.resources.altas

import kotlinx.serialization.Serializable
import top.fifthlight.combine.resources.NinePatch
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import java.nio.file.Path

@Serializable
data class PlacedTexture(
    val path: Path,
    val identifier: String,
    val position: IntOffset,
    val size: IntSize,
    val ninePatch: NinePatch?,
)
