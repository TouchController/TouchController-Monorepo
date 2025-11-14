package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.config.preset.builtin.BuiltInPresetKey
import top.fifthlight.touchcontroller.common.ui.component.AppBar
import top.fifthlight.touchcontroller.common.ui.component.BackButton
import top.fifthlight.touchcontroller.common.ui.component.BuiltInPresetKeySelector
import top.fifthlight.touchcontroller.common.ui.component.Scaffold
import top.fifthlight.touchcontroller.common.ui.model.ImportPresetScreenModel

class ImportPresetScreen(private val onPresetKeySelected: (BuiltInPresetKey) -> Unit) : Screen {
    @Composable
    override fun Content() {
        val screenModel: ImportPresetScreenModel = koinScreenModel { parametersOf(onPresetKeySelected) }
        val navigator = LocalNavigator.current
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_IMPORT_BUILTIN_PRESET),
                        )
                    },
                    trailing = {
                        Button(onClick = {
                            navigator?.pop()
                            screenModel.finish()
                        }) {
                            Text(Text.translatable(Texts.SCREEN_IMPORT_BUILTIN_PRESET_FINISH))
                        }
                    }
                )
            },
        ) { modifier ->
            val key by screenModel.key.collectAsState()
            BuiltInPresetKeySelector(
                modifier = modifier,
                value = key,
                onValueChanged = screenModel::updateKey,
            )
        }
    }
}