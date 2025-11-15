package top.fifthlight.blazerod.model

import java.util.*

data class Mesh(
    val id: MeshId,
    val primitives: List<Primitive>,
    val firstPersonFlag: FirstPersonFlag = FirstPersonFlag.BOTH,
    val weights: List<Float>?,
) {
    enum class FirstPersonFlag {
        AUTO,
        THIRD_PERSON_ONLY,
        FIRST_PERSON_ONLY,
        BOTH,
    }
}

data class MeshId(
    val modelId: UUID,
    val index: Int,
)
