package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.Node
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.util.MutableFloat

interface AnimationChannelComponentContainer {
    fun <T : AnimationChannelComponent.Type<C, T>, C> getComponent(type: T): C?
}

interface AnimationChannel<T : Any, D> : AnimationChannelComponentContainer {
    sealed class Type<T : Any, D> {
        data class NodeData(
            val targetNode: Node?,
            val targetNodeName: String?,
            val targetHumanoidTag: HumanoidTag?,
        )

        data class TransformData(
            val node: NodeData,
            val transformId: TransformId,
        )

        data class ExpressionData(
            val name: String? = null,
            val tag: top.fifthlight.blazerod.model.Expression.Tag? = null,
        )

        data class MorphData(
            val nodeData: NodeData,
            val targetMorphGroupIndex: Int,
        )

        data class CameraData(
            val cameraName: String,
        )

        data object Expression : Type<MutableFloat, ExpressionData>()

        data object Translation : Type<Vector3f, TransformData>()
        data object Scale : Type<Vector3f, TransformData>()
        data object Rotation : Type<Quaternionf, TransformData>()

        data object BedrockTranslation : Type<Vector3f, TransformData>()
        data object BedrockScale : Type<Vector3f, TransformData>()
        data object BedrockRotation : Type<Quaternionf, TransformData>()

        data object Morph : Type<MutableFloat, MorphData>()

        data object CameraFov : Type<MutableFloat, CameraData>()
        data object MMDCameraDistance : Type<MutableFloat, CameraData>()
        data object MMDCameraTarget : Type<Vector3f, CameraData>()
        data object MMDCameraRotation : Type<Vector3f, CameraData>()
    }

    val type: Type<T, D>
    val typeData: D
    fun getData(context: AnimationContext, state: AnimationState, result: T)
}
