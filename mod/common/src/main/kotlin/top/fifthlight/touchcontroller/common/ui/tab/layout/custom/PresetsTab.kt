package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.ui.component.TabButton
import top.fifthlight.touchcontroller.common.ui.model.PresetsTabModel
import top.fifthlight.touchcontroller.common.ui.state.PresetsTabState

object PresetsTab: CustomTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_PRESET)
    }

    @Composable
    override fun Content() {
        val (screenModel, uiState, tabsButton, sideBarAtRight) = LocalCustomTabContext.current
        val tabModel: PresetsTabModel = koinScreenModel { parametersOf(screenModel) }
        val tabState by tabModel.uiState.collectAsState()
        when (val state = tabState) {
            is PresetsTabState.Create -> AlertDialog(
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_CREATE_PRESET))
                },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.createPreset(state)
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_CREATE_PRESET_CREATE))
                    }
                    Button(
                        onClick = {
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_CREATE_PRESET_CANCEL))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(.5f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    EditText(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name,
                        onValueChanged = {
                            tabModel.updateCreatePresetState { copy(name = it) }
                        },
                        placeholder = Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_NAME_PLACEHOLDER),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_SPLIT_CONTROLS))

                        Switch(
                            value = state.controlInfo.splitControls,
                            onValueChanged = {
                                tabModel.updateCreatePresetState {
                                    copy(controlInfo = controlInfo.copy(splitControls = it))
                                }
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DISABLE_TOUCH_GESTURES))

                        Switch(
                            value = state.controlInfo.disableTouchGesture,
                            onValueChanged = {
                                tabModel.updateCreatePresetState {
                                    copy(controlInfo = controlInfo.copy(disableTouchGesture = it))
                                }
                            }
                        )
                    }
                }
            }

            is PresetsTabState.Edit -> AlertDialog(
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_EDIT_PRESET))
                },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.editPreset(state)
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_EDIT_PRESET_OK))
                    }
                    Button(
                        onClick = {
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_EDIT_PRESET_CANCEL))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(.5f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    EditText(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name,
                        onValueChanged = {
                            tabModel.updateEditPresetState { copy(name = it) }
                        },
                        placeholder = Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_NAME_PLACEHOLDER),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_SPLIT_CONTROLS))

                        Switch(
                            value = state.controlInfo.splitControls,
                            onValueChanged = {
                                tabModel.updateEditPresetState {
                                    copy(controlInfo = controlInfo.copy(splitControls = it))
                                }
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DISABLE_TOUCH_GESTURES))

                        Switch(
                            value = state.controlInfo.disableTouchGesture,
                            onValueChanged = {
                                tabModel.updateEditPresetState {
                                    copy(controlInfo = controlInfo.copy(disableTouchGesture = it))
                                }
                            }
                        )
                    }
                }
            }

            is PresetsTabState.Delete -> AlertDialog(
                onDismissRequest = { tabModel.clearState() },
                action = {
                    WarningButton(
                        onClick = {
                            screenModel.deletePreset(state.uuid)
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DELETE_PRESET_DELETE))
                    }
                    Button(
                        onClick = {
                            tabModel.clearState()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DELETE_PRESET_CANCEL))
                    }
                }
            ) {
                val presetName = uiState.allPresets[state.uuid]?.name ?: "ERROR"
                Column(
                    verticalArrangement = Arrangement.spacedBy(4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DELETE_PRESET_1))
                    Text(Text.format(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_DELETE_PRESET_2, presetName))
                }
            }

            PresetsTabState.Empty -> Unit
        }

        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                val currentPreset = uiState.selectedPreset
                IconButton(
                    onClick = {
                        tabModel.openCreatePresetDialog()
                    }
                ) {
                    Icon(Textures.ICON_ADD)
                }
                IconButton(
                    onClick = {
                        currentPreset?.let(screenModel::newPreset)
                    },
                    enabled = currentPreset != null,
                ) {
                    Icon(Textures.ICON_COPY)
                }
                IconButton(
                    onClick = {
                        val uuid = uiState.selectedPresetUuid ?: return@IconButton
                        val currentPreset = currentPreset ?: return@IconButton
                        tabModel.openEditPresetDialog(uuid, currentPreset)
                    },
                    enabled = currentPreset != null,
                ) {
                    Icon(Textures.ICON_CONFIG)
                }
                IconButton(
                    onClick = {
                        uiState.selectedPresetUuid?.let(tabModel::openDeletePresetBox)
                    },
                    enabled = currentPreset != null,
                ) {
                    Icon(Textures.ICON_DELETE)
                }
            }
        ) { modifier ->
            SideBarScaffold(
                modifier = modifier,
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS))
                },
                actions = {
                    val selectedUuid = uiState.selectedPresetUuid
                    val indices = uiState.allPresets.orderedEntries.indices
                    val index = selectedUuid?.let { selectedUuid ->
                        uiState.allPresets.orderedEntries.indexOfFirst { (uuid, _) -> uuid == selectedUuid }
                    } ?: run {
                        -1
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = index > 0,
                        onClick = {
                            selectedUuid?.let { uuid ->
                                tabModel.movePreset(uuid, -1)
                            }
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_MOVE_UP))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = index < indices.last,
                        onClick = {
                            selectedUuid?.let { uuid ->
                                tabModel.movePreset(uuid, 1)
                            }
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PRESETS_MOVE_DOWN))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll()
                        .fillMaxSize()
                ) {
                    for ((uuid, preset) in uiState.allPresets.orderedEntries) {
                        TabButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = uiState.selectedPresetUuid == uuid,
                            onClick = {
                                screenModel.selectPreset(uuid)
                            }
                        ) {
                            Text(preset.name)
                        }
                    }
                }
            }
        }
    }
}