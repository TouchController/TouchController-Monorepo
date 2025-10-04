package top.fifthlight.blazerod.model.animation

import it.unimi.dsi.fastutil.floats.FloatList
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.Accessor
import top.fifthlight.blazerod.model.util.MutableFloat
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface AnimationKeyFrameData<T> {
    val frames: Int
    val elements: Int
    fun get(
        context: AnimationContext,
        state: AnimationState,
        index: Int,
        data: List<T>,
        post: Boolean,
    )

    companion object
}

class FloatListAnimationKeyFrameData<T>(
    private val values: FloatList,
    override val elements: Int,
    val splitPrePost: Boolean = false,
    private val componentCount: Int,
    private val elementGetter: (list: FloatList, offset: Int, result: T) -> Unit,
) : AnimationKeyFrameData<T> {
    private val itemSize = if (splitPrePost) 2 else 1
    private val valueSize = itemSize * elements * componentCount

    init {
        require(values.size % valueSize == 0) {
            "Invalid data size ${values.size} for elements $elements (requires multiple of $valueSize)"
        }
    }

    override val frames = values.size / valueSize

    override fun get(
        context: AnimationContext,
        state: AnimationState,
        index: Int,
        data: List<T>,
        post: Boolean,
    ) {
        val baseOffset = index * valueSize + if (splitPrePost && post) {
            elements * componentCount
        } else {
            0
        }
        for (i in 0 until elements) {
            val offset = baseOffset + i * componentCount
            elementGetter(values, offset, data[i])
        }
    }
}

fun AnimationKeyFrameData.Companion.ofVector3f(
    values: FloatList,
    elements: Int,
    splitPrePost: Boolean = false,
) = FloatListAnimationKeyFrameData<Vector3f>(
    values = values,
    elements = elements,
    splitPrePost = splitPrePost,
    componentCount = 3,
    elementGetter = { list, offset, result ->
        result.set(
            list.getFloat(offset),
            list.getFloat(offset + 1),
            list.getFloat(offset + 2)
        )
    },
)

fun AnimationKeyFrameData.Companion.ofQuaternionf(
    values: FloatList,
    elements: Int,
    splitPrePost: Boolean = false,
) = FloatListAnimationKeyFrameData<Quaternionf>(
    values = values,
    elements = elements,
    splitPrePost = splitPrePost,
    componentCount = 4,
    elementGetter = { list, offset, result ->
        result.set(
            list.getFloat(offset),
            list.getFloat(offset + 1),
            list.getFloat(offset + 2),
            list.getFloat(offset + 3)
        )
    },
)

fun AnimationKeyFrameData.Companion.ofFloat(
    values: FloatList,
    elements: Int,
    splitPrePost: Boolean = false,
) = FloatListAnimationKeyFrameData<MutableFloat>(
    values = values,
    elements = elements,
    splitPrePost = splitPrePost,
    componentCount = 1,
    elementGetter = { list, offset, result -> result.value = list.getFloat(offset) },
)

class AccessorAnimationKeyFrameData<T>(
    private val accessor: Accessor,
    override val elements: Int,
    private val elementGetter: (buffer: ByteBuffer, result: T) -> Unit,
) : AnimationKeyFrameData<T> {
    init {
        require(accessor.count % elements == 0) { "Invalid data size ${accessor.count} for elements $elements" }
    }

    override val frames = accessor.count / elements

    private val isZeroFilled = accessor.bufferView == null
    private val itemLength = accessor.componentType.byteLength * accessor.type.components
    private val itemStride = accessor.bufferView?.byteStride?.takeIf { it != 0 } ?: itemLength
    private val elementStride = itemStride * elements
    private val slice = accessor.bufferView?.let { bufferView ->
        bufferView.buffer.buffer
            .slice(accessor.byteOffset + bufferView.byteOffset, accessor.totalByteLength)
            .asReadOnlyBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)
    } ?: run {
        ByteBuffer
            .allocateDirect(itemLength)
            .asReadOnlyBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)
    }

    override fun get(
        context: AnimationContext,
        state: AnimationState,
        index: Int,
        data: List<T>,
        post: Boolean,
    ) {
        var position = index * elementStride
        for (i in 0 until elements) {
            if (isZeroFilled) {
                slice.clear()
                elementGetter(slice, data[i])
            } else {
                slice.clear()
                slice.position(position)
                slice.limit(position + itemLength)
                elementGetter(slice, data[i])
                position += itemStride
            }
        }
    }
}

fun <T, R> AnimationKeyFrameData<T>.map(
    defaultValue: () -> T,
    transform: (T, R) -> Unit,
): AnimationKeyFrameData<R> {
    val original = this
    return object : AnimationKeyFrameData<R> {
        override val frames: Int
            get() = original.frames
        override val elements: Int
            get() = original.elements

        private val tempOriginalData = List(original.elements) { defaultValue() }

        override fun get(
            context: AnimationContext,
            state: AnimationState,
            index: Int,
            data: List<R>,
            post: Boolean,
        ) {
            original.get(context, state, index, tempOriginalData, post)

            for (i in 0 until elements) {
                transform(tempOriginalData[i], data[i])
            }
        }
    }
}

@JvmName("mapVector3fKeyFrameData")
fun <R> AnimationKeyFrameData<Vector3f>.map(
    transform: (Vector3f, R) -> Unit,
): AnimationKeyFrameData<R> {
    return this.map(defaultValue = { Vector3f() }, transform)
}

@JvmName("mapQuaternionfKeyFrameData")
fun <R> AnimationKeyFrameData<Quaternionf>.map(
    transform: (Quaternionf, R) -> Unit,
): AnimationKeyFrameData<R> {
    return this.map(defaultValue = { Quaternionf() }, transform)
}

@JvmName("mapMutableFloatKeyFrameData")
fun <R> AnimationKeyFrameData<MutableFloat>.map(
    transform: (MutableFloat, R) -> Unit,
): AnimationKeyFrameData<R> {
    return this.map(defaultValue = { MutableFloat() }, transform)
}
