package top.fifthlight.combine.resources

import kotlinx.serialization.Serializable
import top.fifthlight.data.IntSize

@Serializable
data class Metadata(
    val size: IntSize,
    val background: Boolean = false,
)

@Serializable
data class NinePatchMetadata(
    val size: IntSize,
    val ninePatch: NinePatch,
)
