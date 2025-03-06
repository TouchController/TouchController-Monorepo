package top.fifthlight.touchcontroller.di

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import top.fifthlight.touchcontroller.about.AboutInfoProvider
import top.fifthlight.touchcontroller.about.ResourcesAboutInfoProvider
import top.fifthlight.touchcontroller.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.config.preset.PresetManager
import top.fifthlight.touchcontroller.model.ControllerHudModel
import top.fifthlight.touchcontroller.model.TouchStateModel
import top.fifthlight.touchcontroller.platform.PlatformProvider
import top.fifthlight.touchcontroller.ui.model.*

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
    single { ControllerHudModel() }
    single { TouchStateModel() }
    single { PlatformProvider() }
    single<AboutInfoProvider> { ResourcesAboutInfoProvider }

    factory { params -> ItemListScreenModel(params[0], params[1]) }
    factory { params -> ComponentScreenModel(params[0], params[1]) }
    factory { AboutScreenModel() }
    factory { params -> ManageControlPresetsTabModel(params[0]) }
    factory { params -> CustomControlLayoutTabModel(params[0]) }
    factory { params -> PresetsTabModel(params[0]) }
    factory { params -> LayersTabModel(params[0]) }
    factory { params -> WidgetsTabModel(params[0]) }
    factory { ConfigScreenModel() }
}