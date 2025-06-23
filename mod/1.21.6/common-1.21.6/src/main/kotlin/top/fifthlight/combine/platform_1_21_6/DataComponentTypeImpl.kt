package top.fifthlight.combine.platform_1_21_6

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import top.fifthlight.combine.data.DataComponentTypeFactory
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.platform_1_21_x.toCombine
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import kotlin.jvm.optionals.getOrNull
import top.fifthlight.combine.data.DataComponentType as CombineDataComponentType

object DataComponentTypeFactoryImpl : DataComponentTypeFactory {
    override val supportDataComponents: Boolean = true

    override fun of(id: Identifier): CombineDataComponentType? =
        BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id.toMinecraft())?.let { DataComponentTypeImpl(it) }

    override val allComponents: PersistentList<CombineDataComponentType> by lazy {
        BuiltInRegistries.DATA_COMPONENT_TYPE.map { DataComponentTypeImpl(it) }.toPersistentList()
    }
}

data class DataComponentTypeImpl(
    val inner: DataComponentType<*>,
) : CombineDataComponentType {
    override val id: Identifier?
        get() = BuiltInRegistries.DATA_COMPONENT_TYPE.getResourceKey(inner).getOrNull()?.location()?.toCombine()

    override val allItems: PersistentList<Item> by lazy {
        BuiltInRegistries.ITEM
            .filter { it.components().has(inner) }
            .map { it.toCombine() }
            .toPersistentList()
    }
}
