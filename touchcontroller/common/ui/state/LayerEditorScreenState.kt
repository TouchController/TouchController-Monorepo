package top.fifthlight.touchcontroller.common.ui.state

import top.fifthlight.touchcontroller.common.config.LayoutLayer
import top.fifthlight.touchcontroller.common.config.condition.LayerConditions

data class LayerEditorScreenState(
    val name: String,
    val conditions: LayerConditions,
) {
    constructor(layer: LayoutLayer) : this(
        name = layer.name,
        conditions = layer.conditions,
    )

    fun edit(layer: LayoutLayer) = layer.copy(
        name = name,
        conditions = conditions,
    )
}
