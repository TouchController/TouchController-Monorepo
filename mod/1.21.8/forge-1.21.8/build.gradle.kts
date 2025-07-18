plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("TouchController.toolchain-conventions")
    id("TouchController.forge-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.21.8/src/mixin/java")
}

dependencies {
    shadow(project(":mod:common-1.21.6-1.21.8"))
    implementation(project(":mod:common-1.21.6-1.21.8"))
}
