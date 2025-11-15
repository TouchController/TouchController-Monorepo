package top.fifthlight.blazerod.model.bedrock.molang.context

import team.unnamed.mocha.runtime.value.ObjectProperty
import team.unnamed.mocha.runtime.value.ObjectValue
import team.unnamed.mocha.util.CaseInsensitiveStringHashMap
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.bedrock.molang.binding.*

object QueryContext : ObjectValue {
    private val entries = CaseInsensitiveStringHashMap<ObjectProperty>()
    override fun getProperty(name: String) = entries[name]

    private lateinit var context: AnimationContext

    private var animationTotalTime = 0f
    private var animationPlayTime = 0f
    private var allAnimationsFinished = false
    private var anyAnimationFinished = false

    init {
        entries["anim_time"] = numberProperty<AnimationContext> { 0.0 }
        entries["life_time"] = numberProperty<AnimationContext> { 0.0 }
        entries["all_animations_finished"] = booleanProperty<AnimationContext> { false }
        entries["any_animation_finished"] = booleanProperty<AnimationContext> { false }

        // Entity
        entries["position"] = vector3dProperty(AnimationContext.Property.EntityPosition)
        entries["position_delta"] = vector3dProperty(AnimationContext.Property.EntityPositionDelta)
        entries["cardinal_facing_2d"] = intProperty(AnimationContext.Property.EntityHorizontalFacing)
        entries["ground_speed"] = doubleProperty(AnimationContext.Property.EntityGroundSpeed)
        entries["vertical_speed"] = doubleProperty(AnimationContext.Property.EntityVerticalSpeed)
        entries["has_rider"] = booleanProperty(AnimationContext.Property.EntityHasRider)
        entries["is_riding"] = booleanProperty(AnimationContext.Property.EntityIsRiding)
        entries["is_in_water"] = booleanProperty(AnimationContext.Property.EntityIsInWater)
        entries["is_in_fire"] = booleanProperty(AnimationContext.Property.EntityIsInFire)
        entries["is_on_ground"] = booleanProperty(AnimationContext.Property.EntityIsOnGround)

        // LivingEntity
        entries["health"] = floatProperty(AnimationContext.Property.LivingEntityHealth)
        entries["max_health"] = floatProperty(AnimationContext.Property.LivingEntityMaxHealth)
        entries["hurt_time"] = intProperty(AnimationContext.Property.LivingEntityHurtTime)
        entries["is_dead"] = booleanProperty(AnimationContext.Property.LivingEntityIsDead)
        entries["equipment_count"] = intProperty(AnimationContext.Property.LivingEntityEquipmentCount)

        // Player
        entries["head_x_rotation"] = floatProperty(AnimationContext.Property.PlayerHeadXRotation)
        entries["head_y_rotation"] = floatProperty(AnimationContext.Property.PlayerHeadYRotation)
        entries["body_x_rotation"] = floatProperty(AnimationContext.Property.PlayerBodyXRotation)
        entries["body_y_rotation"] = floatProperty(AnimationContext.Property.PlayerBodyYRotation)
        entries["is_first_person"] = booleanProperty(AnimationContext.Property.PlayerIsFirstPerson)
        entries["is_spectator"] = booleanProperty(AnimationContext.Property.PlayerIsSpectator)
        entries["is_sneaking"] = booleanProperty(AnimationContext.Property.PlayerIsSneaking)
        entries["is_sprinting"] = booleanProperty(AnimationContext.Property.PlayerIsSprinting)
        entries["is_swimming"] = booleanProperty(AnimationContext.Property.PlayerIsSwimming)
        entries["is_eating"] = booleanProperty(AnimationContext.Property.PlayerIsEating)
        entries["is_using_item"] = booleanProperty(AnimationContext.Property.PlayerIsUsingItem)
        entries["is_jumping"] = booleanProperty(AnimationContext.Property.PlayerIsJumping)
        entries["is_sleeping"] = booleanProperty(AnimationContext.Property.PlayerIsSleeping)
        entries["player_level"] = intProperty(AnimationContext.Property.PlayerLevel)

        // World
        entries["moon_phase"] = intProperty(AnimationContext.Property.WorldMoonPhase)
        entries["time_of_day"] = floatProperty(AnimationContext.Property.WorldTimeOfDay)
        entries["time_stamp"] = intProperty(AnimationContext.Property.WorldTimeStamp)
    }
}
