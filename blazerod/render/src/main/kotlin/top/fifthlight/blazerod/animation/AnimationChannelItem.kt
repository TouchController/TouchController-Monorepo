package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.animation.AnimationChannel
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.animation.AnimationState
import top.fifthlight.blazerod.model.resource.CameraTransform
import top.fifthlight.blazerod.model.resource.RenderExpression
import top.fifthlight.blazerod.model.resource.RenderExpressionGroup
import top.fifthlight.blazerod.model.util.MutableFloat

sealed class AnimationChannelItem<T : Any, D>(
    val channel: AnimationChannel<T, D>
) {
    abstract fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState)

    class TranslationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Translation) { "Unmatched animation channel: want translation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getData(context, state, translation)
            }
        }
    }

    class ScaleItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Scale) { "Unmatched animation channel: want scale, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getData(context, state, scale)
            }
        }
    }

    class RotationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Quaternionf, Unit>,
    ) : AnimationChannelItem<Quaternionf, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.Rotation) { "Unmatched animation channel: want rotation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformDecomposed(index, transformId) {
                channel.getData(context, state, rotation)
                rotation.normalize()
            }
        }
    }

    class BedrockTranslationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.BedrockTranslation) { "Unmatched animation channel: want translation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformBedrock(index, transformId) {
                channel.getData(context, state, translation)
            }
        }
    }

    class BedrockScaleItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Vector3f, Unit>,
    ) : AnimationChannelItem<Vector3f, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.BedrockScale) { "Unmatched animation channel: want scale, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformBedrock(index, transformId) {
                channel.getData(context, state, scale)
            }
        }
    }

    class BedrockRotationItem(
        private val index: Int,
        private val transformId: TransformId,
        channel: AnimationChannel<Quaternionf, Unit>,
    ) : AnimationChannelItem<Quaternionf, Unit>(channel) {
        init {
            require(channel.type == AnimationChannel.Type.BedrockRotation) { "Unmatched animation channel: want rotation, but got ${channel.type}" }
        }

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            instance.setTransformBedrock(index, transformId) {
                channel.getData(context, state, rotation)
                rotation.normalize()
            }
        }
    }

    class MorphItem(
        private val primitiveIndex: Int,
        private val targetGroupIndex: Int,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.MorphData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.MorphData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            channel.getData(context, state, data)
            instance.setGroupWeight(primitiveIndex, targetGroupIndex, data.value)
        }
    }

    protected fun RenderExpression.apply(instance: ModelInstance, weight: Float) = bindings.forEach { binding ->
        when (binding) {
            is RenderExpression.Binding.MorphTarget -> {
                instance.setGroupWeight(binding.morphedPrimitiveIndex, binding.groupIndex, weight)
            }
        }
    }

    class ExpressionItem(
        val expression: RenderExpression,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.ExpressionData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.ExpressionData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            channel.getData(context, state, data)
            expression.apply(instance, data.value)
        }
    }

    class ExpressionGroupItem(
        val group: RenderExpressionGroup,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.ExpressionData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.ExpressionData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            channel.getData(context, state, data)
            for (item in group.items) {
                val expression = instance.scene.expressions[item.expressionIndex]
                expression.apply(instance, data.value * item.influence)
            }
        }
    }

    class CameraFovItem(
        val cameraIndex: Int,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.CameraData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.CameraData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            channel.getData(context, state, data)
            when (val camera = instance.modelData.cameraTransforms[cameraIndex]) {
                is CameraTransform.MMD -> camera.fov = data.value
                is CameraTransform.Perspective -> camera.yfov = data.value
                else -> Unit
            }
        }
    }

    class MMDCameraDistanceItem(
        val cameraIndex: Int,
        channel: AnimationChannel<MutableFloat, AnimationChannel.Type.CameraData>,
    ) : AnimationChannelItem<MutableFloat, AnimationChannel.Type.CameraData>(channel) {
        private val data = MutableFloat()

        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            channel.getData(context, state, data)
            val camera = instance.modelData.cameraTransforms[cameraIndex] as? CameraTransform.MMD ?: return
            camera.distance = data.value
        }
    }

    class MMDCameraTargetItem(
        val cameraIndex: Int,
        channel: AnimationChannel<Vector3f, AnimationChannel.Type.CameraData>,
    ) : AnimationChannelItem<Vector3f, AnimationChannel.Type.CameraData>(channel) {
        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            val camera = instance.modelData.cameraTransforms[cameraIndex] as? CameraTransform.MMD ?: return
            channel.getData(context, state, camera.targetPosition)
        }
    }

    class MMDCameraRotationItem(
        val cameraIndex: Int,
        channel: AnimationChannel<Vector3f, AnimationChannel.Type.CameraData>,
    ) : AnimationChannelItem<Vector3f, AnimationChannel.Type.CameraData>(channel) {
        override fun apply(instance: ModelInstance, context: AnimationContext, state: AnimationState) {
            val camera = instance.modelData.cameraTransforms[cameraIndex] as? CameraTransform.MMD ?: return
            channel.getData(context, state, camera.rotationEulerAngles)
        }
    }
}
