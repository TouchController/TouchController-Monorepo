package top.fifthlight.touchcontroller.common.config.preset

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltInPresetKey
import kotlin.uuid.Uuid

@Serializable
sealed class PresetConfig {
    @Serializable
    @SerialName("builtin")
    data class BuiltIn(
        val key: BuiltInPresetKey = BuiltInPresetKey(),
    ) : PresetConfig()

    @Serializable
    @SerialName("custom")
    data class Custom(
        val uuid: Uuid? = null,
    ) : PresetConfig()
}