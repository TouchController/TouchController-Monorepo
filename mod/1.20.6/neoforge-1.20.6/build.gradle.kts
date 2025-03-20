plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("TouchController.toolchain-conventions")
    id("TouchController.neoforge-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.20.6/src/mixin/java")
}

dependencies {
    shadow(project(":mod:1.20.6:common-1.20.6"))
    implementation(project(":mod:1.20.6:common-1.20.6"))
}
