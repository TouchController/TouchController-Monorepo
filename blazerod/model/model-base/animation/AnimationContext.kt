package top.fifthlight.blazerod.model.animation

import org.joml.Vector3d
import top.fifthlight.blazerod.model.util.*

interface AnimationContext {
    companion object {
        const val SECONDS_PER_TICK = 1f / 20f
    }

    enum class RenderingTargetType {
        PLAYER,
        MAID, // Reserved for TouhouLittleMaid
        ENTITY,
        BLOCK,
    }

    interface Property<T> {
        object GameTick : Property<LongWrapper>
        object DeltaTick : Property<FloatWrapper>

        object RenderTarget : Property<RenderingTargetType>

        // Entity
        object EntityPosition : Property<Vector3d>
        object EntityPositionDelta : Property<Vector3d>
        object EntityHorizontalFacing : Property<IntWrapper>
        object EntityGroundSpeed : Property<DoubleWrapper>
        object EntityVerticalSpeed : Property<DoubleWrapper>
        object EntityHasRider : Property<BooleanWrapper>
        object EntityIsRiding : Property<BooleanWrapper>
        object EntityIsInWater : Property<BooleanWrapper>
        object EntityIsInWaterOrRain : Property<BooleanWrapper>
        object EntityIsInFire : Property<BooleanWrapper>
        object EntityIsOnGround : Property<BooleanWrapper>

        // LivingEntity
        object LivingEntityHealth : Property<FloatWrapper>
        object LivingEntityMaxHealth : Property<FloatWrapper>
        object LivingEntityHurtTime : Property<IntWrapper>
        object LivingEntityIsDead : Property<BooleanWrapper>
        object LivingEntityEquipmentCount : Property<IntWrapper>

        // Player
        object PlayerHeadXRotation : Property<FloatWrapper>
        object PlayerHeadYRotation : Property<FloatWrapper>
        object PlayerBodyXRotation : Property<FloatWrapper>
        object PlayerBodyYRotation : Property<FloatWrapper>
        object PlayerIsFirstPerson : Property<BooleanWrapper>
        object PlayerPersonView : Property<IntWrapper>
        object PlayerIsSpectator : Property<BooleanWrapper>
        object PlayerIsSneaking : Property<BooleanWrapper>
        object PlayerIsSprinting : Property<BooleanWrapper>
        object PlayerIsSwimming : Property<BooleanWrapper>
        object PlayerIsEating : Property<BooleanWrapper>
        object PlayerIsUsingItem : Property<BooleanWrapper>
        object PlayerIsJumping : Property<BooleanWrapper>
        object PlayerIsSleeping : Property<BooleanWrapper>
        object PlayerLevel : Property<IntWrapper>
        object PlayerFoodLevel : Property<IntWrapper>

        // World
        object WorldMoonPhase : Property<IntWrapper>
        object WorldTimeOfDay : Property<FloatWrapper>
        object WorldTimeStamp : Property<IntWrapper>
        object WorldWeather : Property<IntWrapper>
        object WorldDimension : Property<String>

        // Game
        object GameFps : Property<IntWrapper>
    }

    // game ticks
    fun getGameTick(): Long

    // 0 ~ 1
    fun getDeltaTick(): Float

    fun <T> getProperty(type: Property<T>): T?

    fun getPropertyTypes(): Set<Property<*>>
}
