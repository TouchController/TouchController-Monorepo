package top.fifthlight.touchcontroller.common_1_21_x.gal

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.helper.SyncableGameMode

abstract class AbstractPlayerHandleImpl(
    val inner: LocalPlayer,
) : PlayerHandle {
    private val client: Minecraft
        get() = Minecraft.getInstance()

    override fun changeLookDirection(deltaYaw: Double, deltaPitch: Double) {
        // Magic value 0.15 from net.minecraft.entity.Entity.turn
        inner.turn(deltaYaw / 0.15, deltaPitch / 0.15)
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

    override val canFly: Boolean
        get() = inner.abilities.mayfly
}