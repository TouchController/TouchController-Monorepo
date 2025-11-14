package top.fifthlight.touchcontroller.common.ui.model

import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltInPresetKey
import top.fifthlight.touchcontroller.common.ext.mapState

class ManageControlPresetsTabModel(
    private val configScreenModel: ConfigScreenModel
) : TouchControllerScreenModel() {
    val presetConfig = configScreenModel.uiState.mapState {
        when (val preset = it.config.preset) {
            is PresetConfig.BuiltIn -> preset
            is PresetConfig.Custom -> null
        }
    }

    fun update(config: PresetConfig.BuiltIn) {
        configScreenModel.updateConfig {
            copy(preset = config)
        }
    }

    fun updateKey(key: BuiltInPresetKey) {
        configScreenModel.updateConfig {
            if (preset is PresetConfig.BuiltIn) {
                copy(preset = preset.copy(key = key))
            } else {
                this
            }
        }
    }
}