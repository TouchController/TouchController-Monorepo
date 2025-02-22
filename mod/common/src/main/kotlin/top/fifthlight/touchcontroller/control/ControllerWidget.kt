package top.fifthlight.touchcontroller.control

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.layout.Align
import top.fifthlight.touchcontroller.layout.Context
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed class ControllerWidget {
    abstract val id: Uuid
    abstract val align: Align
    abstract val offset: IntOffset
    abstract val opacity: Float
    abstract val lockMoving: Boolean

    interface Property<Config : ControllerWidget, Value> {
        @Composable
        fun controller(
            modifier: Modifier,
            config: ControllerWidget,
            onConfigChanged: (ControllerWidget) -> Unit
        )
    }

    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        val properties = persistentListOf<Property<ControllerWidget, *>>(
            BooleanProperty(
                getValue = { it.lockMoving },
                setValue = { config, value ->
                    config.cloneBase(lockMoving = value)
                },
                message = textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_LOCK_MOVING),
            ),
            EnumProperty(
                getValue = { it.align },
                setValue = { config, value ->
                    config.cloneBase(
                        align = value,
                        offset = IntOffset.ZERO,
                    )
                },
                name = textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_NAME),
                items = listOf(
                    Align.LEFT_TOP to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_LEFT),
                    Align.LEFT_CENTER to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_LEFT),
                    Align.LEFT_BOTTOM to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_LEFT),
                    Align.CENTER_TOP to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_CENTER),
                    Align.CENTER_CENTER to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_CENTER),
                    Align.CENTER_BOTTOM to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_CENTER),
                    Align.RIGHT_TOP to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_RIGHT),
                    Align.RIGHT_CENTER to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_RIGHT),
                    Align.RIGHT_BOTTOM to textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_RIGHT),
                ),
            ),
            FloatProperty(
                getValue = { it.opacity },
                setValue = { config, value -> config.cloneBase(opacity = value) },
                messageFormatter = { opacity ->
                    textFactory.format(
                        Texts.WIDGET_GENERAL_PROPERTY_OPACITY,
                        round(opacity * 100f).toInt().toString()
                    )
                }
            )
        )
    }

    @Transient
    open val properties: PersistentList<Property<ControllerWidget, *>> = Companion.properties

    abstract fun size(): IntSize

    abstract fun layout(context: Context)

    abstract fun cloneBase(
        id: Uuid = this.id,
        align: Align = this.align,
        offset: IntOffset = this.offset,
        opacity: Float = this.opacity,
        lockMoving: Boolean = this.lockMoving,
    ): ControllerWidget

    open fun newId() = cloneBase()
}
