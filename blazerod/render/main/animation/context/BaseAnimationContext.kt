package top.fifthlight.blazerod.animation.context

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.world.ClientWorld
import org.joml.Vector3d
import top.fifthlight.blazerod.mixin.MinecraftClientAccessor
import top.fifthlight.blazerod.model.animation.AnimationContext
import top.fifthlight.blazerod.model.util.*
import kotlin.jvm.optionals.getOrNull

open class BaseAnimationContext(
    protected val client: MinecraftClient = MinecraftClient.getInstance(),
) : AnimationContext {
    protected val tickCounter: RenderTickCounter = client.renderTickCounter
    protected val world: ClientWorld?
        get() = client.world

    override fun getGameTick(): Long = (client as MinecraftClientAccessor).uptimeInTicks

    override fun getDeltaTick(): Float = tickCounter.getTickProgress(false)

    protected val vector3dBuffer = Vector3d()
    protected val intBuffer = MutableInt()
    protected val longBuffer = MutableLong()
    protected val floatBuffer = MutableFloat()
    protected val doubleBuffer = MutableDouble()
    protected val booleanBuffer = MutableBoolean()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getProperty(type: AnimationContext.Property<T>): T? = when (type) {
        AnimationContext.Property.GameTick -> longBuffer.apply { value = getGameTick() }

        AnimationContext.Property.DeltaTick -> floatBuffer.apply { value = getDeltaTick() }

        AnimationContext.Property.WorldTimeOfDay -> world?.let { world ->
            floatBuffer.apply {
                value = world.timeOfDay.toFloat() / 24000f
            }
        }

        AnimationContext.Property.WorldTimeStamp -> world?.let { world ->
            intBuffer.apply {
                value = world.timeOfDay.toInt()
            }
        }

        AnimationContext.Property.WorldMoonPhase -> world?.let { world ->
            intBuffer.apply {
                value = world.moonPhase
            }
        }

        AnimationContext.Property.WorldWeather -> world?.let { world ->
            intBuffer.apply {
                value = when {
                    !world.isRaining -> 0
                    world.isThundering -> 2
                    else -> 1
                }
            }
        }

        AnimationContext.Property.WorldDimension -> world?.let { world ->
            world.dimensionEntry.key?.getOrNull()?.value?.toString()
        }

        AnimationContext.Property.GameFps -> intBuffer.apply {
            value = client.currentFps
        }

        else -> null
    } as T?

    companion object {
        @JvmStatic
        protected val propertyTypes = setOf(
            AnimationContext.Property.GameTick,
            AnimationContext.Property.DeltaTick,
            AnimationContext.Property.WorldTimeOfDay,
            AnimationContext.Property.WorldTimeStamp,
            AnimationContext.Property.WorldMoonPhase,
            AnimationContext.Property.WorldWeather,
            AnimationContext.Property.WorldDimension,
            AnimationContext.Property.GameFps,
        )
            @JvmName("getBasePropertyTypes")
            get

        val instance = BaseAnimationContext()
    }

    override fun getPropertyTypes(): Set<AnimationContext.Property<*>> = propertyTypes
}
