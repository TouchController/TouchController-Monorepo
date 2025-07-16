package top.fifthlight.combine.platform_1_21_6_1_21_7

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.*
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.ItemSubclass
import top.fifthlight.combine.platform_1_21_x.TextImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.touchcontroller.common.config.ItemList
import kotlin.jvm.optionals.getOrNull
import top.fifthlight.combine.data.Item as CombineItem
import top.fifthlight.combine.data.ItemStack as CombineItemStack

object ItemFactoryImpl : ItemFactory {
    override fun createItem(id: Identifier): CombineItem? {
        val item = BuiltInRegistries.ITEM.getOptional(id.toMinecraft()).getOrNull() ?: return null
        return ItemImpl(item)
    }

    override fun createItemStack(
        item: CombineItem,
        amount: Int
    ): CombineItemStack {
        val minecraftItem = (item as ItemImpl).inner
        val stack = ItemStack(minecraftItem, amount)
        return ItemStackImpl(stack)
    }

    override fun createItemStack(id: Identifier, amount: Int): CombineItemStack? {
        val item = BuiltInRegistries.ITEM.getOptional(id.toMinecraft()).getOrNull() ?: return null
        val stack = ItemStack(item, amount)
        return ItemStackImpl(stack)
    }

    override val allItems: PersistentList<CombineItem> by lazy {
        BuiltInRegistries.ITEM.map(Item::toCombine).toPersistentList()
    }

    val rangedWeaponSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("Ranged weapon")),
        configId = "RangedWeaponItem",
        clazz = ProjectileWeaponItem::class.java,
    )

    val projectileSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("Projectile")),
        configId = "ProjectileItem",
        clazz = ProjectileItem::class.java,
    )

    val bucketSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("Bucket")),
        configId = "BucketItem",
        clazz = BucketItem::class.java,
    )

    val boatSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("Boat")),
        configId = "BoatItem",
        clazz = BoatItem::class.java,
    )

    val placeableOnWaterSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("PlaceableOnWater")),
        configId = "PlaceableOnWaterItem",
        clazz = PlaceOnWaterBlockItem::class.java,
    )

    val spawnEggSubclass = ItemSubclassImpl(
        name = TextImpl(Component.literal("SpawnEgg")),
        configId = "SpawnEggItem",
        clazz = SpawnEggItem::class.java,
    )

    override val subclasses: PersistentList<ItemSubclass> = persistentListOf(
        rangedWeaponSubclass,
        projectileSubclass,
        bucketSubclass,
        boatSubclass,
        placeableOnWaterSubclass,
        spawnEggSubclass,
    )
}

fun Item.toCombine() = ItemImpl(this)
fun ItemStack.toCombine() = ItemStackImpl(this)
fun CombineItem.toVanilla() = (this as ItemImpl).inner
fun CombineItemStack.toVanilla() = (this as ItemStackImpl).inner
fun ItemList.contains(item: Item) = contains(item.toCombine())
