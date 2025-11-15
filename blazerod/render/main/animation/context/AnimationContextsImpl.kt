package top.fifthlight.blazerod.animation.context

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import top.fifthlight.blazerod.api.animation.AnimationContexts
import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl

@ActualImpl(AnimationContexts::class)
object AnimationContextsImpl : AnimationContexts {
    @JvmStatic
    @ActualConstructor("create")
    fun create(): AnimationContexts = this

    override fun base() = BaseAnimationContext()

    override fun entity(entity: Entity) = EntityAnimationContext(entity)

    override fun livingEntity(entity: LivingEntity) = LivingEntityAnimationContext(entity)

    override fun player(player: PlayerEntity) = PlayerEntityAnimationContext(player)
}