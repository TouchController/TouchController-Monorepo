package top.fifthlight.blazerod.model.animation

interface Animation {
    val name: String?
    val channels: List<AnimationChannel<*, *>>

    val duration: Float?
        get() = null

    fun createState(context: AnimationContext): AnimationState
}
