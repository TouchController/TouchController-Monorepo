plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("TouchController.toolchain-conventions")
    id("TouchController.forge-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.20.4/src/mixin/java")
}

dependencies {
    shadow(project(":mod:1.20.4:common-1.20.4"))
    implementation(project(":mod:1.20.4:common-1.20.4"))
}
