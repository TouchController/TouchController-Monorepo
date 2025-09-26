package top.fifthlight.blazerod.animation

import top.fifthlight.blazerod.model.animation.Animation
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.animation.AnimationState
import top.fifthlight.blazerod.runtime.ModelInstance
import java.util.concurrent.ConcurrentLinkedDeque

class AnimationItem(
    val name: String? = null,
    val animation: Animation,
    val channels: List<AnimationChannelItem<*, *, *>>,
) {
    val duration
        get() = animation.duration

    fun createState(context: AnimationContext) = animation.createState(context)
}

class AnimationItemPendingValues(animationItem: AnimationItem) {
    @Volatile
    var applied: Boolean = false

    val pendingValues = Array(animationItem.channels.size) { animationItem.channels[it].createPendingValue() }
}

class AnimationItemInstance(val animationItem: AnimationItem) {
    private val pendingStack = ConcurrentLinkedDeque<AnimationItemPendingValues>()

    fun createState(context: AnimationContext) = animationItem.createState(context)

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun <P : Any> AnimationChannelItem<*, *, P>.updateUnsafe(
        context: AnimationContext,
        state: AnimationState,
        pendingValue: Any,
    ) = update(context, state, pendingValue as P)

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun <P : Any> AnimationChannelItem<*, *, P>.applyUnsafe(
        instance: ModelInstance,
        pendingValue: Any,
    ) = apply(instance, pendingValue as P)

    fun update(context: AnimationContext, state: AnimationState) =
        (pendingStack.pollLast() ?: AnimationItemPendingValues(animationItem)).also {
            it.applied = false
            animationItem.channels.forEachIndexed { index, channel ->
                channel.updateUnsafe(context, state, it.pendingValues[index])
            }
        }

    fun apply(instance: ModelInstance, pendingValues: AnimationItemPendingValues) =
        animationItem.channels.forEachIndexed { index, channel ->
            pendingValues.pendingValues[index].let { pendingValue ->
                channel.applyUnsafe(instance, pendingValue)
            }
            if (!pendingValues.applied) {
                pendingValues.applied = true
                pendingStack.addLast(pendingValues)
            }
    }
}
