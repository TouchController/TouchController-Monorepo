package top.fifthlight.combine.resources

import kotlinx.serialization.Serializable
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect

@Serializable
data class NinePatch(
    val scaleArea: IntRect,
    val padding: IntPadding
)
