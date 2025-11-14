package top.fifthlight.touchcontroller.common.ui.state

import top.fifthlight.touchcontroller.common.config.LayoutLayer

sealed class LayersTabState {
    data object Empty : LayersTabState()

    data class Delete(
        val index: Int,
        val layer: LayoutLayer,
    ) : LayersTabState()
}
