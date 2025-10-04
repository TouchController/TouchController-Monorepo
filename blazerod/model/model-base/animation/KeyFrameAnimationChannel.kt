package top.fifthlight.blazerod.model.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.util.MutableFloat

data class KeyFrameAnimationChannel<T : Any, D>(
    override val type: AnimationChannel.Type<T, D>,
    override val typeData: D,
    private val components: List<AnimationChannelComponent<*, *>> = listOf(),
    val indexer: AnimationKeyFrameIndexer,
    val interpolator: AnimationInterpolator<T>,
    val keyframeData: AnimationKeyFrameData<T>,
    val interpolation: AnimationInterpolation,
    val valueSetter: (List<T>, T) -> Unit,
    val defaultValue: () -> T,
) : AnimationChannel<T, D> {
    init {
        require(interpolation.elements == keyframeData.elements) { "Bad elements of keyframe data: ${keyframeData.elements}" }
    }

    val duration: Float
        get() = indexer.lastTime

    private val indexResult = AnimationKeyFrameIndexer.FindResult()
    private val startValues = List(interpolation.elements) { defaultValue() }
    private val endValues = List(interpolation.elements) { defaultValue() }

    private val componentOfTypes = components.associateBy { it.type }

    init {
        for (component in componentOfTypes.values) {
            component.onAttachToChannel(this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AnimationChannelComponent.Type<C, T>, C> getComponent(type: T): C? =
        componentOfTypes[type] as C?

    override fun getData(context: AnimationContext, state: AnimationState, result: T) {
        val time = state.getTime()
        indexer.findKeyFrames(time, indexResult)
        if (indexResult.startFrame == indexResult.endFrame || time < indexResult.startTime) {
            keyframeData.get(context, state, indexResult.startFrame, startValues, post = false)
            valueSetter(startValues, result)
            return
        }
        if (indexResult.endTime < time) {
            keyframeData.get(context, state, indexResult.endFrame, endValues, post = true)
            valueSetter(endValues, result)
            return
        }
        val delta = (time - indexResult.startTime) / (indexResult.endTime - indexResult.startTime)
        keyframeData.get(context, state, indexResult.startFrame, startValues, post = false)
        keyframeData.get(context, state, indexResult.endFrame, endValues, post = true)
        interpolator.interpolate(
            context = context,
            state = state,
            delta = delta,
            startFrame = indexResult.startFrame,
            endFrame = indexResult.endFrame,
            startValue = startValues,
            endValue = endValues,
            result = result,
        )
    }
}

@JvmName("Vector3fKeyFrameAnimationChannel")
fun <D> KeyFrameAnimationChannel(
    type: AnimationChannel.Type<Vector3f, D>,
    typeData: D,
    components: List<AnimationChannelComponent<*, *>> = listOf(),
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Vector3f>,
    interpolation: AnimationInterpolation,
): KeyFrameAnimationChannel<Vector3f, D> = KeyFrameAnimationChannel(
    type = type,
    typeData = typeData,
    components = components,
    indexer = indexer,
    interpolator = Vector3AnimationInterpolator(interpolation),
    keyframeData = keyframeData,
    interpolation = interpolation,
    valueSetter = { values, result -> result.set(values[0]) },
    defaultValue = ::Vector3f,
)

@JvmName("QuaternionfKeyFrameAnimationChannel")
fun <D> KeyFrameAnimationChannel(
    type: AnimationChannel.Type<Quaternionf, D>,
    typeData: D,
    components: List<AnimationChannelComponent<*, *>> = listOf(),
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<Quaternionf>,
    interpolation: AnimationInterpolation,
): KeyFrameAnimationChannel<Quaternionf, D> = KeyFrameAnimationChannel(
    type = type,
    typeData = typeData,
    components = components,
    indexer = indexer,
    interpolator = QuaternionAnimationInterpolator(interpolation),
    keyframeData = keyframeData,
    interpolation = interpolation,
    valueSetter = { values, result -> result.set(values[0]) },
    defaultValue = ::Quaternionf,
)

@JvmName("FloatKeyFrameAnimationChannel")
fun <D> KeyFrameAnimationChannel(
    type: AnimationChannel.Type<MutableFloat, D>,
    typeData: D,
    components: List<AnimationChannelComponent<*, *>> = listOf(),
    indexer: AnimationKeyFrameIndexer,
    keyframeData: AnimationKeyFrameData<MutableFloat>,
    interpolation: AnimationInterpolation,
): KeyFrameAnimationChannel<MutableFloat, D> = KeyFrameAnimationChannel(
    type = type,
    typeData = typeData,
    components = components,
    indexer = indexer,
    interpolator = FloatAnimationInterpolator(interpolation),
    keyframeData = keyframeData,
    interpolation = interpolation,
    valueSetter = { values, result -> result.value = values[0].value },
    defaultValue = ::MutableFloat,
)
