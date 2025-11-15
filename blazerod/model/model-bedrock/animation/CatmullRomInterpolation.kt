package top.fifthlight.blazerod.model.bedrock.animation

import org.joml.*
import top.fifthlight.blazerod.model.animation.*
import top.fifthlight.blazerod.model.util.FloatWrapper
import top.fifthlight.blazerod.model.util.MutableFloat

class CatmullRomInterpolation : AnimationInterpolation(1),
    AnimationChannelComponent<CatmullRomInterpolation, CatmullRomInterpolation.ComponentType> {
    object ComponentType : AnimationChannelComponent.Type<CatmullRomInterpolation, ComponentType> {
        override val name: String
            get() = "catmull_rom_interpolation"
    }

    override val type: ComponentType
        get() = ComponentType

    lateinit var keyFrameData: AnimationKeyFrameData<*>

    // Set this interpolation as a component, to get the keyframe data
    override fun onAttachToChannel(channel: AnimationChannel<*, *>) {
        val channel = (channel as? KeyFrameAnimationChannel)
            ?: error("CatmullRomInterpolation must be attached to a keyframe animation channel")
        this.keyFrameData = channel.keyframeData
    }

    private val vector3fP0 = Vector3f()
    private val vector3fP3 = Vector3f()
    private val vector3fP0List = listOf(vector3fP0)
    private val vector3fP3List = listOf(vector3fP3)
    override fun interpolateVector3f(
        context: AnimationContext,
        state: AnimationState,
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<Vector3fc>,
        endValue: List<Vector3fc>,
        result: Vector3f,
    ) {
        @Suppress("UNCHECKED_CAST")
        val keyframeData = keyFrameData as AnimationKeyFrameData<Vector3fc>

        val i0 = (startFrame - 1).coerceAtLeast(0)
        val i3 = (endFrame + 1).coerceIn(0, keyFrameData.frames - 1)
        keyframeData.get(context, state, i0, vector3fP0List, false)
        keyframeData.get(context, state, i3, vector3fP3List, false)

        val p0 = vector3fP0
        val p1 = startValue[0]
        val p2 = endValue[0]
        val p3 = vector3fP3

        // Catmull–Rom formula
        val t = delta
        val t2 = t * t
        val t3 = t2 * t

        // coefficients
        val c0 = -0.5f * t3 + t2 - 0.5f * t
        val c1 = 1.5f * t3 - 2.5f * t2 + 1.0f
        val c2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t
        val c3 = 0.5f * t3 - 0.5f * t2

        p0.mul(c0, result)
            .fma(c1, p1, result)
            .fma(c2, p2, result)
            .fma(c3, p3, result)
    }

    private val quaternionfP0 = Quaternionf()
    private val quaternionfP3 = Quaternionf()
    private val quaternionfP0List = listOf(quaternionfP0)
    private val quaternionfP3List = listOf(quaternionfP3)
    private val tempQuaternion = Quaternionf()
    override fun interpolateQuaternionf(
        context: AnimationContext,
        state: AnimationState,
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<Quaternionfc>,
        endValue: List<Quaternionfc>,
        result: Quaternionf,
    ) {
        @Suppress("UNCHECKED_CAST")
        val keyframeData = keyFrameData as AnimationKeyFrameData<Quaternionfc>

        val i0 = (startFrame - 1).coerceAtLeast(0)
        val i3 = (endFrame + 1).coerceIn(0, keyFrameData.frames - 1)
        keyframeData.get(context, state, i0, quaternionfP0List, false)
        keyframeData.get(context, state, i3, quaternionfP3List, false)

        val p0 = quaternionfP0
        val p1 = startValue[0]
        val p2 = endValue[0]
        val p3 = quaternionfP3

        // Catmull–Rom formula
        val t = delta
        val t2 = t * t
        val t3 = t2 * t

        // coefficients
        val c0 = (-0.5f * t3) + (1f * t2) + (-0.5f * t)
        val c1 = (1.5f * t3) + (-2.5f * t2) + 1.0f
        val c2 = (-1.5f * t3) + (2.0f * t2) + (0.5f * t)
        val c3 = (0.5f * t3) + (-0.5f * t2)

        p0.mul(c0, result)
        p1.mul(c1, tempQuaternion)
        result.add(tempQuaternion)
        p2.mul(c2, tempQuaternion)
        result.add(tempQuaternion)
        p3.mul(c3, tempQuaternion)
        result.add(tempQuaternion)
        result.normalize()
    }

    private val floatP0 = MutableFloat()
    private val floatP3 = MutableFloat()
    private val floatP0List = listOf(floatP0)
    private val floatP3List = listOf(floatP3)
    override fun interpolateFloat(
        context: AnimationContext,
        state: AnimationState,
        delta: Float,
        startFrame: Int,
        endFrame: Int,
        startValue: List<FloatWrapper>,
        endValue: List<FloatWrapper>,
        result: MutableFloat,
    ) {
        @Suppress("UNCHECKED_CAST")
        val keyframeData = keyFrameData as AnimationKeyFrameData<FloatWrapper>

        val i0 = (startFrame - 1).coerceAtLeast(0)
        val i3 = (endFrame + 1).coerceIn(0, keyFrameData.frames - 1)
        keyframeData.get(context, state, i0, floatP0List, false)
        keyframeData.get(context, state, i3, floatP3List, false)

        val p0 = floatP0.value
        val p1 = startValue[0].value
        val p2 = endValue[0].value
        val p3 = floatP3.value

        // Catmull–Rom formula
        val t = delta
        val t2 = t * t
        val t3 = t2 * t

        // coefficients
        val c0 = (-0.5f * t3) + (1f * t2) + (-0.5f * t)
        val c1 = (1.5f * t3) + (-2.5f * t2) + 1.0f
        val c2 = (-1.5f * t3) + (2.0f * t2) + (0.5f * t)
        val c3 = (0.5f * t3) + (-0.5f * t2)

        result.value = Math.fma(c0, p0, Math.fma(c1, p1, Math.fma(c2, p2, c3 * p3)))
    }
}
