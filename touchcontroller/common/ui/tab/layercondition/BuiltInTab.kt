package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.FlowRow
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.condition.BuiltinLayerConditionKey
import top.fifthlight.touchcontroller.common.ui.component.ListButton

object BuiltInTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_MOTION)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_BUILTIN

    @Composable
    override fun Content() {
        val layerConditionTabContext = LocalLayerConditionTabContext.current
        FlowRow(
            modifier = Modifier
                .padding(4)
                .verticalScroll()
                .fillMaxSize(),
            maxColumns = 2,
            expandColumnWidth = true,
        ) {
            for (key in BuiltinLayerConditionKey.Key.entries) {
                ListButton(
                    onClick = {
                        layerConditionTabContext.onConditionAdded(
                            BuiltinLayerConditionKey(key)
                        )
                    }
                ) {
                    Text(Text.translatable(key.text))
                }
            }
        }
    }
}
