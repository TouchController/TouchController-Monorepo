package top.fifthlight.touchcontroller.control

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
import top.fifthlight.touchcontroller.layout.Align
import top.fifthlight.touchcontroller.layout.Context
import top.fifthlight.touchcontroller.layout.ScreenshotButton
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("screenshot_button")
data class ScreenshotButton(
    val size: Float = 1f,
    val classic: Boolean = true,
    override val id: Uuid = Uuid.random(),
    override val align: Align = Align.CENTER_TOP,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget() {
    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<ScreenshotButton, *>>(
            FloatProperty(
                getValue = { it.size },
                setValue = { config, value -> config.copy(size = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_SCREENSHOT_BUTTON_PROPERTY_SIZE,
                        round(it * 100f).toString()
                    )
                }
            ),
            BooleanProperty(
                getValue = { it.classic },
                setValue = { config, value -> config.copy(classic = value) },
                message = textFactory.of(Texts.WIDGET_SCREENSHOT_BUTTON_PROPERTY_CLASSIC),
            )
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    private val textureSize
        get() = 18

    override fun size(): IntSize = IntSize((size * textureSize).toInt())

    override fun layout(context: Context) {
        context.ScreenshotButton(config = this)
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