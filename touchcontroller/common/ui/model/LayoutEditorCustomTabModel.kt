package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.collections.immutable.plus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.fifthlight.touchcontroller.common.config.preset.CustomCondition
import top.fifthlight.touchcontroller.common.config.preset.LayerCustomConditions
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset
import top.fifthlight.touchcontroller.common.ui.state.LayoutEditorCustomTabState
import top.fifthlight.touchcontroller.common.ui.tab.layercondition.LayerConditionTabContext

class LayoutEditorCustomTabModel(
    private val context: LayerConditionTabContext,
) : TouchControllerScreenModel() {
    private val _uiState = MutableStateFlow(LayoutEditorCustomTabState())
    val uiState = _uiState.asStateFlow()

    fun addCondition(preset: LayoutPreset) {
        context.onCustomConditionsChanged(
            LayerCustomConditions(
                preset.controlInfo.customConditions.conditions + CustomCondition()
            )
        )
    }

    fun openEditConditionDialog(index: Int, condition: CustomCondition) {
        _uiState.value = _uiState.value.copy(
            editState = LayoutEditorCustomTabState.EditState(
                index = index,
                name = condition.name,
            )
        )
    }

    fun closeEditConditionDialog() {
        _uiState.value = _uiState.value.copy(editState = null)
    }

    fun editCondition(preset: LayoutPreset, index: Int, newCondition: CustomCondition) {
        context.onCustomConditionsChanged(
            LayerCustomConditions(
                preset.controlInfo.customConditions.conditions.set(index, newCondition)
            )
        )
    }

    fun removeCondition(preset: LayoutPreset, index: Int) {
        context.onCustomConditionsChanged(
            LayerCustomConditions(
                preset.controlInfo.customConditions.conditions.removeAt(index)
            )
        )
    }

    fun updateEditState(editor: LayoutEditorCustomTabState.EditState.() -> LayoutEditorCustomTabState.EditState) {
        _uiState.value = _uiState.value.copy(
            editState = _uiState.value.editState?.editor()
        )
    }
}