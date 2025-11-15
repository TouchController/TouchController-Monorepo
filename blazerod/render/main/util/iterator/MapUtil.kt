package top.fifthlight.blazerod.util.iterator

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap

fun <V> Int2ReferenceMap<V>.getOrPut(key: Int, defaultValue: () -> V): V =
    get(key) ?: defaultValue().also { put(key, it) }
