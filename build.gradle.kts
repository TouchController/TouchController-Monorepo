import java.io.FileNotFoundException
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm).apply(false)
    alias(libs.plugins.jetbrains.kotlin.serialization).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.rust.android.gradle).apply(false)
    alias(libs.plugins.jetbrains.kotlin.android).apply(false)
    alias(libs.plugins.maven.publish).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

project.ext["localProperties"] = Properties().apply {
    try {
        rootProject.file("local.properties").reader().use(::load)
    } catch (_: FileNotFoundException) {}
}

subprojects {
    group = "top.fifthlight.touchcontroller"

    repositories {
        mavenCentral()
        google()
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/")
        }
    }
}