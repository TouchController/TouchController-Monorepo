package top.fifthlight.touchcontroller.common_1_21_4

import org.koin.dsl.module
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.platform_1_21_4.CanvasImpl
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common_1_21_3_1_21_4.platformModule

val versionModule = module {
    includes(
        platformModule,
        appModule,
    )
    factory<Canvas> { params -> CanvasImpl(params.get()) }
}