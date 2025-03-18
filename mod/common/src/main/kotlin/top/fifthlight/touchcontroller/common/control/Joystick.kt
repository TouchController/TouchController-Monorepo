package top.fifthlight.touchcontroller.common.control

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.layout.Align
import top.fifthlight.touchcontroller.common.layout.Context
import top.fifthlight.touchcontroller.common.layout.Joystick
import kotlin.math.round
import kotlin.uuid.Uuid

@Serializable
@SerialName("joystick")
data class Joystick(
    val textureSet: TextureSet.TextureSetKey = TextureSet.TextureSetKey.NEW,
    val size: Float = 1.5f,
    val stickSize: Float = 1f,
    val triggerSprint: Boolean = false,
    val increaseOpacityWhenActive: Boolean = true,
    override val id: Uuid = fastRandomUuid(),
    override val name: Name = Name.Translatable(Texts.WIDGET_JOYSTICK_NAME),
    override val align: Align = Align.LEFT_BOTTOM,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget() {
    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<Joystick, *>>(
            FloatProperty(
                getValue = { it.size },
                setValue = { config, value -> config.copy(size = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_JOYSTICK_PROPERTY_SIZE,
                        round(it * 100f).toString()
                    )
                },
            ),
            TextureSetProperty(
                textFactory = textFactory,
                getValue = { it.textureSet },
                setValue = { config, value -> config.copy(textureSet = value) },
                name = textFactory.of(Texts.WIDGET_JOYSTICK_PROPERTY_TEXTURE_SET),
            ),
            FloatProperty(
                getValue = { it.stickSize },
                setValue = { config, value -> config.copy(stickSize = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_JOYSTICK_PROPERTY_STICK_SIZE,
                        round(it * 100f).toString()
                    )
                },
            ),
            BooleanProperty(
                getValue = { it.triggerSprint },
                setValue = { config, value -> config.copy(triggerSprint = value) },
                name = textFactory.of(Texts.WIDGET_JOYSTICK_PROPERTY_TRIGGER_SPRINT),
            ),
            BooleanProperty(
                getValue = { it.increaseOpacityWhenActive },
                setValue = { config, value -> config.copy(increaseOpacityWhenActive = value) },
                name = textFactory.of(Texts.WIDGET_JOYSTICK_PROPERTY_INCREASE_OPACITY_WHEN_ACTIVE),
            )
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    override fun size(): IntSize = IntSize((size * 72).toInt())

    fun stickSize() = IntSize((stickSize * 48).toInt())

    override fun layout(context: Context) {
        context.Joystick(this@Joystick)
    }

    override fun cloneBase(
        id: Uuid,
        name: Name,
        align: Align,
        offset: IntOffset,
        opacity: Float,
        lockMoving: Boolean
    ) = copy(
        id = id,
        name = name,
        align = align,
        offset = offset,
        opacity = opacity,
        lockMoving = lockMoving,
    )
}