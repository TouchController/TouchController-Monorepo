package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltInPresetKey

class ImportPresetScreenModel(
    private val onPresetKeySelected: (BuiltInPresetKey) -> Unit,
) : TouchControllerScreenModel() {
    private val _key = MutableStateFlow(BuiltInPresetKey())
    val key = _key.asStateFlow()

    fun updateKey(key: BuiltInPresetKey) {
        _key.value = key
    }

    fun finish() {
        onPresetKeySelected(key.value)
    }
}
