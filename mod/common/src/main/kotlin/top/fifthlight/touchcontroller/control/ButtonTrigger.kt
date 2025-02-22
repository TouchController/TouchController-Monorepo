package top.fifthlight.touchcontroller.control

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.touchcontroller.gal.KeyBindingType

@Serializable
sealed class ButtonTrigger {
    @Serializable
    @SerialName("press")
    data class Press(
        val down: WidgetTriggerAction = WidgetTriggerAction.Empty,
        val press: KeyBindingType? = null,
        val up: WidgetTriggerAction = WidgetTriggerAction.Empty,
    ) : ButtonTrigger()

    @Serializable
    @SerialName("lock")
    data class Lock(
        val method: LockMethod = LockMethod.SINGLE_CLICK,
        val key: KeyBindingType,
    ) : ButtonTrigger() {
        @Serializable
        enum class LockMethod {
            @Serializable
            @SerialName("single_click")
            SINGLE_CLICK,

            @Serializable
            @SerialName("double_click")
            DOUBLE_CLICK,
        }
    }
}