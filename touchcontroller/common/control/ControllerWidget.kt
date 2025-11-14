package top.fifthlight.touchcontroller.common.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.layout.Align
import top.fifthlight.touchcontroller.common.layout.Context
import kotlin.math.round
import kotlin.uuid.Uuid

@Immutable
@Serializable
sealed class ControllerWidget {
    abstract val id: Uuid
    abstract val name: Name
    abstract val align: Align
    abstract val offset: IntOffset
    abstract val opacity: Float
    abstract val lockMoving: Boolean

    @Immutable
    @Serializable
    sealed class Name {
        @Serializable
        @SerialName("translatable")
        data class Translatable(val identifier: Identifier) : Name()

        @Serializable
        @SerialName("literal")
        data class Literal(val string: String) : Name()

        fun getText(textFactory: TextFactory) = when (this) {
            is Translatable -> textFactory.of(identifier)
            is Literal -> textFactory.literal(string)
        }

        @Composable
        fun getText() = getText(LocalTextFactory.current)

        fun asString(textFactory: TextFactory) = getText(textFactory).string

        @Composable
        fun asString() = getText().string
    }

    abstract class Property<Config : ControllerWidget, Value>(
        val getValue: (Config) -> Value,
        val setValue: (Config, Value) -> Config,
    ) {
        @Composable
        abstract fun controller(
            modifier: Modifier,
            config: ControllerWidget,
            currentPreset: LayoutPreset?,
            onConfigChanged: (ControllerWidget) -> Unit,
        )
    }

    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        val properties = persistentListOf<Property<ControllerWidget, *>>(
            NameProperty(
                getValue = { it.name },
                setValue = { config, value ->
                    config.cloneBase(name = value)
                },
                name = textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_NAME),
            ),
            BooleanProperty(
                getValue = { it.lockMoving },
                setValue = { config, value ->
                    config.cloneBase(lockMoving = value)
                },
                name = textFactory.of(Texts.WIDGET_GENERAL_PROPERTY_LOCK_MOVING),
            ),
            AnchorProperty(),
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
        name: Name = this.name,
        align: Align = this.align,
        offset: IntOffset = this.offset,
        opacity: Float = this.opacity,
        lockMoving: Boolean = this.lockMoving,
    ): ControllerWidget

    open fun newId() = cloneBase(
        id = fastRandomUuid(),
    )
}
