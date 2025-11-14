package top.fifthlight.combine.item

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.data.Identifier

val LocalDataComponentTypeFactory = staticCompositionLocalOf<DataComponentTypeFactory> {
    error(
        "No DataComponentTypeFactory in context"
    )
}

interface DataComponentTypeFactory {
    val supportDataComponents: Boolean

    fun of(id: Identifier): DataComponentType?

    val allComponents: PersistentList<DataComponentType>

    object Unsupported : DataComponentTypeFactory {
        override val supportDataComponents
            get() = false

        override fun of(id: Identifier): DataComponentType? = null

        override val allComponents: PersistentList<DataComponentType> = persistentListOf()
    }
}

interface DataComponentType {
    val id: Identifier?

    val allItems: PersistentList<Item>
}