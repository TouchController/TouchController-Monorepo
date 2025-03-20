plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("TouchController.toolchain-conventions")
    id("TouchController.fabric-conventions")
    id("TouchController.about-libraries-conventions")
}

sourceSets.main {
    java.srcDir("../common-1.21.4/src/mixin/java")
}

dependencies {
    shadow(project(":mod:1.21.4:common-1.21.4"))
    implementation(project(":mod:1.21.4:common-1.21.4"))
}
