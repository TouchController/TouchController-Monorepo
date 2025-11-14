package top.fifthlight.combine.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.collections.immutable.PersistentList
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text

val LocalItemFactory = staticCompositionLocalOf<ItemFactory> {
    error("No ItemFactory in context")
}

interface ItemFactory {
    fun createItem(id: Identifier): Item?
    fun createItemStack(item: Item, amount: Int): ItemStack
    fun createItemStack(id: Identifier, amount: Int): ItemStack?
    val allItems: PersistentList<Item>
    val subclasses: PersistentList<ItemSubclass>
}

interface MetadataItemFactory : ItemFactory {
    override fun createItem(id: Identifier): MetadataItem?
    fun createItem(id: Identifier, metadata: Int? = null): MetadataItem?
    override fun createItemStack(item: Item, amount: Int): MetadataItemStack
    override fun createItemStack(id: Identifier, amount: Int): MetadataItemStack?
    override val allItems: PersistentList<MetadataItem>
}

interface ItemSubclass {
    val id: String
    val configId: String
    val name: Text
    val items: PersistentList<Item>
}

@Immutable
interface Item {
    val id: Identifier
    fun isSubclassOf(subclass: ItemSubclass): Boolean
    fun containComponents(component: DataComponentType): Boolean
    fun matches(other: Item): Boolean = equals(other)

    @Composable
    fun toStack() = toStack(1)

    @Composable
    fun toStack(amount: Int) = LocalItemFactory.current.createItemStack(this, amount)

    companion object {
        @Composable
        fun of(id: Identifier) = LocalItemFactory.current.createItem(id)
    }
}

interface MetadataItem : Item {
    val metadata: Int?
}
