package top.fifthlight.blazerod.api.animation

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.mergetools.api.ExpectFactory

interface AnimationContexts {
    fun base(): AnimationContext
    fun entity(entity: Entity): AnimationContext
    fun livingEntity(entity: LivingEntity): AnimationContext
    fun player(player: PlayerEntity): AnimationContext

    @ExpectFactory
    interface Factory {
        fun create(): AnimationContexts
    }
}