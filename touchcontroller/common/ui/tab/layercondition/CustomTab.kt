package top.fifthlight.touchcontroller.common.ui.tab.layercondition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.condition.CustomLayerConditionKey
import top.fifthlight.touchcontroller.common.ui.component.ListButton
import top.fifthlight.touchcontroller.common.ui.model.LayoutEditorCustomTabModel

object CustomTab : LayerConditionTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_EDIT)
    }

    override val name: Identifier
        get() = Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION

    @Composable
    override fun Content() {
        val layerConditionTabContext = LocalLayerConditionTabContext.current
        val tabModel: LayoutEditorCustomTabModel = koinScreenModel { parametersOf(layerConditionTabContext) }
        val preset = layerConditionTabContext.preset
        val onConditionAdded = layerConditionTabContext.onConditionAdded
        val tabState by tabModel.uiState.collectAsState()

        AlertDialog(
            modifier = Modifier.fillMaxWidth(.4f),
            value = tabState,
            valueTransformer = { it.editState },
            title = {
                Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_EDIT))
            },
            action = {
                val editState = tabState.editState
                GuideButton(
                    onClick = {
                        editState?.let { editState ->
                            tabModel.editCondition(
                                preset, editState.index, editState.edit(
                                    preset.controlInfo.customConditions.conditions[editState.index]
                                )
                            )
                            tabModel.closeEditConditionDialog()
                        }
                    },
                ) {
                    Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_EDIT_OK))
                }
                Button(
                    onClick = {
                        tabModel.closeEditConditionDialog()
                    },
                ) {
                    Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_EDIT_CANCEL))
                }
            }
        ) { editState ->
            val defaultString = Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_UNNAMED).string
            EditText(
                modifier = Modifier.fillMaxWidth(),
                value = editState.name ?: defaultString,
                onValueChanged = {
                    tabModel.updateEditState {
                        copy(name = it)
                    }
                },
                placeholder = Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_NAME_PLACEHOLDER),
            )
        }

        Column(
            modifier = Modifier
                .padding(4),
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll()
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                for ((index, condition) in preset.controlInfo.customConditions.conditions.withIndex()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    ) {
                        ListButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                onConditionAdded(CustomLayerConditionKey(condition.uuid))
                            },
                        ) {
                            Text(
                                modifier = Modifier
                                    .alignment(Alignment.CenterLeft)
                                    .fillMaxWidth(),
                                text = condition.name?.let { Text.literal(it) }
                                    ?: Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_UNNAMED),
                            )
                        }

                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                tabModel.openEditConditionDialog(index, condition)
                            },
                        ) {
                            Icon(Textures.ICON_EDIT)
                        }
                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                tabModel.removeCondition(preset, index)
                            },
                        ) {
                            Icon(Textures.ICON_DELETE)
                        }
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    tabModel.addCondition(preset)
                },
            ) {
                Text(Text.translatable(Texts.SCREEN_LAYER_EDITOR_CUSTOM_CONDITION_ADD))
            }
        }
    }
}