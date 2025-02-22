package top.fifthlight.touchcontroller.control

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.inject
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.layout.Align
import top.fifthlight.touchcontroller.layout.Context
import top.fifthlight.touchcontroller.layout.ForwardButton
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
enum class ForwardButtonTexture {
    @SerialName("classic")
    CLASSIC,

    @SerialName("new")
    NEW,
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("forward_button")
data class ForwardButton(
    val size: Float = 2f,
    val texture: ForwardButtonTexture = ForwardButtonTexture.CLASSIC,
    override val id: Uuid = Uuid.random(),
    override val align: Align = Align.LEFT_BOTTOM,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget() {
    companion object {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<ForwardButton, *>>(
            FloatProperty(
                getValue = { it.size },
                setValue = { config, value -> config.copy(size = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_FORWARD_BUTTON_PROPERTY_SIZE,
                        round(it * 100f).toString()
                    )
                }
            ),
            EnumProperty(
                getValue = { it.texture },
                setValue = { config, value -> config.copy(texture = value) },
                name = textFactory.of(Texts.WIDGET_FORWARD_BUTTON_PROPERTY_STYLE),
                items = listOf(
                    ForwardButtonTexture.CLASSIC to textFactory.of(Texts.WIDGET_FORWARD_BUTTON_PROPERTY_STYLE_CLASSIC),
                    ForwardButtonTexture.NEW to textFactory.of(Texts.WIDGET_FORWARD_BUTTON_PROPERTY_STYLE_NEW),
                ),
            ),
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    private val textureSize
        get() = 22

    override fun size(): IntSize = IntSize((size * textureSize).toInt())


    override fun layout(context: Context) {
        context.ForwardButton(this@ForwardButton)
    }

    override fun cloneBase(
        id: Uuid,
        align: Align,
        offset: IntOffset,
        opacity: Float,
        lockMoving: Boolean
    ) = copy(
        align = align,
        offset = offset,
        opacity = opacity,
        lockMoving = lockMoving,
    )
}