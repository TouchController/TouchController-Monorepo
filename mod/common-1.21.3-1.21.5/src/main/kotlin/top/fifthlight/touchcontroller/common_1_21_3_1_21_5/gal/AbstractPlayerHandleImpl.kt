package top.fifthlight.touchcontroller.common_1_21_3_1_21_5.gal

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.*
import net.minecraft.world.entity.monster.Strider
import net.minecraft.world.entity.vehicle.AbstractBoat
import net.minecraft.world.entity.vehicle.AbstractMinecart
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.platform_1_21_3_1_21_5.ItemStackImpl
import top.fifthlight.combine.platform_1_21_3_1_21_5.toCombine
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.RidingEntityType
import top.fifthlight.touchcontroller.common_1_21_x.gal.AbstractPlayerHandleImpl as UpperAbstractPlayerHandleImpl

abstract class AbstractPlayerHandleImpl(
    inner: LocalPlayer,
) : UpperAbstractPlayerHandleImpl(inner) {
    override fun hasItemsOnHand(list: ItemList): Boolean = InteractionHand.entries.any { hand ->
        inner.getItemInHand(hand).toCombine().item in list
    }

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