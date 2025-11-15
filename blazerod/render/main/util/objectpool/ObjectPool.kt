package top.fifthlight.blazerod.util.objectpool

import top.fifthlight.blazerod.debug.ObjectPoolTracker
import java.util.*

private val pools = Collections.synchronizedSet(mutableSetOf<Pool<*>>())
private var cleaned = false
fun cleanupObjectPools() {
    require(!cleaned) { "All pools has been cleaned" }
    cleaned = true
    pools.forEach { it.close() }
    pools.clear()
}

interface Pool<T> : AutoCloseable {
    fun acquire(): T
    fun release(obj: T)
}

open class ObjectPool<T : Any>(
    protected val identifier: String,
    protected val create: () -> T,
    protected val onAcquired: (T.() -> Unit)? = null,
    protected val onReleased: (T.() -> Unit)? = null,
    protected val onClosed: (T.() -> Unit)?,
) : Pool<T> {
    protected var closed: Boolean = false
    protected val pool = ArrayDeque<T>()

    override fun acquire(): T {
        require(!closed) { "Pool is closed" }
        return if (pool.isEmpty()) {
            create().also {
                ObjectPoolTracker.instance?.set(identifier) {
                    copy(allocatedItem = allocatedItem + 1)
                }
            }
        } else {
            ObjectPoolTracker.instance?.set(identifier) {
                copy(
                    allocatedItem = allocatedItem + 1,
                    pooledItem = pooledItem - 1,
                )
            }
            pool.removeFirst()
        }.also { obj ->
            try {
                onAcquired?.invoke(obj)
            } catch (ex: Throwable) {
                ObjectPoolTracker.instance?.set(identifier) {
                    copy(
                        allocatedItem = allocatedItem - 1,
                        failedItem = failedItem + 1,
                    )
                }
                throw ex
            }
        }
    }

    override fun release(obj: T) {
        require(!closed) { "Pool is closed" }
        try {
            onReleased?.invoke(obj)
        } catch (ex: Throwable) {
            ObjectPoolTracker.instance?.set(identifier) {
                copy(
                    allocatedItem = allocatedItem - 1,
                    failedItem = failedItem + 1,
                )
            }
            throw ex
        }
        ObjectPoolTracker.instance?.set(identifier) {
            copy(
                allocatedItem = allocatedItem - 1,
                pooledItem = pooledItem + 1,
            )
        }
        pool.addLast(obj)
    }

    override fun close() {
        if (closed) {
            return
        }
        ObjectPoolTracker.instance?.set(identifier) { ObjectPoolTracker.Item() }
        pool.forEach { onClosed?.invoke(it) }
        pool.clear()
        closed = true
    }
}

class ThreadSafeObjectPool<T>(
    val inner: Pool<T>,
) : Pool<T> {
    override fun acquire(): T = synchronized(this) {
        inner.acquire()
    }

    override fun release(obj: T) = synchronized(this) {
        inner.release(obj)
    }

    override fun close() = inner.close()
}
