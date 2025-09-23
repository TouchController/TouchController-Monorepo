@file:Suppress("NOTHING_TO_INLINE")

package top.fifthlight.blazerod.model.bedrock

import com.google.gson.stream.JsonReader
import org.joml.Vector2f
import org.joml.Vector3f

inline fun <T> JsonReader.with(block: JsonReader.() -> T) = try {
    block()
} finally {
    close()
}

inline fun JsonReader.obj(onEntry: JsonReader.(key: String) -> Unit) {
    beginObject()
    while (hasNext()) {
        onEntry(nextName())
    }
    endObject()
}

inline fun JsonReader.array(onEntry: JsonReader.() -> Unit) {
    beginArray()
    while (hasNext()) {
        onEntry()
    }
    endArray()
}

inline fun JsonReader.vec3(vector3f: Vector3f = Vector3f()) = vector3f.apply {
    beginArray()
    x = nextDouble().toFloat()
    y = nextDouble().toFloat()
    z = nextDouble().toFloat()
    endArray()
}

inline fun JsonReader.vec2(vector2f: Vector2f = Vector2f()) = vector2f.apply {
    beginArray()
    x = nextDouble().toFloat()
    y = nextDouble().toFloat()
    endArray()
}
