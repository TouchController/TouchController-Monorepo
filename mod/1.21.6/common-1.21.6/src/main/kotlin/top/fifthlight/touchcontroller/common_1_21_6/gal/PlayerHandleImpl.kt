package top.fifthlight.touchcontroller.common_1_21_6.gal

import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.*
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.Donkey
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.entity.animal.horse.Mule
import net.minecraft.world.entity.animal.horse.SkeletonHorse
import net.minecraft.world.entity.animal.horse.ZombieHorse
import net.minecraft.world.entity.monster.Strider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.vehicle.AbstractBoat
import net.minecraft.world.entity.vehicle.AbstractMinecart
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.platform_1_21_6.ItemStackImpl
import top.fifthlight.combine.platform_1_21_6.toCombine
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.PlayerInventory
import top.fifthlight.touchcontroller.common.gal.RidingEntityType
import top.fifthlight.touchcontroller.common_1_21_x.gal.AbstractPlayerHandleImpl

class PlayerHandleImpl(
    inner: LocalPlayer,
) : AbstractPlayerHandleImpl(inner) {
    override var currentSelectedSlot: Int
        get() = inner.inventory.selectedSlot
        set(value) {
            inner.inventory.selectedSlot = value
        }

    override fun hasItemsOnHand(list: ItemList): Boolean = InteractionHand.entries.any { hand ->
        inner.getItemInHand(hand).toCombine().item in list
    }

    override fun getInventory() = PlayerInventory(
        main = inner.inventory.nonEquipmentItems.map { it.toCombine() }.toPersistentList(),
        armor = Inventory.EQUIPMENT_SLOT_MAPPING
            .filterValues { it != EquipmentSlot.OFFHAND }
            .keys
            .map { inner.inventory.getItem(it).toCombine() }
            .toPersistentList(),
        offHand = inner.inventory.getItem(Inventory.SLOT_OFFHAND).toCombine(),
    )

    override fun getInventorySlot(index: Int): ItemStack = ItemStackImpl(inner.inventory.getItem(index))

    override val ridingEntityType: RidingEntityType?
        get() = when (inner.vehicle) {
            null -> null
            is AbstractMinecart -> RidingEntityType.MINECART
            is AbstractBoat -> RidingEntityType.BOAT
            is Pig -> RidingEntityType.PIG
            is Camel -> RidingEntityType.CAMEL
            is Horse, is Donkey, is Mule, is ZombieHorse, is SkeletonHorse -> RidingEntityType.HORSE
            is Llama -> RidingEntityType.LLAMA
            is Strider -> RidingEntityType.STRIDER
            else -> RidingEntityType.OTHER
        }
}


object PlayerHandleFactoryImpl : PlayerHandleFactory {
    private val client = Minecraft.getInstance()

    override fun getPlayerHandle(): PlayerHandle? = client.player?.let(::PlayerHandleImpl)
}