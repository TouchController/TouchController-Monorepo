package top.fifthlight.touchcontroller

import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.config.ConfigDirectoryProvider
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.NativeLibraryPathGetter
import top.fifthlight.touchcontroller.gal.KeyBindingHandlerImpl
import top.fifthlight.touchcontroller.gal.NativeLibraryPathGetterImpl

val loaderModule = module {
    single<KeyBindingHandler> { KeyBindingHandlerImpl }
    single<ConfigDirectoryProvider> { ConfigDirectoryProviderImpl }
    single<NativeLibraryPathGetter> { NativeLibraryPathGetterImpl }
}
