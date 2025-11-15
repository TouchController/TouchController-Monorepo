package top.fifthlight.blazerod.animation.context

import net.minecraft.entity.Entity
import net.minecraft.util.math.Direction
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.animation.AnimationContext.Property.*
import top.fifthlight.blazerod.model.animation.AnimationContext.RenderingTargetType
import top.fifthlight.blazerod.util.math.set
import top.fifthlight.blazerod.util.math.sub

open class EntityAnimationContext(
    open val entity: Entity,
) : BaseAnimationContext() {
    companion object {
        @JvmStatic
        protected val propertyTypes = BaseAnimationContext.propertyTypes + setOf(
            RenderTarget,
            EntityPosition,
            EntityPositionDelta,
            EntityHorizontalFacing,
            EntityGroundSpeed,
            EntityVerticalSpeed,
            EntityHasRider,
            EntityIsRiding,
            EntityIsInWater,
            EntityIsInWaterOrRain,
            EntityIsInFire,
            EntityIsOnGround,
        )
            @JvmName("getEntityPropertyTypes")
            get
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getProperty(type: AnimationContext.Property<T>): T? = when (type) {
        RenderTarget -> RenderingTargetType.ENTITY

        EntityPosition -> vector3dBuffer.set(entity.pos)

        EntityPositionDelta -> entity.pos.sub(entity.lastRenderPos, vector3dBuffer)

        EntityHorizontalFacing -> when (entity.horizontalFacing) {
            Direction.NORTH -> 2
            Direction.SOUTH -> 3
            Direction.WEST -> 4
            Direction.EAST -> 5
            Direction.UP, Direction.DOWN -> throw AssertionError("Invalid cardinal facing")
        }.let { intBuffer.apply { value = it } }

        EntityGroundSpeed -> doubleBuffer.apply {
            value = entity.movement.horizontalLength()
        }

        EntityVerticalSpeed -> doubleBuffer.apply { value = entity.movement.y }

        EntityHasRider -> booleanBuffer.apply { value = entity.hasPassengers() }

        EntityIsRiding -> booleanBuffer.apply { value = entity.hasVehicle() }

        EntityIsInWater -> booleanBuffer.apply { value = entity.isTouchingWater }

        EntityIsInWaterOrRain -> booleanBuffer.apply { value = entity.isTouchingWaterOrRain }

        EntityIsInFire -> booleanBuffer.apply { value = entity.isOnFire }

        EntityIsOnGround -> booleanBuffer.apply { value = entity.isOnGround }

        else -> super.getProperty(type)
    } as T?

    override fun getPropertyTypes() = propertyTypes
}