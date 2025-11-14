package top.fifthlight.touchcontroller.common.ui.tab.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.GuideButton
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.combine.widget.ui.WarningButton
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.ui.component.AppBar
import top.fifthlight.touchcontroller.common.ui.component.BackButton
import top.fifthlight.touchcontroller.common.ui.component.BuiltInPresetKeySelector
import top.fifthlight.touchcontroller.common.ui.component.Scaffold
import top.fifthlight.touchcontroller.common.ui.model.LocalConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.model.ManageControlPresetsTabModel
import top.fifthlight.touchcontroller.common.ui.tab.Tab
import top.fifthlight.touchcontroller.common.ui.tab.TabGroup
import top.fifthlight.touchcontroller.common.ui.tab.TabOptions

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
                BuiltInPresetKeySelector(
                    modifier = modifier,
                    value = currentPresetConfig.key,
                    onValueChanged = screenModel::updateKey,
                )
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