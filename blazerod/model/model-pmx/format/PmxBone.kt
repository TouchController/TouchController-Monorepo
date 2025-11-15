package top.fifthlight.blazerod.model.pmx.format

import org.joml.Vector3fc

data class PmxBone(
    val index: Int,
    val nameLocal: String,
    val nameUniversal: String,
    val position: Vector3fc,
    val parentBoneIndex: Int?,
    val layer: Int,
    val flags: Flags,
    val tailPosition: TailPosition,
    val inheritParentIndex: Int?,
    val inheritParentInfluence: Float?,
    val axisDirection: Vector3fc?,
    val localCoordinate: LocalCoordinate?,
    val externalParentIndex: Int?,
    val ikData: IkData?,
) {
    init {
        if (flags.inheritRotation || flags.inheritTranslation) {
            inheritParentIndex!!
            inheritParentInfluence!!
        }
        if (flags.localCoordinate) {
            localCoordinate!!
        }
        if (flags.externalParentDeform) {
            externalParentIndex!!
        }
        if (flags.ik) {
            ikData!!
        }
    }

    data class Flags(
        val indexedTailPosition: Boolean,
        val rotatable: Boolean,
        val translatable: Boolean,
        val isVisible: Boolean,
        val enabled: Boolean,
        val ik: Boolean,
        val inheritLocal: Boolean,
        val inheritRotation: Boolean,
        val inheritTranslation: Boolean,
        val fixedAxis: Boolean,
        val localCoordinate: Boolean,
        val physicsAfterDeform: Boolean,
        val externalParentDeform: Boolean,
    )

    sealed class TailPosition {
        data class Indexed(
            val boneIndex: Int,
        ): TailPosition()
        data class Scalar(
            val position: Vector3fc,
        ): TailPosition()
    }

    data class IkLink(
        val index: Int,
        val limits: Limits?,
    ) {
        data class Limits(
            val limitMin: Vector3fc,
            val limitMax: Vector3fc,
        )
    }

    data class IkData(
        val effectorIndex: Int,
        val targetIndex: Int,
        val loopCount: Int,
        val limitRadian: Float,
        val links: List<IkLink>,
    )

    data class InheritData(
        val sourceIndex: Int,
        val targetIndex: Int,
        val influence: Float,
        val inheritRotation: Boolean,
        val inheritTranslation: Boolean,
        val inheritLocal: Boolean,
    )

    val inheritData = if (flags.inheritRotation || flags.inheritTranslation) {
        InheritData(
            sourceIndex = inheritParentIndex!!,
            targetIndex = index,
            influence = inheritParentInfluence!!,
            inheritRotation = flags.inheritRotation,
            inheritTranslation = flags.inheritTranslation,
            inheritLocal = flags.inheritLocal,
        )
    } else {
        null
    }

    data class LocalCoordinate(
        val xVector: Vector3fc?,
        val yVector: Vector3fc?,
    )
}
