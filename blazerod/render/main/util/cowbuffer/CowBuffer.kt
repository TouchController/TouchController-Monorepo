package top.fifthlight.blazerod.util.cowbuffer

import top.fifthlight.blazerod.api.refcount.AbstractRefCount
import top.fifthlight.blazerod.api.refcount.RefCount
import top.fifthlight.blazerod.util.objectpool.ObjectPool

/**
 * A single-threaded container for copy-on-write buffers.
 *
 * It don't create extra buffer when there is only one reference, to avoid unnecessary allocations.
 * Make sure you maintain reference count correctly, otherwise bad things will happen.
 */
class CowBuffer<C : CowBuffer.Content<C>> private constructor() : AbstractRefCount() {
    companion object {
        private val POOL = ObjectPool<CowBuffer<*>>(
            identifier = "cow_buffer",
            create = ::CowBuffer,
            onReleased = CowBuffer<*>::resetState,
            onClosed = { },
        )

        @Suppress("UNCHECKED_CAST")
        fun <C : Content<C>> acquire(content: C): CowBuffer<C> = (POOL.acquire() as CowBuffer<C>).apply {
            _content = content
            content.increaseReferenceCount()
        }
    }

    override val typeId: String
        get() = "cow_buffer"

    override fun onClosed() {
        content.decreaseReferenceCount()
        _content = null
        POOL.release(this)
    }

    interface Content<C : Content<C>> : RefCount {
        fun copy(): C
    }

    private var _content: C? = null
    val content: C
        get() = _content ?: error("Buffer has been recycled")

    fun copy() = acquire(content)

    fun edit(editor: C.() -> Unit): CowBuffer<C> {
        if (referenceCount <= 1) {
            editor(content)
            return this
        } else {
            val copy = content.copy()
            editor(copy)
            return acquire(copy)
        }
    }
}

fun <C : CowBuffer.Content<C>> List<CowBuffer<C>>.copy() = List(size) { get(it).copy() }
