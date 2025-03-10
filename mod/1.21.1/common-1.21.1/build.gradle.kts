plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    id("TouchController.toolchain-conventions")
    id("TouchController.fabric-library-conventions")
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

dependencies {
    compileOnly(project(":mod:common"))
    compileOnly(project(":combine"))
    implementation(project(":mod:common-lwjgl3"))
    api(project(":mod:common-1.21-1.21.1"))
}
