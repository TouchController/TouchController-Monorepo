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
import top.fifthlight.touchcontroller.common.layout.BoatButton
import top.fifthlight.touchcontroller.common.layout.Context
import kotlin.math.round
import kotlin.uuid.Uuid

@Serializable
enum class BoatButtonSide {
    @SerialName("left")
    LEFT,

    @SerialName("right")
    RIGHT
}

@Serializable
@SerialName("boat_button")
data class BoatButton(
    val textureSet: TextureSet.TextureSetKey = TextureSet.TextureSetKey.CLASSIC,
    val size: Float = 3f,
    val side: BoatButtonSide = BoatButtonSide.LEFT,
    override val id: Uuid = fastRandomUuid(),
    override val name: Name = Name.Translatable(Texts.WIDGET_BOAT_BUTTON_NAME),
    override val align: Align = Align.LEFT_BOTTOM,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget() {
    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<BoatButton, *>>(
            FloatProperty(
                getValue = { it.size },
                setValue = { config, value -> config.copy(size = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_BOAT_BUTTON_PROPERTY_SIZE,
                        round(it * 100f).toString()
                    )
                },
            ),
            TextureSetProperty(
                textFactory = textFactory,
                getValue = { it.textureSet },
                setValue = { config, value -> config.copy(textureSet = value) },
                name = textFactory.of(Texts.WIDGET_BOAT_BUTTON_PROPERTY_TEXTURE_SET),
            ),
            EnumProperty(
                getValue = { it.side },
                setValue = { config, value -> config.copy(side = value) },
                name = textFactory.of(Texts.WIDGET_BOAT_BUTTON_PROPERTY_SIDE),
                items = persistentListOf(
                    BoatButtonSide.LEFT to textFactory.of(Texts.WIDGET_BOAT_BUTTON_PROPERTY_SIDE_LEFT),
                    BoatButtonSide.RIGHT to textFactory.of(Texts.WIDGET_BOAT_BUTTON_PROPERTY_SIDE_RIGHT),
                )
            ),
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    private val textureSize
        get() = textureSet.textureSet.up.size

    override fun size(): IntSize = (textureSize.toSize() * size).toIntSize()

    val classic = textureSet == TextureSet.TextureSetKey.CLASSIC || textureSet == TextureSet.TextureSetKey.CLASSIC_EXTENSION

    override fun layout(context: Context) {
        context.BoatButton(this)
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