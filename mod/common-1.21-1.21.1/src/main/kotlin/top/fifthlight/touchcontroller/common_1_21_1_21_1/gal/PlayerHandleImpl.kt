package top.fifthlight.touchcontroller.common_1_21_1_21_1.gal

import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.*
import net.minecraft.world.entity.monster.Strider
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.platform_1_21_1_21_1.ItemStackImpl
import top.fifthlight.combine.platform_1_21_1_21_1.toCombine
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.PlayerInventory
import top.fifthlight.touchcontroller.common.gal.RidingEntityType
import top.fifthlight.touchcontroller.helper.SyncableGameMode

@JvmInline
value class PlayerHandleImpl(val inner: LocalPlayer) : PlayerHandle {
    private val client: Minecraft
        get() = Minecraft.getInstance()

    override fun hasItemsOnHand(list: ItemList): Boolean = InteractionHand.entries.any { hand ->
        inner.getItemInHand(hand).toCombine().item in list
    }

    override fun changeLookDirection(deltaYaw: Double, deltaPitch: Double) {
        // Magic value 0.15 from net.minecraft.entity.Entity.turn
        inner.turn(deltaYaw / 0.15, deltaPitch / 0.15)
    }

    override var currentSelectedSlot: Int
        get() = inner.inventory.selected
        set(value) {
            inner.inventory.selected = value
        }

    override fun dropSlot(index: Int) {
        if (index == currentSelectedSlot) {
            inner.drop(true)
            return
        }

        val originalSlot = currentSelectedSlot
        val interactionManagerAccessor = client.gameMode as SyncableGameMode

        // Can it trigger anti-cheat?
        currentSelectedSlot = index
        interactionManagerAccessor.`touchcontroller$callSyncSelectedSlot`()

        inner.drop(true)

        currentSelectedSlot = originalSlot
        interactionManagerAccessor.`touchcontroller$callSyncSelectedSlot`()
    }

    override fun getInventorySlot(index: Int): ItemStack = ItemStackImpl(inner.inventory.getItem(index))

    override fun getInventory() = PlayerInventory(
        main = inner.inventory.items.map { it.toCombine() }.toPersistentList(),
        armor = inner.inventory.armor.map { it.toCombine() }.toPersistentList(),
        offHand = inner.inventory.offhand.first().toCombine(),
    )

    override val isUsingItem: Boolean
        get() = inner.isUsingItem

    override val onGround: Boolean
        get() = inner.onGround()

    override var isFlying: Boolean
        get() = inner.abilities.flying
        set(value) {
            inner.abilities.flying = value
        }

    override val isSubmergedInWater: Boolean
        get() = inner.isUnderWater

    override val isTouchingWater: Boolean
        get() = inner.isInWater

    override var isSprinting: Boolean
        get() = inner.isSprinting
        set(value) {
            inner.isSprinting = value
        }

    override val isSneaking: Boolean
        get() = inner.isSteppingCarefully

    override val ridingEntityType: RidingEntityType?
        get() = when (inner.vehicle) {
            null -> null
            is Minecart -> RidingEntityType.MINECART
            is Boat -> RidingEntityType.BOAT
            is Pig -> RidingEntityType.PIG
            is Camel -> RidingEntityType.CAMEL
            is Horse, is Donkey, is Mule, is ZombieHorse, is SkeletonHorse -> RidingEntityType.HORSE
            is Llama -> RidingEntityType.LLAMA
            is Strider -> RidingEntityType.STRIDER
            else -> RidingEntityType.OTHER
        }

    override val canFly: Boolean
        get() = inner.abilities.mayfly
}

object PlayerHandleFactoryImpl : PlayerHandleFactory {
    private val client = Minecraft.getInstance()

    override fun getPlayerHandle(): PlayerHandle? = client.player?.let(::PlayerHandleImpl)
}