package top.fifthlight.combine.platform_1_21_6_1_21_7

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import top.fifthlight.combine.data.DataComponentType
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemSubclass
import top.fifthlight.combine.platform_1_21_x.toCombine
import top.fifthlight.combine.data.Item as CombineItem

@JvmInline
value class ItemImpl(
    val inner: Item
) : CombineItem {
    override val id: Identifier
        get() = BuiltInRegistries.ITEM.getKey(inner).toCombine()

    override fun isSubclassOf(subclass: ItemSubclass): Boolean {
        val targetClazz = (subclass as ItemSubclassImpl<*>).clazz
        val itemClazz = inner.javaClass
        return itemClazz == targetClazz || itemClazz.superclass == targetClazz || itemClazz.interfaces.contains(
            targetClazz
        )
    }

    override fun containComponents(component: DataComponentType) =
        inner.components().has((component as DataComponentTypeImpl).inner)
}