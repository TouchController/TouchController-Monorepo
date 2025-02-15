plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
}

version = "0.0.1"

sourceSets.main {
    kotlin.srcDir(project(":mod:resources").layout.buildDirectory.dir("generated/kotlin/resources"))
}

tasks.compileKotlin {
    dependsOn(":mod:resources:generate")
}

dependencies {
    implementation(project(":combine"))
}

kotlin {
    jvmToolchain(8)
}
