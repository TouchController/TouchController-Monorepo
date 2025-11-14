package top.fifthlight.touchcontroller.common.config.preset

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Immutable
@Serializable
data class CustomCondition(
    val uuid: Uuid = Uuid.random(),
    val name: String? = null,
)