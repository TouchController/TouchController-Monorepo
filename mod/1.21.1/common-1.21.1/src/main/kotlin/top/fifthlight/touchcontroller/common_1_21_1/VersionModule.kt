package top.fifthlight.touchcontroller.common_1_21_1

import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common_1_21_1_21_1.platformModule

val versionModule = module {
    includes(
        platformModule,
        appModule,
    )
}