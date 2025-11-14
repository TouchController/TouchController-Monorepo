package top.fifthlight.touchcontroller.common.gal

import kotlinx.collections.immutable.PersistentList
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.touchcontroller.common.config.ItemList

enum class RidingEntityType {
    MINECART,
    BOAT,
    PIG,
    HORSE,
    LLAMA,
    CAMEL,
    STRIDER,
    OTHER,
}

data class PlayerInventory(
    val main: PersistentList<ItemStack>,
    val armor: PersistentList<ItemStack>,
    val offHand: ItemStack?,
)

interface PlayerHandle {
    fun matchesItemOnHand(item: Item): Boolean
    fun hasItemsOnHand(list: ItemList): Boolean
    fun changeLookDirection(deltaYaw: Double, deltaPitch: Double)
    var currentSelectedSlot: Int
    fun dropSlot(index: Int)
    fun getInventorySlot(index: Int): ItemStack
    fun getInventory(): PlayerInventory
    val isUsingItem: Boolean
    val onGround: Boolean
    var isFlying: Boolean
    val isSubmergedInWater: Boolean
    val isTouchingWater: Boolean
    var isSprinting: Boolean
    val isSneaking: Boolean
    val ridingEntityType: RidingEntityType?
    val canFly: Boolean
}

interface PlayerHandleFactory {
    fun getPlayerHandle(): PlayerHandle?
}