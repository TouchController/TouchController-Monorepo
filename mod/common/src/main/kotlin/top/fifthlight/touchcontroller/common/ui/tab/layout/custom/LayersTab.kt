package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.*
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.PersistentMap
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.*
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.LayerConditionKey
import top.fifthlight.touchcontroller.common.config.LayerConditionValue
import top.fifthlight.touchcontroller.common.config.text
import top.fifthlight.touchcontroller.common.ui.component.ListButton
import top.fifthlight.touchcontroller.common.ui.model.LayersTabModel
import top.fifthlight.touchcontroller.common.ui.state.LayersTabState

@Composable
private fun LayerConditionPanel(
    modifier: Modifier = Modifier,
    value: PersistentMap<LayerConditionKey, LayerConditionValue>,
    onValueChanged: (PersistentMap<LayerConditionKey, LayerConditionValue>) -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        maxColumns = 2,
        expandColumnWidth = true,
    ) {
        for (key in LayerConditionKey.entries) {
            var expanded by remember { mutableStateOf(false) }
            Select(
                expanded = expanded,
                onExpandedChanged = { expanded = it },
                dropDownContent = {
                    val selectedIndex = LayerConditionValue.allValues.indexOf(value[key])
                    val textFactory = LocalTextFactory.current
                    DropdownItemList(
                        modifier = Modifier.verticalScroll(),
                        items = LayerConditionValue.allValues,
                        textProvider = { textFactory.of(it.text()) },
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            expanded = false
                            onValueChanged(
                                when (val conditionValue = LayerConditionValue.allValues[index]) {
                                    null -> value.remove(key)
                                    else -> value.put(key, conditionValue)
                                }
                            )
                        }
                    )
                }
            ) {
                Text(Text.translatable(key.text))
                Spacer(modifier = Modifier.weight(1f))
                SelectIcon(expanded = expanded)
            }
        }
    }
}

object LayersTab: CustomTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_LAYER)
    }

    @Composable
    override fun Content() {
        val (screenModel, uiState, tabsButton, sideBarAtRight) = LocalCustomTabContext.current
        val tabModel: LayersTabModel = koinScreenModel { parametersOf(screenModel) }
        val tabState by tabModel.uiState.collectAsState()
        when (val state = tabState) {
            is LayersTabState.Create -> AlertDialog(
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CREATE_LAYER))
                },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.createLayer(state)
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CREATE_LAYER_CREATE))
                    }
                    Button(
                        onClick = {
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CREATE_LAYER_CANCEL))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(.5f)
                        .fillMaxHeight(.8f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    EditText(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name,
                        onValueChanged = {
                            tabModel.updateCreateLayerState { copy(name = it) }
                        },
                        placeholder = Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_NAME_PLACEHOLDER),
                    )

                    LayerConditionPanel(
                        modifier = Modifier
                            .verticalScroll()
                            .weight(1f)
                            .fillMaxWidth(),
                        value = state.condition,
                        onValueChanged = {
                            tabModel.updateCreateLayerState { copy(condition = it) }
                        }
                    )
                }
            }

            is LayersTabState.Edit -> AlertDialog(
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_EDIT_LAYER))
                },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.clearState()
                            screenModel.editLayer(state::edit)
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_EDIT_LAYER_EDIT))
                    }
                    Button(
                        onClick = {
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_EDIT_LAYER_CANCEL))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(.5f)
                        .fillMaxHeight(.8f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    EditText(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name,
                        onValueChanged = {
                            tabModel.updateEditLayerState { copy(name = it) }
                        },
                        placeholder = Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_NAME_PLACEHOLDER),
                    )

                    LayerConditionPanel(
                        modifier = Modifier
                            .verticalScroll()
                            .weight(1f)
                            .fillMaxWidth(),
                        value = state.condition,
                        onValueChanged = {
                            tabModel.updateEditLayerState { copy(condition = it) }
                        }
                    )
                }
            }

            is LayersTabState.Delete -> AlertDialog(
                onDismissRequest = { tabModel.clearState() },
                action = {
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
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_1))
                    Text(Text.format(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_DELETE_LAYER_2, state.layer.name))
                }
            }

            LayersTabState.Empty -> Unit
        }
        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                if (uiState.selectedPreset != null) {
                    val currentLayer = uiState.selectedLayer
                    IconButton(
                        onClick = {
                            tabModel.openCreateLayerDialog()
                        },
                    ) {
                        Icon(Textures.ICON_ADD)
                    }
                    IconButton(
                        onClick = {
                            currentLayer?.let(tabModel::copyLayer)
                        },
                        enabled = currentLayer != null,
                    ) {
                        Icon(Textures.ICON_COPY)
                    }
                    IconButton(
                        onClick = {
                            val index = uiState.pageState.selectedLayerIndex
                            val layer = currentLayer ?: return@IconButton
                            tabModel.openEditLayerDialog(index, layer)
                        },
                        enabled = currentLayer != null,
                    ) {
                        Icon(Textures.ICON_CONFIG)
                    }
                    IconButton(
                        onClick = {
                            val index = uiState.pageState.selectedLayerIndex
                            val layer = currentLayer ?: return@IconButton
                            tabModel.openDeleteLayerDialog(index, layer)
                        },
                        enabled = currentLayer != null,
                    ) {
                        Icon(Textures.ICON_DELETE)
                    }
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
                            modifier = Modifier.weight(1f),
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
                            modifier = Modifier.weight(1f),
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
                        for ((index, layer) in uiState.selectedPreset.layout.withIndex()) {
                            ListButton(
                                checked = index == uiState.pageState.selectedLayerIndex,
                                onClick = {
                                    screenModel.selectLayer(index)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = layer.name,
                                    )
                                    Text(
                                        Text.format(
                                            Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_COUNT,
                                            layer.condition.conditions.size
                                        )
                                    )
                                }
                            }
                        }
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