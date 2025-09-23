package top.fifthlight.blazerod.model.bedrock.animation

import it.unimi.dsi.fastutil.floats.FloatList
import org.joml.Vector3f
import top.fifthlight.blazerod.model.animation.AnimationKeyFrameData

class BedrockKeyFrameData(
    private val values: FloatList,
    private val molangs: List<String?>,
) : AnimationKeyFrameData<Vector3f> {
    override val frames = values.size / 6

    override val elements: Int
        get() = 1

    override fun get(index: Int, data: List<Vector3f>, post: Boolean) {
        // TODO use molang
        val baseOffset = index * 6 + if (post) 3 else 0
        data[0].set(
            values.getFloat(baseOffset + 0),
            values.getFloat(baseOffset + 1),
            values.getFloat(baseOffset + 2),
        )
    }
}