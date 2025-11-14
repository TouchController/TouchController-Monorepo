package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.touchcontroller.common.config.condition.LayerConditions
import top.fifthlight.touchcontroller.common.config.preset.LayerCustomConditions
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset

data class LayerConditionTabContext(
    val preset: LayoutPreset,
    val onCustomConditionsChanged: (LayerCustomConditions) -> Unit,
    val onConditionAdded: (LayerConditions.Key) -> Unit,
)

val LocalLayerConditionTabContext =
    compositionLocalOf<LayerConditionTabContext> { error("No LayerConditionTabContext") }

abstract class LayerConditionTab : Screen {
    @Composable
    abstract fun Icon()

    abstract val name: Identifier
}

val allLayerConditionTabs = persistentListOf(
    BuiltInTab,
    HoldingItemTab,
    CustomTab,
)
