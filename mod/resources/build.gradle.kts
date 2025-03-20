plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

group = "top.fifthlight.touchcontroller"

val resourcesDir = layout.projectDirectory.dir("src/main/resources")

val generateSourcesDir = layout.buildDirectory.dir("generated/kotlin")
val generateAtlasFile = layout.buildDirectory.file("generated/atlas.png")
val generateLegacyLangDir = layout.buildDirectory.dir("generated/legacy-lang")

fun getBuildInfo(): Map<String, String> {
    val propertyNames = listOf(
        "modId",
        "modName",
        "modVersion",
        "modDescription",
        "modLicense",
        "modAuthors",
        "modContributors",
        "modState",
    )
    return propertyNames.associate { Pair(it, properties[it] as String) }
}

task<JavaExec>("generateResource") {
    dependsOn(tasks.compileKotlin)

    val buildInfo = getBuildInfo()
    val buildInfoStr = buildInfo.entries.joinToString("\n") { (key, value) -> "$key: $value" }

    inputs.properties(buildInfo)
    inputs.dir(resourcesDir)
    inputs.files(sourceSets["main"].runtimeClasspath)
    outputs.apply {
        dir(generateSourcesDir)
        file(generateAtlasFile)
        dir(generateLegacyLangDir)
    }

    group = "build"
    description = "Generate resource files"
    mainClass = "top.fifthlight.touchcontroller.resource.GenerateResourcesKt"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(
        buildInfoStr,
        resourcesDir.asFile.toString(),
        generateSourcesDir.get().toString(),
        generateAtlasFile.get().toString(),
        generateLegacyLangDir.get().toString(),
    )
}

val jarTasks = listOf(
    task<Jar>("generateLangJar") {
        archiveBaseName = "lang"
        destinationDirectory = layout.buildDirectory.dir("libs")

        from(resourcesDir.dir("lang").asFileTree) {
            into("assets/touchcontroller/lang")
        }
    },
    task<Jar>("generateLegacyLangJar") {
        dependsOn("generateResource")
        archiveBaseName = "legacy-lang"
        destinationDirectory = layout.buildDirectory.dir("libs")

        from(fileTree(generateLegacyLangDir)) {
            into("assets/touchcontroller/lang")
        }
    },
    task<Jar>("generateForgeIconJar") {
        archiveBaseName = "forge-icon"
        destinationDirectory = layout.buildDirectory.dir("libs")

        from(resourcesDir.file("icon.png"))
    },
    task<Jar>("generateFabricIconJar") {
        archiveBaseName = "fabric-icon"
        destinationDirectory = layout.buildDirectory.dir("libs")

        from(resourcesDir.file("icon.png")) {
            into("assets/touchcontroller")
        }
    },
    task<Jar>("generateTextureJar") {
        dependsOn("generateResource")
        archiveBaseName = "texture"
        destinationDirectory = layout.buildDirectory.dir("libs")

        from(generateAtlasFile) {
            into("assets/touchcontroller/textures/gui")
        }
        from(fileTree(resourcesDir.file("icon.png"))) {
            into("assets/touchcontroller/textures")
        }
        from(fileTree(resourcesDir.dir("texture/background"))) {
            into("assets/touchcontroller/textures/gui/background")
        }
    },
)

jarTasks.forEach { task ->
    val fileName = task.archiveBaseName.get()
    configurations {
        create(fileName)
    }
    artifacts {
        add(fileName, task)
    }
}

task("generate") {
    dependsOn("generateResource")
    jarTasks.forEach(::dependsOn)
}

tasks.build {
    dependsOn("generate")
}

tasks.processResources {
    enabled = false
}

dependencies {
    implementation(project(":common-data"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
