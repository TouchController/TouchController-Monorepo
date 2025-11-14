package top.fifthlight.touchcontroller.common.config

import kotlinx.serialization.Serializable
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

@Serializable
data class RegularConfig(
    val disableMouseMove: Boolean = true,
    val disableMouseClick: Boolean = true,
    val disableMouseLock: Boolean = false,
    val disableHotBarKey: Boolean = false,
    val vibration: Boolean = true,
    val quickHandSwap: Boolean = false,
)

@Serializable
data class ControlConfig(
    val viewMovementSensitivity: Float = 495f,
    val viewHoldDetectThreshold: Int = 2,
    val viewHoldDetectTicks: Int = 5,
)

@Serializable
data class TouchRingConfig(
    val radius: Int = 36,
    val outerRadius: Int = 2,
    val initialProgress: Float = .5f
)

@Serializable
data class DebugConfig(
    val showPointers: Boolean = false,
    val enableTouchEmulation: Boolean = false,
)

@Serializable
data class ItemConfig(
    val usableItems: ItemList,
    val showCrosshairItems: ItemList,
    val crosshairAimingItems: ItemList,
) {
    companion object {
        fun default(itemListProvider: DefaultItemListProvider) = ItemConfig(
            usableItems = itemListProvider.usableItems,
            showCrosshairItems = itemListProvider.showCrosshairItems,
            crosshairAimingItems = itemListProvider.crosshairAimingItems,
        )
    }
}

@Serializable
data class ChatConfig(
    val lineSpacing: Int = 0,
    val textColor: Color = Colors.WHITE,
)

@Serializable
data class GlobalConfig(
    val regular: RegularConfig = RegularConfig(),
    val control: ControlConfig = ControlConfig(),
    val touchRing: TouchRingConfig = TouchRingConfig(),
    val debug: DebugConfig = DebugConfig(),
    val item: ItemConfig,
    val preset: PresetConfig = PresetConfig.BuiltIn(),
    val chat: ChatConfig = ChatConfig(),
) {
    companion object {
        fun default(itemListProvider: DefaultItemListProvider) = GlobalConfig(
            item = ItemConfig.default(itemListProvider),
        )
    }
}
