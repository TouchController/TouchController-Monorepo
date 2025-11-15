package top.fifthlight.blazerod.api.event

interface Event<T> {
    val invoker: T
    fun register(handler: T)
}

private class EventImpl<T>(
    private val invokerFactory: (List<T>) -> T,
) : Event<T> {
    private val handlers = mutableListOf<T>()

    @Volatile
    override var invoker = invokerFactory(handlers)
        private set

    override fun register(handler: T) = synchronized(this) {
        handlers.add(handler)
        invoker = invokerFactory(handlers)
    }
}

fun <T> Event(
    invokerFactory: (List<T>) -> T,
): Event<T> = EventImpl(invokerFactory)
