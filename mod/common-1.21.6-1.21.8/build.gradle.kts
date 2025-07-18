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
    versions("1.21.6", "1.21.7", "1.21.8")
}

dependencies {
    compileOnly(project(":mod:common"))
    compileOnly(project(":combine"))
    compileOnly(libs.joml)
    implementation(project(":mod:common-lwjgl3"))
    api(project(":mod:common-1.21.x"))
}
