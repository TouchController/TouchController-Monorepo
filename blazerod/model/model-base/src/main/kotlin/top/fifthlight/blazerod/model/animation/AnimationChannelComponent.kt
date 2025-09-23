package top.fifthlight.blazerod.model.animation

interface AnimationChannelComponent<C: AnimationChannelComponent<C, T>, T: AnimationChannelComponent.Type<C, T>> {
    interface Type<C: AnimationChannelComponent<C, T>, T: Type<C, T>> {
        val name: String
    }

    val type: T
    fun onAttachToChannel(channel: AnimationChannel<*, *>) = Unit
}
