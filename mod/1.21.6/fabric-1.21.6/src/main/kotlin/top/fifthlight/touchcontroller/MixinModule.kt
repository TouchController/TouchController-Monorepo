package top.fifthlight.touchcontroller

import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.gal.KeyBindingHandlerImpl

val mixinModule = module {
    single<KeyBindingHandler> { KeyBindingHandlerImpl }
}