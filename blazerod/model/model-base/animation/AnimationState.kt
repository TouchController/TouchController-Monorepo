package top.fifthlight.blazerod.model.animation

interface AnimationState {
    val duration: Float?
    val playing: Boolean

    fun updateTime(context: AnimationContext)

    fun getTime(): Float
}
