package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import top.fifthlight.blazerod.model.util.MutableFloat

data class SingleFrameAnimationChannel<T : Any, D>(
    override val type: AnimationChannel.Type<T, D>,
    override val typeData: D,
    val setValue: (context: AnimationContext, state: AnimationState, result: T) -> Unit,
) : AnimationChannel<T, D> {
    val duration: Float
        get() = 0f

    override fun <T : AnimationChannelComponent.Type<C, T>, C> getComponent(type: T): C? = null

    override fun getData(
        context: AnimationContext,
        state: AnimationState,
        result: T,
    ) = setValue(context, state, result)
}

@JvmName("Vector3fSingleFrameAnimationChannel")
fun <D> SingleFrameAnimationChannel(
    type: AnimationChannel.Type<Vector3f, D>,
    typeData: D,
    value: Vector3fc,
) = SingleFrameAnimationChannel(type, typeData) { _, _, result ->
    result.set(value)
}

@JvmName("QuaternionfSingleFrameAnimationChannel")
fun <D> SingleFrameAnimationChannel(
    type: AnimationChannel.Type<Quaternionf, D>,
    typeData: D,
    value: Quaternionf,
) = SingleFrameAnimationChannel(type, typeData) { _, _, result ->
    result.set(value)
}

@JvmName("FloatSingleFrameAnimationChannel")
fun <D> SingleFrameAnimationChannel(
    type: AnimationChannel.Type<MutableFloat, D>,
    typeData: D,
    value: Float,
) = SingleFrameAnimationChannel(type, typeData) { _, _, result ->
    result.value = value
}
