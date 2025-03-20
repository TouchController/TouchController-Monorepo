plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("TouchController.toolchain-conventions")
    id("TouchController.forge-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.21.3/src/mixin/java")
}

dependencies {
    shadow(project(":mod:1.21.3:common-1.21.3"))
    implementation(project(":mod:1.21.3:common-1.21.3"))
}
