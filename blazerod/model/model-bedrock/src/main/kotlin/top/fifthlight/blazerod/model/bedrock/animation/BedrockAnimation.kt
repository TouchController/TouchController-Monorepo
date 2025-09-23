package top.fifthlight.blazerod.model.bedrock.animation

import top.fifthlight.blazerod.model.animation.*

class BedrockAnimation(
    override val name: String,
    override val channels: List<AnimationChannel<*, *>>,
    duration: Float?,
    val loopMode: AnimationLoopMode,
) : Animation {
    override val duration = duration ?: channels.maxOfOrNull { (it as? KeyFrameAnimationChannel)?.duration ?: 0f } ?: 0f

    override fun createState(context: AnimationContext) = SimpleAnimationState(
        context = context,
        duration = duration,
        loop = loopMode != AnimationLoopMode.NO_LOOP,
    )
}