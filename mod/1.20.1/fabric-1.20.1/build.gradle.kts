plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    id("TouchController.toolchain-conventions")
    id("TouchController.fabric-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.20.1/src/mixin/java")
}

dependencies {
    shadow(project(":mod:1.20.1:common-1.20.1"))
    implementation(project(":mod:1.20.1:common-1.20.1"))
}
