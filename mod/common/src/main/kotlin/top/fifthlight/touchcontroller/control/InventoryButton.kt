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
import top.fifthlight.touchcontroller.layout.InventoryButton
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("inventory_button")
data class InventoryButton(
    val size: Float = 1f,
    override val id: Uuid = Uuid.random(),
    override val align: Align = Align.CENTER_BOTTOM,
    override val offset: IntOffset = IntOffset(101, 0),
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget() {
    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<InventoryButton, *>>(
            FloatProperty(
                getValue = { it.size },
                setValue = { config, value -> config.copy(size = value) },
                range = .5f..4f,
                messageFormatter = {
                    textFactory.format(
                        Texts.WIDGET_CHAT_BUTTON_PROPERTY_SIZE,
                        round(it * 100f).toString()
                    )
                }
            )
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    override fun size(): IntSize = IntSize((size * 22).toInt())

    override fun layout(context: Context) {
        context.InventoryButton()
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