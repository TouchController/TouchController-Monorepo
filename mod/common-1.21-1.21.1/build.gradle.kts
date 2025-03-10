plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    id("top.fifthlight.stubgen")
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

minecraftStub {
    versions("1.21", "1.21.1")
}

dependencies {
    compileOnly(project(":mod:common"))
    compileOnly(project(":combine"))
    compileOnly(libs.joml)
    api(project(":mod:common-1.21.x"))
}
