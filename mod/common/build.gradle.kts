plugins {
    `java-library`
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val modVersion: String by extra.properties

version = modVersion
group = "top.fifthlight.touchcontroller"

tasks.compileKotlin {
    dependsOn(":mod:resources:generate")
}

dependencies {
    api(project(":common-data"))
    implementation(project(":proxy-client"))
    implementation(project(":proxy-server"))

    api(project(":combine"))
    api(project(":combine-ui"))

    compileOnlyApi(libs.joml)

    api(libs.koin.core)
    api(libs.koin.compose)
    api(libs.koin.logger.slf4j)

    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    api(libs.voyager.navigator)
    api(libs.voyager.screenModel)
    api(libs.voyager.koin)
}

kotlin {
    jvmToolchain(8)
}
