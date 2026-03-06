package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.api.animation.AnimationItemInstance
import top.fifthlight.blazerod.api.animation.AnimationItemPendingValues
import top.fifthlight.blazerod.api.resource.ModelInstance
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.animation.AnimationState
import top.fifthlight.blazerod.runtime.ModelInstanceImpl
import kotlin.math.max
import kotlin.math.min

class CrossfadeAnimationItemPendingValues(
    val sourcePending: AnimationItemPendingValues,
    val targetPending: AnimationItemPendingValues,
    var weight: Float,
    var rootWeight: Float
) : AnimationItemPendingValues

class CrossfadeAnimationState(
    val sourceState: AnimationState,
    val targetState: AnimationState,
    context: AnimationContext,
    override val duration: Float
) : AnimationState {
    
    var elapsedTime: Float = 0f
    private var prevGameTick: Long = context.getGameTick()
    private var prevDeltaTick: Float = context.getDeltaTick()

    override val playing: Boolean
        get() = true

    override fun updateTime(context: AnimationContext) {
        sourceState.updateTime(context)
        targetState.updateTime(context)
        
        val currentTick = context.getGameTick()
        val currentDelta = context.getDeltaTick()
        val timePassed = ((currentTick - prevGameTick).toFloat() + (currentDelta - prevDeltaTick)) * AnimationContext.SECONDS_PER_TICK
        
        prevGameTick = currentTick
        prevDeltaTick = currentDelta
        elapsedTime += timePassed
    }

    override fun getTime(): Float = elapsedTime
}

class CrossfadeAnimationItemInstance(
    val sourceInstance: AnimationItemInstanceImpl,
    val targetInstance: AnimationItemInstanceImpl,
    val duration: Float
) : AnimationItemInstance {

    override fun createState(context: AnimationContext): AnimationState {
        val sourceState = sourceInstance.createState(context)
        val targetState = targetInstance.createState(context)
        return CrossfadeAnimationState(sourceState, targetState, context, duration)
    }

    override fun update(context: AnimationContext, state: AnimationState): AnimationItemPendingValues {
        val crossfadeState = state as CrossfadeAnimationState
        
        val sourcePending = sourceInstance.update(context, crossfadeState.sourceState)
        val targetPending = targetInstance.update(context, crossfadeState.targetState)
        
        val weight = min(1f, max(0f, crossfadeState.elapsedTime / crossfadeState.duration))
        val rootWeight = min(1f, max(0f, crossfadeState.elapsedTime / 0.05f))
        
        return CrossfadeAnimationItemPendingValues(sourcePending, targetPending, weight, rootWeight)
    }

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun <P : Any> AnimationChannelItem<*, *, P>.applyUnsafe(
        instance: ModelInstanceImpl,
        pendingValue: Any,
    ) = apply(instance, pendingValue as P)

    override fun apply(instance: ModelInstance, pendingValues: AnimationItemPendingValues) {
        val crossfadeValues = pendingValues as CrossfadeAnimationItemPendingValues
        val modelImpl = instance as ModelInstanceImpl
        
        val weight = crossfadeValues.weight
        val rootWeight = crossfadeValues.rootWeight
        
        if (weight >= 1f) {
            targetInstance.apply(modelImpl, crossfadeValues.targetPending)
            return
        }

        val sourcePendingImpl = crossfadeValues.sourcePending as AnimationItemPendingValuesImpl
        val targetPendingImpl = crossfadeValues.targetPending as AnimationItemPendingValuesImpl
        
        if (weight <= 0f) {
            sourceInstance.apply(modelImpl, crossfadeValues.sourcePending)
            return
        }
        
        val sourceChannels = sourceInstance.animationItem.channels
        val targetChannels = targetInstance.animationItem.channels
        
        val sourceMap = HashMap<Pair<Int, Class<out AnimationChannelItem<*,*,*>>>, Any>()
        for (i in sourceChannels.indices) {
            val ch = sourceChannels[i]
            val nodeIdx = ch.targetNodeIndex
            if (nodeIdx != null) {
                sourceMap[Pair(nodeIdx, ch.javaClass)] = sourcePendingImpl.pendingValues[i]
            }
        }
        
        val appliedNodes = HashSet<Pair<Int, Class<out AnimationChannelItem<*,*,*>>>>()
        
        // Apply target channels (blending from source or identity)
        for (i in targetChannels.indices) {
            val tChannel = targetChannels[i]
            val nodeIdx = tChannel.targetNodeIndex ?: continue
            val key = Pair(nodeIdx, tChannel.javaClass)
            val tVal = targetPendingImpl.pendingValues[i]
            val sVal = sourceMap[key]
            
            appliedNodes.add(key)
            
            // Check if this is a Root node (no parent)
            val isRoot = modelImpl.scene.nodes[nodeIdx].parent == null
            val currentWeight = if (isRoot && tChannel is AnimationChannelItem.TranslationItem) rootWeight else weight
            
            when (tChannel) {
                is AnimationChannelItem.TranslationItem -> {
                    val tVec = tVal as Vector3f
                    if (sVal != null) {
                        val sVec = sVal as Vector3f
                        tVec.lerp(sVec, 1f - currentWeight)
                    } else {
                        val sVec = Vector3f(0f, 0f, 0f)
                        tVec.lerp(sVec, 1f - currentWeight)
                    }
                    tChannel.applyUnsafe(modelImpl, tVec)
                }
                is AnimationChannelItem.RotationItem -> {
                    val tQuat = tVal as Quaternionf
                    if (sVal != null) {
                        val sQuat = sVal as Quaternionf
                        tQuat.slerp(sQuat, 1f - currentWeight)
                    } else {
                        val sQuat = Quaternionf()
                        tQuat.slerp(sQuat, 1f - currentWeight)
                    }
                    tChannel.applyUnsafe(modelImpl, tQuat)
                }
                is AnimationChannelItem.ScaleItem -> {
                    val tVec = tVal as Vector3f
                    if (sVal != null) {
                        val sVec = sVal as Vector3f
                        tVec.lerp(sVec, 1f - currentWeight)
                    } else {
                        val sVec = Vector3f(1f, 1f, 1f)
                        tVec.lerp(sVec, 1f - currentWeight)
                    }
                    tChannel.applyUnsafe(modelImpl, tVec)
                }
                else -> {
                    if (currentWeight >= 0.5f) {
                        tChannel.applyUnsafe(modelImpl, tVal)
                    } else if (sVal != null) {
                        for (j in sourceChannels.indices) {
                            val sChannel = sourceChannels[j]
                            if (sChannel.targetNodeIndex == nodeIdx && sChannel.javaClass == tChannel.javaClass) {
                                sChannel.applyUnsafe(modelImpl, sVal)
                                break
                            }
                        }
                    }
                }
            }
        }
        
        // Apply remaining source channels (blending to identity)
        for (i in sourceChannels.indices) {
            val sChannel = sourceChannels[i]
            val nodeIdx = sChannel.targetNodeIndex ?: continue
            val key = Pair(nodeIdx, sChannel.javaClass)
            
            if (!appliedNodes.contains(key)) {
                val sVal = sourcePendingImpl.pendingValues[i]
                
                val isRoot = modelImpl.scene.nodes[nodeIdx].parent == null
                val currentWeight = if (isRoot && sChannel is AnimationChannelItem.TranslationItem) rootWeight else weight
                
                when (sChannel) {
                    is AnimationChannelItem.TranslationItem -> {
                        val sVec = sVal as Vector3f
                        val tVec = Vector3f(0f, 0f, 0f)
                        sVec.lerp(tVec, currentWeight)
                        sChannel.applyUnsafe(modelImpl, sVec)
                    }
                    is AnimationChannelItem.RotationItem -> {
                        val sQuat = sVal as Quaternionf
                        val tQuat = Quaternionf()
                        sQuat.slerp(tQuat, currentWeight)
                        sChannel.applyUnsafe(modelImpl, sQuat)
                    }
                    is AnimationChannelItem.ScaleItem -> {
                        val sVec = sVal as Vector3f
                        val tVec = Vector3f(1f, 1f, 1f)
                        sVec.lerp(tVec, currentWeight)
                        sChannel.applyUnsafe(modelImpl, sVec)
                    }
                    else -> {
                        if (currentWeight < 0.5f) {
                            sChannel.applyUnsafe(modelImpl, sVal)
                        }
                    }
                }
            }
        }
    }
}
