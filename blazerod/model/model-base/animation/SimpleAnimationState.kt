package top.fifthlight.blazerod.model.animation

class SimpleAnimationState(
    initGameTick: Long,
    initDeltaTick: Float,
    override val duration: Float,
    loop: Boolean = true,
): AnimationState {
    constructor(
        context: AnimationContext,
        duration: Float,
        loop: Boolean = true,
    ): this(
        initGameTick = context.getGameTick(),
        initDeltaTick = context.getDeltaTick(),
        duration = duration,
        loop = loop,
    )

    private var prevPauseGameTick: Long = initGameTick
    private var prevPauseDeltaTick: Float = initDeltaTick
    private var prevGameTick: Long = initGameTick
    private var prevDeltaTick: Float = initDeltaTick
    private var prevPauseTime: Float = 0f

    private var currentTime: Float = 0f

    init {
        markPause()
    }

    override fun updateTime(context: AnimationContext) {
        prevGameTick = context.getGameTick()
        prevDeltaTick = context.getDeltaTick()

        if (paused) {
            currentTime = prevPauseTime
            return
        }

        val timeUntilPause = ((prevGameTick - prevPauseGameTick).toFloat() + (prevDeltaTick - prevPauseDeltaTick)) * AnimationContext.SECONDS_PER_TICK
        val scaledTimeUntilPause = timeUntilPause * speed
        currentTime = prevPauseTime + scaledTimeUntilPause

        if (loop) {
            currentTime %= duration
            if (currentTime < 0) currentTime += duration
        } else {
            currentTime = currentTime.coerceIn(0f, duration)
        }
    }

    override fun getTime() = currentTime

    fun seek(time: Float) {
        val targetTime = if (loop) {
            var result = time % duration
            if (result < 0) result += duration
            result
        } else {
            time.coerceIn(0f, duration)
        }

        currentTime = targetTime
        markPause()
    }

    private fun markPause() {
        prevPauseGameTick = prevGameTick
        prevPauseDeltaTick = prevDeltaTick
        prevPauseTime = currentTime
    }

    var loop: Boolean = loop
        set(value) {
            if (field != value) {
                markPause()
            }
            field = value
        }

    var paused: Boolean = false
        set(value) {
            if (field != value) {
                markPause()
            }
            field = value
        }

    var speed: Float = 1f
        set(value) {
            if (field != value) {
                markPause()
            }
            field = value
        }

    override val playing: Boolean
        get() = !paused
}
