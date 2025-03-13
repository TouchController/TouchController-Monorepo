package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.config.ControllerLayout
import top.fifthlight.touchcontroller.common.config.LayoutLayer
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.config.preset.PresetManager
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltinPresetKey
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.ext.combineStates
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.ui.state.CustomControlLayoutTabState
import kotlin.uuid.Uuid

class CustomControlLayoutTabModel(
    private val configScreenModel: ConfigScreenModel,
) : TouchControllerScreenModel() {
    private val presetManager: PresetManager by inject()
    private val pageState = MutableStateFlow(CustomControlLayoutTabState.Enabled.PageState())
    val uiState =
        combineStates(configScreenModel.uiState, presetManager.presets, pageState) { uiState, presets, selectState ->
            val config = uiState.config
            when (val preset = config.preset) {
                is PresetConfig.BuiltIn -> CustomControlLayoutTabState.Disabled
                is PresetConfig.Custom -> {
                    val selectedPreset = selectState.editState?.undoStack?.currentItem
                    val selectedLayer = selectedPreset?.layout?.getOrNull(selectState.selectedLayerIndex)
                    val selectedWidget = selectedLayer?.widgets?.getOrNull(selectState.selectedWidgetIndex)
                    CustomControlLayoutTabState.Enabled(
                        allPresets = presets,
                        selectedPresetUuid = preset.uuid,
                        selectedPreset = selectedPreset,
                        selectedLayer = selectedLayer,
                        selectedWidget = selectedWidget,
                        pageState = selectState,
                    )
                }
            }
        }

    init {
        coroutineScope.launch {
            configScreenModel.uiState.collectLatest { uiState ->
                val config = uiState.config
                val presetConfig = (config.preset as? PresetConfig.Custom) ?: return@collectLatest
                val selectedPresetUuid = presetConfig.uuid
                fun CustomControlLayoutTabState.Enabled.EditState.save() {
                    presetManager.savePreset(presetUuid, undoStack.currentItem, create = false)
                }
                pageState.getAndUpdate { pageState ->
                    val editState = pageState.editState
                    if (selectedPresetUuid == null) {
                        editState?.save()
                        pageState.copy(editState = null)
                    } else if (editState != null) {
                        if (editState.presetUuid != selectedPresetUuid) {
                            editState.save()
                            val selectedPreset =
                                presetManager.presets.map { it[selectedPresetUuid] }.first { it != null }!!
                            pageState.copy(
                                editState = CustomControlLayoutTabState.Enabled.EditState(
                                    presetUuid = selectedPresetUuid,
                                    undoStack = CustomControlLayoutTabState.Enabled.UndoStack(selectedPreset),
                                )
                            )
                        } else {
                            pageState
                        }
                    } else {
                        val selectedPreset = presetManager.presets.map { it[selectedPresetUuid] }.first { it != null }!!
                        pageState.copy(
                            editState = CustomControlLayoutTabState.Enabled.EditState(
                                presetUuid = selectedPresetUuid,
                                undoStack = CustomControlLayoutTabState.Enabled.UndoStack(selectedPreset),
                            )
                        )
                    }
                }
            }
        }
        coroutineScope.launch {
            try {
                awaitCancellation()
            } finally {
                pageState.value.editState?.let { editState ->
                    presetManager.savePreset(
                        uuid = editState.presetUuid,
                        editState.undoStack.currentItem,
                    )
                }
            }
        }
    }

    fun enableCustomLayout() {
        configScreenModel.updateConfig {
            if (preset is PresetConfig.BuiltIn) {
                copy(preset = PresetConfig.Custom())
            } else {
                this
            }
        }
    }

    fun setShowSideBar(showSideBar: Boolean) {
        pageState.getAndUpdate { it.copy(showSideBar = showSideBar) }
    }

    fun setMoveLocked(moveLocked: Boolean) {
        pageState.getAndUpdate { it.copy(moveLocked = moveLocked) }
    }

    fun undo() {
        pageState.getAndUpdate { pageState ->
            if (pageState.editState != null) {
                pageState.copy(
                    editState = pageState.editState.copy(
                        undoStack = pageState.editState.undoStack.undo(),
                    )
                )
            } else {
                pageState
            }
        }
    }

    fun redo() {
        pageState.getAndUpdate { pageState ->
            if (pageState.editState != null) {
                pageState.copy(
                    editState = pageState.editState.copy(
                        undoStack = pageState.editState.undoStack.redo(),
                    )
                )
            } else {
                pageState
            }
        }
    }

    fun editPreset(editor: LayoutPreset.() -> LayoutPreset) {
        val uiState = uiState.value as? CustomControlLayoutTabState.Enabled ?: return
        val preset = uiState.selectedPreset ?: return
        val newPreset = editor(preset)
        pageState.getAndUpdate { pageState ->
            if (pageState.editState != null) {
                pageState.copy(
                    editState = pageState.editState.copy(
                        undoStack = pageState.editState.undoStack + newPreset,
                    )
                )
            } else {
                pageState
            }
        }
    }

    fun selectPreset(uuid: Uuid) {
        pageState.getAndUpdate {
            it.copy(
                selectedLayerIndex = 0,
                selectedWidgetIndex = -1
            )
        }
        configScreenModel.updateConfig {
            copy(preset = PresetConfig.Custom(uuid))
        }
    }

    fun newPreset(preset: LayoutPreset? = null) {
        pageState.getAndUpdate {
            it.copy(
                selectedLayerIndex = 0,
                selectedWidgetIndex = -1
            )
        }
        val uuid = fastRandomUuid()
        val preset = preset ?: BuiltinPresetKey.DEFAULT.preset.copy(
            name = "New preset"
        )
        presetManager.savePreset(uuid, preset)
        configScreenModel.updateConfig {
            copy(preset = PresetConfig.Custom(uuid = uuid))
        }
    }

    fun deletePreset(uuid: Uuid) {
        presetManager.removePreset(uuid)
        pageState.getAndUpdate {
            it.copy(
                selectedLayerIndex = 0,
                selectedWidgetIndex = -1
            )
        }
        configScreenModel.updateConfig {
            if (preset is PresetConfig.Custom && preset.uuid == uuid) {
                copy(preset = PresetConfig.Custom())
            } else {
                this
            }
        }
    }

    fun copyWidget(widget: ControllerWidget) {
        pageState.getAndUpdate { it.copy(copiedWidget = widget) }
    }

    fun selectWidget(index: Int) {
        pageState.getAndUpdate { it.copy(selectedWidgetIndex = index) }
    }

    fun editLayer(action: LayoutLayer.() -> LayoutLayer) {
        val uiState = uiState.value as? CustomControlLayoutTabState.Enabled ?: return
        val selectedPreset = uiState.selectedPreset ?: return
        val selectedLayerIndex =
            uiState.pageState.selectedLayerIndex.takeIf { it in selectedPreset.layout.indices } ?: return
        editPreset {
            copy(
                layout = ControllerLayout(
                    layout.set(selectedLayerIndex, action(layout[selectedLayerIndex]))
                )
            )
        }
    }

    fun deleteLayer(index: Int) {
        pageState.getAndUpdate {
            it.copy(
                selectedWidgetIndex = -1,
                selectedLayerIndex = -1,
            )
        }
        editPreset {
            copy(layout = ControllerLayout(layout.removeAt(index)))
        }
    }

    fun selectLayer(index: Int) {
        pageState.getAndUpdate {
            it.copy(
                selectedWidgetIndex = -1,
                selectedLayerIndex = index,
            )
        }
    }

    fun editWidget(index: Int, widget: ControllerWidget) {
        editLayer { copy(widgets = widgets.set(index, widget)) }
    }

    fun newWidget(widget: ControllerWidget): Int {
        val newWidget = widget.newId()
        var index = 0
        editLayer { copy(widgets = widgets.add(newWidget)).also { index = it.widgets.size - 1 } }
        return index
    }

    fun deleteWidget(index: Int) {
        pageState.getAndUpdate {
            it.copy(
                selectedWidgetIndex = -1
            )
        }
        editLayer { copy(widgets = widgets.removeAt(index)) }
    }
}