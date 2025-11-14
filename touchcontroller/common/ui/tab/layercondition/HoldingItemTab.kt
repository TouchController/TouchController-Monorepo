package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.condition.HoldingItemConditions
import top.fifthlight.touchcontroller.common.ui.component.ItemChooser

object HoldingItemTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_BLOCK)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_HOLDING_ITEM

    @Composable
    override fun Content() {
        val layerConditionTabContext = LocalLayerConditionTabContext.current
        ItemChooser(
            onItemChosen = {
                layerConditionTabContext.onConditionAdded(HoldingItemConditions(it))
            }
        )
    }
}