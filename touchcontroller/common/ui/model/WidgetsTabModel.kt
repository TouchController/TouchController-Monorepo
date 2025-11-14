package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.config.widget.WidgetPresetManager
import top.fifthlight.touchcontroller.common.control.BuiltInWidgets
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.ext.combineStates
import top.fifthlight.touchcontroller.common.ui.state.WidgetsTabState
import top.fifthlight.touchcontroller.common.ui.state.WidgetsTabState.ListContent

class WidgetsTabModel(
    private val screenModel: CustomControlLayoutTabModel
) : TouchControllerScreenModel() {
    private val widgetPresetManager: WidgetPresetManager by inject()
    private val tabState = MutableStateFlow(WidgetsTabState.TabState())
    val uiState = combineStates(tabState, widgetPresetManager.presets) { tabState, presets ->
        WidgetsTabState(
            listContent = when (tabState.listState) {
                WidgetsTabState.ListState.BUILTIN -> ListContent.BuiltIn(builtIn = BuiltInWidgets[tabState.newWidgetParams.textureSet])
                WidgetsTabState.ListState.CUSTOM -> ListContent.Custom(widgets = presets)
            },
            tabState = tabState,
        )
    }

    fun selectBuiltinTab() {
        tabState.getAndUpdate {
            it.copy(listState = WidgetsTabState.ListState.BUILTIN)
        }
    }

    fun selectCustomTab() {
        tabState.getAndUpdate {
            it.copy(listState = WidgetsTabState.ListState.CUSTOM)
        }
    }

    fun openNewWidgetParamsDialog() {
        tabState.getAndUpdate {
            it.copy(dialogState = WidgetsTabState.DialogState.ChangeNewWidgetParams(it.newWidgetParams))
        }
    }

    fun updateNewWidgetParamsDialog(editor: WidgetsTabState.DialogState.ChangeNewWidgetParams.() -> WidgetsTabState.DialogState.ChangeNewWidgetParams) {
        tabState.getAndUpdate {
            var params = it.dialogState
            if (params is WidgetsTabState.DialogState.ChangeNewWidgetParams) {
                params = editor(params)
            }
            it.copy(dialogState = params)
        }
    }

    fun openRenameWidgetPresetItemDialog(index: Int, widget: ControllerWidget) {
        tabState.getAndUpdate {
            it.copy(
                dialogState = WidgetsTabState.DialogState.RenameWidgetPresetItem(
                    index = index,
                    widget = widget,
                )
            )
        }
    }

    fun updateRenameWidgetPresetItemDialog(newName: String) {
        tabState.getAndUpdate {
            var params = it.dialogState
            if (params is WidgetsTabState.DialogState.RenameWidgetPresetItem) {
                params = params.copy(name = ControllerWidget.Name.Literal(newName))
            }
            it.copy(dialogState = params)
        }
    }

    fun closeDialog() {
        tabState.getAndUpdate {
            it.copy(dialogState = WidgetsTabState.DialogState.Empty)
        }
    }

    fun updateNewWidgetParams(params: WidgetsTabState.NewWidgetParams) {
        tabState.getAndUpdate {
            it.copy(newWidgetParams = params)
        }
    }

    fun renameWidgetPresetItem(index: Int, newName: ControllerWidget.Name) {
        val presets = widgetPresetManager.presets.value
        val widget = presets[index].cloneBase(name = newName)
        widgetPresetManager.save(presets.set(index, widget))
    }

    fun deleteWidgetPresetItem(index: Int) {
        val presets = widgetPresetManager.presets.value
        widgetPresetManager.save(presets.removeAt(index))
    }
}