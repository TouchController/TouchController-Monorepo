package top.fifthlight.touchcontroller.common.di

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.about.AboutInfoProvider
import top.fifthlight.touchcontroller.common.about.ResourcesAboutInfoProvider
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.config.preset.PresetManager
import top.fifthlight.touchcontroller.common.config.widget.WidgetPresetManager
import top.fifthlight.touchcontroller.common.input.InputManager
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.model.TouchStateModel
import top.fifthlight.touchcontroller.common.platform.PlatformProvider
import top.fifthlight.touchcontroller.common.ui.model.*

val appModule = module {
    single {
        @OptIn(ExperimentalSerializationApi::class)
        Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
            allowTrailingComma = true
            prettyPrint = true
            prettyPrintIndent = "  "
            isLenient = true
        }
    }
    single { GlobalConfigHolder() }
    single { PresetManager() }
    single { WidgetPresetManager() }
    single { ControllerHudModel() }
    single { TouchStateModel() }
    single { PlatformProvider() }
    single { InputManager }
    single<AboutInfoProvider> { ResourcesAboutInfoProvider }

    factory { params -> ItemListScreenModel(params[0], params[1]) }
    factory { params -> ComponentScreenModel(params[0], params[1]) }
    factory { AboutScreenModel() }
    factory { params -> ManageControlPresetsTabModel(params[0]) }
    factory { params -> CustomControlLayoutTabModel(params[0]) }
    factory { params -> PresetsTabModel(params[0]) }
    factory { params -> LayersTabModel(params[0]) }
    factory { params -> ImportPresetScreenModel(params[0]) }
    factory { params -> WidgetsTabModel(params[0]) }
    factory { params -> LayerEditorScreenModel(params[0], params[1]) }
    factory { params -> LayoutEditorCustomTabModel(params[0]) }
    factory { ConfigScreenModel() }
    factory { ChatScreenModel() }
}