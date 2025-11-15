package top.fifthlight.blazerod.debug

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ObjectPoolTracker {
    val itemCount: ConcurrentMap<String, Item> = ConcurrentHashMap()

    data class Item(
        val allocatedItem: Int = 0,
        val pooledItem: Int = 0,
        val failedItem: Int = 0,
    )

    inline fun set(identifier: String, crossinline func: Item.() -> Item) {
        itemCount.compute(identifier) { key, value -> func(value ?: Item()) }
    }

    fun dumpData() = itemCount.toMap()

    companion object {
        @Volatile
        var instance: ObjectPoolTracker? = null
            private set

        fun initialize() {
            synchronized(Companion) {
                if (instance != null) {
                    return
                }
                instance = ObjectPoolTracker()
            }
        }
    }
}