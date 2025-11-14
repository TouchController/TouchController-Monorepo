package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.*
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.layout.Layout
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.ParentDataModifierNode
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.node.LocalScreenSize
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.ControllerLayout
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltInPresetKey
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.layout.ContextInput
import kotlin.math.min

private data class ControllerWidgetModifierNode(
    val widget: ControllerWidget,
) : ParentDataModifierNode, Modifier.Node<ControllerWidgetModifierNode> {
    override fun modifierParentData(parentData: Any?): ControllerWidget = widget
}

@Composable
private fun PresetPreview(
    modifier: Modifier = Modifier,
    preset: ControllerLayout = ControllerLayout(),
    minimumLogicalSize: IntSize = IntSize(480, 270),
) {
    var scale by remember { mutableStateOf<Float?>(null) }
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val size = IntSize(constraints.maxWidth, constraints.maxHeight)
            val displayScale = min(
                size.width.toFloat() / minimumLogicalSize.width.toFloat(),
                size.height.toFloat() / minimumLogicalSize.height.toFloat(),
            ).coerceAtMost(1f)
            scale = displayScale
            val logicalSize = (size.toSize() / displayScale).toIntSize()
            val childConstraint = constraints.copy(
                minWidth = 0,
                minHeight = 0,
            )
            val placeables = measurables.map {
                it.measure(childConstraint)
            }
            layout(size) {
                for ((index, placeable) in placeables.withIndex()) {
                    val measurable = measurables[index]
                    val widget = (measurable.parentData as? ControllerWidget)
                        ?: error("Bad parent data: ${measurable.parentData}")
                    val offset =
                        widget.align.alignOffset(logicalSize, widget.size(), widget.offset).toOffset() * displayScale
                    placeable.placeAt(offset.toIntOffset())
                }
            }
        }
    ) {
        val currentScale = scale
        if (currentScale == null) {
            return@Layout
        }
        for (layer in preset.layers) {
            if (!layer.conditions.check(ContextInput.EMPTY)) {
                continue
            }
            for (widget in layer.widgets) {
                ControllerWidget(
                    modifier = Modifier.then(ControllerWidgetModifierNode(widget)),
                    widget = widget,
                    scale = currentScale,
                )
            }
        }
    }
}

@Composable
fun BuiltInPresetKeySelector(
    modifier: Modifier = Modifier,
    value: BuiltInPresetKey,
    onValueChanged: (BuiltInPresetKey) -> Unit,
) {
    @Composable
    fun StyleBox(
        modifier: Modifier = Modifier,
        itemModifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_TEXTURE_STYLE))
            RadioColumn {
                for (textureSet in TextureSet.TextureSetKey.entries) {
                    RadioBoxItem(
                        modifier = itemModifier,
                        value = value.textureSet == textureSet,
                        onValueChanged = {
                            onValueChanged(value.copy(textureSet = textureSet))
                        },
                    ) {
                        Text(Text.translatable(textureSet.titleText))
                    }
                }
            }
        }
    }

    @Composable
    fun OptionBox(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(
                Text.format(
                    Texts.SCREEN_CONFIG_PERCENT,
                    Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_OPACITY),
                    (value.opacity * 100).toInt().toString()
                )
            )
            Slider(
                modifier = Modifier.fillMaxWidth(),
                range = 0f..1f,
                value = value.opacity,
                onValueChanged = {
                    onValueChanged(value.copy(opacity = it))
                },
            )

            Text(
                Text.format(
                    Texts.SCREEN_CONFIG_PERCENT,
                    Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_SCALE),
                    (value.scale * 100).toInt().toString()
                )
            )
            Slider(
                modifier = Modifier.fillMaxWidth(),
                range = .5f..4f,
                value = value.scale,
                onValueChanged = {
                    onValueChanged(value.copy(scale = it))
                },
            )
        }
    }

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
            PresetPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                preset = value.preset.layout,
            )
            if (LocalScreenSize.current.width > 600) {
                Row(
                    modifier = Modifier
                        .padding(4)
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4),
                ) {
                    StyleBox()
                    OptionBox(Modifier.weight(1f))
                }
            } else {
                OptionBox(
                    modifier = Modifier
                        .padding(4)
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxWidth(),
                )
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
            if (LocalScreenSize.current.width < 600) {
                StyleBox(
                    modifier = Modifier.fillMaxWidth(),
                    itemModifier = Modifier.fillMaxWidth(),
                )
            }

            Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_CONTROL_STYLE))
            RadioColumn(modifier = Modifier.fillMaxWidth()) {
                RadioBoxItem(
                    value = value.controlStyle == BuiltInPresetKey.ControlStyle.TouchGesture,
                    onValueChanged = {
                        if (value.controlStyle != BuiltInPresetKey.ControlStyle.TouchGesture) {
                            onValueChanged(value.copy(controlStyle = BuiltInPresetKey.ControlStyle.TouchGesture))
                        }
                    }
                ) {
                    Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_CONTROL_STYLE_CLICK_TO_INTERACT))
                }

                RadioBoxItem(
                    value = value.controlStyle is BuiltInPresetKey.ControlStyle.SplitControls,
                    onValueChanged = {
                        if (value.controlStyle !is BuiltInPresetKey.ControlStyle.SplitControls) {
                            onValueChanged(value.copy(controlStyle = BuiltInPresetKey.ControlStyle.SplitControls()))
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
                    enabled = value.controlStyle is BuiltInPresetKey.ControlStyle.SplitControls,
                    value = (value.controlStyle as? BuiltInPresetKey.ControlStyle.SplitControls)?.buttonInteraction == true,
                    onValueChanged = {
                        if (value.controlStyle is BuiltInPresetKey.ControlStyle.SplitControls) {
                            onValueChanged(value.copy(controlStyle = value.controlStyle.copy(buttonInteraction = it)))
                        }
                    },
                )
            }

            Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_MOVE_METHOD))
            RadioColumn(modifier = Modifier.fillMaxWidth()) {
                RadioBoxItem(
                    value = value.moveMethod is BuiltInPresetKey.MoveMethod.Dpad,
                    onValueChanged = {
                        if (value.moveMethod !is BuiltInPresetKey.MoveMethod.Dpad) {
                            onValueChanged(value.copy(moveMethod = BuiltInPresetKey.MoveMethod.Dpad()))
                        }
                    }
                ) {
                    Text(Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_MOVE_METHOD_DPAD))
                }

                RadioBoxItem(
                    value = value.moveMethod is BuiltInPresetKey.MoveMethod.Joystick,
                    onValueChanged = {
                        if (value.moveMethod !is BuiltInPresetKey.MoveMethod.Joystick) {
                            onValueChanged(value.copy(moveMethod = BuiltInPresetKey.MoveMethod.Joystick()))
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
                    enabled = value.moveMethod is BuiltInPresetKey.MoveMethod.Joystick,
                    value = (value.moveMethod as? BuiltInPresetKey.MoveMethod.Joystick)?.triggerSprint == true,
                    onValueChanged = {
                        if (value.moveMethod is BuiltInPresetKey.MoveMethod.Joystick) {
                            onValueChanged(value.copy(moveMethod = value.moveMethod.copy(triggerSprint = it)))
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
                    enabled = value.moveMethod is BuiltInPresetKey.MoveMethod.Dpad && (value.controlStyle as? BuiltInPresetKey.ControlStyle.SplitControls)?.buttonInteraction != true,
                    value = (value.moveMethod as? BuiltInPresetKey.MoveMethod.Dpad)?.swapJumpAndSneak == true,
                    onValueChanged = {
                        if (value.moveMethod is BuiltInPresetKey.MoveMethod.Dpad) {
                            onValueChanged(value.copy(moveMethod = value.moveMethod.copy(swapJumpAndSneak = it)))
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
                            BuiltInPresetKey.SprintButtonLocation.entries.indexOf(value.sprintButtonLocation)
                        val textFactory = LocalTextFactory.current
                        DropdownItemList(
                            modifier = Modifier.verticalScroll(),
                            items = BuiltInPresetKey.SprintButtonLocation.entries,
                            textProvider = { textFactory.of(it.nameId) },
                            selectedIndex = selectedIndex,
                            onItemSelected = { index ->
                                expanded = false
                                onValueChanged(value.copy(sprintButtonLocation = BuiltInPresetKey.SprintButtonLocation.entries[index]))
                            }
                        )
                    }
                ) {
                    Text(Text.translatable(value.sprintButtonLocation.nameId))
                    SelectIcon(expanded = expanded)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = Text.translatable(Texts.SCREEN_MANAGE_CONTROL_PRESET_USE_VANILLA_CHAT),
                )
                Switch(
                    value = value.useVanillaChat,
                    onValueChanged = {
                        onValueChanged(value.copy(useVanillaChat = it))
                    },
                )
            }
        }
    }
}