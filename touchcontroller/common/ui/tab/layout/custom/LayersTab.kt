package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.*
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.data.IntRect
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.ControllerLayout
import top.fifthlight.touchcontroller.common.config.LayoutLayer
import top.fifthlight.touchcontroller.common.ext.mapState
import top.fifthlight.touchcontroller.common.ui.component.ListButton
import top.fifthlight.touchcontroller.common.ui.component.LocalListButtonDrawable
import top.fifthlight.touchcontroller.common.ui.model.LayersTabModel
import top.fifthlight.touchcontroller.common.ui.screen.LayerEditorScreen
import top.fifthlight.touchcontroller.common.ui.state.CustomControlLayoutTabState
import top.fifthlight.touchcontroller.common.ui.state.LayersTabState

@Composable
private fun LayersList(
    modifier: Modifier = Modifier,
    listContent: PersistentList<LayoutLayer> = persistentListOf(),
    currentSelectedLayoutIndex: Int? = null,
    onLayerSelected: (Int, LayoutLayer) -> Unit = { _, _ -> },
    onLayerEdited: (Int, LayoutLayer) -> Unit = { _, _ -> },
    onLayerCopied: (Int, LayoutLayer) -> Unit = { _, _ -> },
    onLayerDeleted: (Int, LayoutLayer) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier) {
        for ((index, preset) in listContent.withIndex()) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                ListButton(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    checked = currentSelectedLayoutIndex == index,
                    onClick = {
                        onLayerSelected(index, preset)
                    },
                ) {
                    Text(
                        modifier = Modifier.alignment(Alignment.CenterLeft),
                        text = preset.name,
                    )
                }

                var expanded by remember { mutableStateOf(false) }
                var anchor by remember { mutableStateOf(IntRect.ZERO) }
                IconButton(
                    modifier = Modifier
                        .width(24)
                        .minHeight(24)
                        .fillMaxHeight()
                        .anchor { anchor = it },
                    drawableSet = LocalListButtonDrawable.current.unchecked,
                    onClick = {
                        expanded = true
                    },
                ) {
                    Icon(Textures.ICON_MENU)
                }

                DropDownMenu(
                    expanded = expanded,
                    anchor = anchor,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    DropdownItemList(
                        modifier = Modifier.verticalScroll(),
                        onItemSelected = { expanded = false },
                        items = persistentListOf(
                            Pair(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_EDIT)) {
                                onLayerEdited(index, preset)
                            },
                            Pair(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_COPY)) {
                                onLayerCopied(index, preset)
                            },
                            Pair(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE)) {
                                onLayerDeleted(index, preset)
                            },
                        ),
                    )
                }
            }
        }
    }
}

object LayersTab : CustomTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_LAYER)
    }

    @Composable
    override fun Content() {
        val (screenModel, uiState, tabsButton, sideBarAtRight, parentNavigator) = LocalCustomTabContext.current
        val tabModel: LayersTabModel = koinScreenModel { parametersOf(screenModel) }
        val tabState by tabModel.uiState.collectAsState()

        AlertDialog(
            value = tabState,
            valueTransformer = { tabState as? LayersTabState.Delete },
            onDismissRequest = { tabModel.clearState() },
            action = { state ->
                WarningButton(
                    onClick = {
                        screenModel.deleteLayer(state.index)
                        tabModel.clearState()
                    },
                ) {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_DELETE))
                }
                Button(
                    onClick = {
                        tabModel.clearState()
                    },
                ) {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_CANCEL))
                }
            }
        ) { state ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_1))
                Text(Text.format(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_2, state.layer.name))
            }
        }
        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                IconButton(
                    onClick = {
                        uiState.selectedPreset?.let { preset ->
                            parentNavigator?.push(
                                LayerEditorScreen(
                                    screenName = Texts.SCREEN_LAYER_EDITOR_CREATE,
                                    preset = screenModel.uiState.mapState { (it as? CustomControlLayoutTabState.Enabled)?.selectedPreset },
                                    onCustomConditionsChanged = {
                                        screenModel.editPreset {
                                            copy(
                                                controlInfo = preset.controlInfo.copy(
                                                    customConditions = it
                                                )
                                            )
                                        }
                                    },
                                    initialValue = LayoutLayer(),
                                    onValueChanged = tabModel::createLayer,
                                )
                            )
                        }
                    },
                    enabled = uiState.selectedPreset != null,
                ) {
                    Icon(Textures.ICON_ADD)
                }
            }
        ) { modifier ->
            SideBarScaffold(
                modifier = modifier,
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS))
                },
                actions = if (uiState.selectedPreset != null) {
                    {
                        val selectedPreset = uiState.selectedPreset
                        val layerIndices = selectedPreset.layout.indices
                        val selectedLayerIndex = uiState.pageState.selectedLayerIndex.takeIf { it in layerIndices }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            enabled = selectedLayerIndex != null && selectedLayerIndex > 0,
                            onClick = {
                                selectedLayerIndex?.let { index ->
                                    tabModel.moveLayer(index, -1)
                                    screenModel.selectLayer(index - 1)
                                }
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_MOVE_UP))
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            enabled = selectedLayerIndex != null && selectedLayerIndex < layerIndices.last,
                            onClick = {
                                selectedLayerIndex?.let { index ->
                                    tabModel.moveLayer(index, 1)
                                    screenModel.selectLayer(index + 1)
                                }
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_MOVE_DOWN))
                        }
                    }
                } else {
                    null
                }
            ) {
                if (uiState.selectedPreset != null) {
                    Column(
                        modifier = Modifier
                            .verticalScroll()
                            .fillMaxSize()
                    ) {
                        LayersList(
                            modifier = Modifier
                                .padding(4)
                                .fillMaxWidth(),
                            listContent = uiState.selectedPreset.layout,
                            currentSelectedLayoutIndex = uiState.pageState.selectedLayerIndex,
                            onLayerSelected = { index, layer ->
                                screenModel.selectLayer(index)
                            },
                            onLayerEdited = { index, layer ->
                                parentNavigator?.push(
                                    LayerEditorScreen(
                                        screenName = Texts.SCREEN_LAYER_EDITOR_EDIT,
                                        preset = screenModel.uiState.mapState { (it as? CustomControlLayoutTabState.Enabled)?.selectedPreset },
                                        onCustomConditionsChanged = {
                                            screenModel.editPreset {
                                                copy(
                                                    controlInfo = uiState.selectedPreset.controlInfo.copy(
                                                        customConditions = it
                                                    )
                                                )
                                            }
                                        },
                                        initialValue = layer,
                                        onValueChanged = { newLayer ->
                                            screenModel.editPreset {
                                                copy(
                                                    layout = ControllerLayout(
                                                        layout.set(index, newLayer)
                                                    )
                                                )
                                            }
                                        },
                                    )
                                )
                            },
                            onLayerCopied = { index, layer ->
                                tabModel.copyLayer(layer)
                            },
                            onLayerDeleted = { index, layer ->
                                tabModel.openDeleteLayerDialog(index, layer)
                            },
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.Center,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_NO_PRESET_SELECTED))
                    }
                }
            }
        }
    }
}