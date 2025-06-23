plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    id("TouchController.toolchain-conventions")
    id("top.fifthlight.stubgen")
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

minecraftStub {
    versions("1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6")
}

dependencies {
    compileOnly(project(":mod:common"))
    compileOnly(project(":mod:common-lwjgl3"))
    compileOnly(project(":combine"))
    compileOnly(libs.joml)
}
