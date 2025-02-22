plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

version = "0.0.1"

sourceSets.main {
    kotlin.srcDir(project(":mod:resources").layout.buildDirectory.dir("generated/kotlin"))
}

tasks.compileKotlin {
    dependsOn(":mod:resources:generate")
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(project(":combine"))
    api(libs.koin.compose)
}

kotlin {
    jvmToolchain(8)
}
