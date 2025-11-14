package top.fifthlight.touchcontroller.common.ui.state

import top.fifthlight.touchcontroller.common.config.preset.CustomCondition

data class LayoutEditorCustomTabState(
    val editState: EditState? = null,
) {
    data class EditState(
        val index: Int,
        val name: String? = null,
    ) {
        fun edit(customCondition: CustomCondition) = customCondition.copy(name = name)
    }
}
