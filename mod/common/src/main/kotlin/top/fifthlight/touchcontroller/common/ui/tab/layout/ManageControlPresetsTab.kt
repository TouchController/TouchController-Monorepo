package top.fifthlight.touchcontroller.common.ui.tab.layout

import androidx.compose.runtime.*
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.pointer.toggleable
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.base.layout.*
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.ControllerLayout
import top.fifthlight.touchcontroller.common.config.LayerConditionValue
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltinPresetKey
import top.fifthlight.touchcontroller.common.ui.component.AppBar
import top.fifthlight.touchcontroller.common.ui.component.BackButton
import top.fifthlight.touchcontroller.common.ui.component.ControllerWidget
import top.fifthlight.touchcontroller.common.ui.component.Scaffold
import top.fifthlight.touchcontroller.common.ui.model.LocalConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.model.ManageControlPresetsTabModel
import top.fifthlight.touchcontroller.common.ui.tab.Tab
import top.fifthlight.touchcontroller.common.ui.tab.TabGroup
import top.fifthlight.touchcontroller.common.ui.tab.TabOptions

@Composable
private fun RadioContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(4)
            .border(Textures.WIDGET_BACKGROUND_FLOAT_WINDOW)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(4),
    ) {
        content()
    }
}

@Composable
private fun RadioBoxItem(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.toggleable(
            interactionSource = interactionSource,
            value = value,
            onValueChanged = onValueChanged
        ),
        horizontalArrangement = Arrangement.spacedBy(4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioIcon(
            interactionSource = interactionSource,
            value = value,
        )
        CompositionLocalProvider(
            LocalColorTheme provides ColorTheme.light,
        ) {
            content()
        }
    }
}

@Composable
private fun PresetPreview(
    modifier: Modifier = Modifier,
    preset: ControllerLayout = ControllerLayout(),
) {
    Box(modifier = modifier) {
        for (layer in preset.layers) {
            if (layer.condition.values.any { it != LayerConditionValue.NEVER }) {
                continue
            }
            for (widget in layer.widgets) {
                ControllerWidget(
                    modifier = Modifier
                        .alignment(widget.align.alignment)
                        .offset(widget.align.normalizeOffset(widget.offset)),
                    widget = widget,
                )
            }
        }
    }
}

object ManageControlPresetsTab : Tab() {
    override val options = TabOptions(
        titleId = Texts.SCREEN_CONFIG_LAYOUT_MANAGE_CONTROL_PRESET,
        group = TabGroup.LayoutGroup,
        index = 0,
        openAsScreen = true,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_TITLE),
                        )
                    },
                )
            },
        ) { modifier ->
            val configScreenModel = LocalConfigScreenModel.current
            val screenModel = koinScreenModel<ManageControlPresetsTabModel> { parametersOf(configScreenModel) }
            val presetConfig by screenModel.presetConfig.collectAsState()
            val currentPresetConfig = presetConfig
            if (currentPresetConfig != null) {
                val presetKey = currentPresetConfig.key
                Row(
                    modifier = Modifier
                        .background(BackgroundTextures.BRICK_BACKGROUND)
                        .then(modifier)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(6f)
                            .fillMaxHeight(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            alignment = Alignment.Center,
                        ) {
                            Text("Real-time preview")
                            PresetPreview(
                                modifier = Modifier.fillMaxSize(),
                                preset = currentPresetConfig.key.preset.layout,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(4)
                                .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4),
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_TEXTURE_STYLE))
                                RadioContainer {
                                    for (textureSet in TextureSet.TextureSetKey.entries) {
                                        RadioBoxItem(
                                            value = presetKey.textureSet == textureSet,
                                            onValueChanged = {
                                                screenModel.updateKey { copy(textureSet = textureSet) }
                                            },
                                        ) {
                                            Text(Text.translatable(textureSet.titleText))
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4),
                            ) {
                                Text(
                                    Text.format(
                                        Texts.SCREEN_CONFIG_PERCENT,
                                        Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_OPACITY),
                                        (presetKey.opacity * 100).toInt().toString()
                                    )
                                )
                                Slider(
                                    modifier = Modifier.fillMaxWidth(),
                                    range = 0f..1f,
                                    value = presetKey.opacity,
                                    onValueChanged = {
                                        screenModel.updateKey { copy(opacity = it) }
                                    },
                                )

                                Text(
                                    Text.format(
                                        Texts.SCREEN_CONFIG_PERCENT,
                                        Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SCALE),
                                        (presetKey.scale * 100).toInt().toString()
                                    )
                                )
                                Slider(
                                    modifier = Modifier.fillMaxWidth(),
                                    range = .5f..4f,
                                    value = presetKey.scale,
                                    onValueChanged = {
                                        screenModel.updateKey { copy(scale = it) }
                                    },
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(4)
                            .weight(4f)
                            .verticalScroll()
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4),
                    ) {
                        Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_CONTROL_STYLE))
                        RadioContainer(modifier = Modifier.fillMaxWidth()) {
                            RadioBoxItem(
                                value = presetKey.controlStyle == BuiltinPresetKey.ControlStyle.TouchGesture,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (presetKey.controlStyle != BuiltinPresetKey.ControlStyle.TouchGesture) {
                                            copy(controlStyle = BuiltinPresetKey.ControlStyle.TouchGesture)
                                        } else {
                                            this
                                        }
                                    }
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_CONTROL_STYLE_CLICK_TO_INTERACT))
                            }

                            RadioBoxItem(
                                value = presetKey.controlStyle is BuiltinPresetKey.ControlStyle.SplitControls,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (presetKey.controlStyle !is BuiltinPresetKey.ControlStyle.SplitControls) {
                                            copy(controlStyle = BuiltinPresetKey.ControlStyle.SplitControls())
                                        } else {
                                            this
                                        }
                                    }
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_CONTROL_STYLE_AIMING_BY_CROSSHAIR))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_ATTACK_AND_INTERACT_BY_BUTTON),
                            )
                            Switch(
                                enabled = presetKey.controlStyle is BuiltinPresetKey.ControlStyle.SplitControls,
                                value = (presetKey.controlStyle as? BuiltinPresetKey.ControlStyle.SplitControls)?.buttonInteraction == true,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (controlStyle is BuiltinPresetKey.ControlStyle.SplitControls) {
                                            copy(controlStyle = controlStyle.copy(buttonInteraction = it))
                                        } else {
                                            this
                                        }
                                    }
                                },
                            )
                        }

                        Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_MOVE_METHOD))
                        RadioContainer(modifier = Modifier.fillMaxWidth()) {
                            RadioBoxItem(
                                value = presetKey.moveMethod is BuiltinPresetKey.MoveMethod.Dpad,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (moveMethod !is BuiltinPresetKey.MoveMethod.Dpad) {
                                            copy(moveMethod = BuiltinPresetKey.MoveMethod.Dpad())
                                        } else {
                                            this
                                        }
                                    }
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_MOVE_METHOD_DPAD))
                            }

                            RadioBoxItem(
                                value = presetKey.moveMethod is BuiltinPresetKey.MoveMethod.Joystick,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (moveMethod !is BuiltinPresetKey.MoveMethod.Joystick) {
                                            copy(moveMethod = BuiltinPresetKey.MoveMethod.Joystick())
                                        } else {
                                            this
                                        }
                                    }
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_MOVE_METHOD_JOYSTICK))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SPRINT_USING_JOYSTICK),
                            )
                            Switch(
                                enabled = presetKey.moveMethod is BuiltinPresetKey.MoveMethod.Joystick,
                                value = (presetKey.moveMethod as? BuiltinPresetKey.MoveMethod.Joystick)?.triggerSprint == true,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (moveMethod is BuiltinPresetKey.MoveMethod.Joystick) {
                                            copy(moveMethod = moveMethod.copy(triggerSprint = it))
                                        } else {
                                            this
                                        }
                                    }
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SWAP_JUMP_AND_SNEAK),
                            )
                            Switch(
                                enabled = presetKey.moveMethod is BuiltinPresetKey.MoveMethod.Dpad,
                                value = (presetKey.moveMethod as? BuiltinPresetKey.MoveMethod.Dpad)?.swapJumpAndSneak == true,
                                onValueChanged = {
                                    screenModel.updateKey {
                                        if (moveMethod is BuiltinPresetKey.MoveMethod.Dpad) {
                                            copy(moveMethod = moveMethod.copy(swapJumpAndSneak = it))
                                        } else {
                                            this
                                        }
                                    }
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SPRINT),
                            )
                            var expanded by remember { mutableStateOf(false) }
                            Select(
                                expanded = expanded,
                                onExpandedChanged = { expanded = it },
                                dropDownContent = {
                                    val selectedIndex =
                                        BuiltinPresetKey.SprintButtonLocation.entries.indexOf(presetKey.sprintButtonLocation)
                                    val textFactory = LocalTextFactory.current
                                    DropdownItemList(
                                        modifier = Modifier.verticalScroll(),
                                        items = BuiltinPresetKey.SprintButtonLocation.entries,
                                        textProvider = { textFactory.of(it.nameId) },
                                        selectedIndex = selectedIndex,
                                        onItemSelected = { index ->
                                            expanded = false
                                            screenModel.updateKey {
                                                copy(sprintButtonLocation = BuiltinPresetKey.SprintButtonLocation.entries[index])
                                            }
                                        }
                                    )
                                }
                            ) {
                                Text(Text.translatable(presetKey.sprintButtonLocation.nameId))
                                SelectIcon(expanded = expanded)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(BackgroundTextures.BRICK_BACKGROUND)
                        .then(modifier),
                    alignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12)
                            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12),
                    ) {
                        Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SWITCH_MESSAGE))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12),
                        ) {
                            WarningButton(
                                onClick = {
                                    screenModel.update(PresetConfig.BuiltIn())
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SWITCH_SWITCH))
                            }
                            GuideButton(
                                onClick = {
                                    navigator?.replace(CustomControlLayoutTab)
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SWITCH_GOTO_CUSTOM))
                            }
                        }
                    }
                }
            }
        }
    }
}