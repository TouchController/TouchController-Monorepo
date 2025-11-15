package top.fifthlight.blazerod.model.pmd.format

import org.joml.Vector3f

internal data class PmdBone(
    val name: String,
    val parentBoneIndex: Int?,
    val tailBoneIndex: Int?,
    val type: Int,
    val targetBoneIndex: Int?,
    val position: Vector3f,
)
