package top.fifthlight.touchcontroller.common_1_21_3

import org.koin.dsl.module
import top.fifthlight.touchcontroller.common.di.appModule
import top.fifthlight.touchcontroller.common_1_21_3_1_21_4.platformModule

val versionModule = module {
    includes(
        platformModule,
        appModule,
    )
}