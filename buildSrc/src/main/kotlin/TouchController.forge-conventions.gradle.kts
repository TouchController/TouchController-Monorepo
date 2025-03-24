import org.gradle.accessors.dm.LibrariesForLibs
import org.spongepowered.asm.gradle.plugins.MixinExtension.AddMixinsToJarTask
import top.fifthlight.touchcontoller.gradle.MinecraftVersion

plugins {
    idea
    java
    id("net.minecraftforge.gradle")
    id("com.gradleup.gr8")
    id("org.parchmentmc.librarian.forgegradle")
    id("org.spongepowered.mixin")
    id("r8-parallel")
}

val libs = the<LibrariesForLibs>()

val modId: String by extra.properties
val modName: String by extra.properties
val modVersion: String by extra.properties
val modDescription: String by extra.properties
val modLicense: String by extra.properties
val modLicenseLink: String by extra.properties
val modIssueTracker: String by extra.properties
val modHomepage: String by extra.properties
val modAuthors: String by extra.properties
val modContributors: String by extra.properties
val gameVersion: String by extra.properties
val forgeVersion: String by extra.properties
val mappingType: String by extra.properties
val useMixin: String by extra.properties
val useMixinBool = useMixin.toBoolean()
val remapOutput: String by extra.properties
val remapOutputBool = remapOutput.toBoolean()
val useAccessTransformer: String by extra.properties
val useAccessTransformerBool = useAccessTransformer.toBoolean()
val useCoreMod: String by extra.properties
val useCoreModBool = useCoreMod.toBoolean()
val bridgeSlf4j: String by extra.properties
val bridgeSlf4jBool = bridgeSlf4j.toBoolean()
val legacyLanguageFormat: String by extra.properties
val legacyLanguageFormatBool = legacyLanguageFormat.toBoolean()
val excludeR8: String by extra.properties
val excludeR8Jar: String by extra.properties
val minecraftVersion = MinecraftVersion(gameVersion)

version = "$modVersion+forge-$gameVersion"
group = "top.fifthlight.touchcontroller"

minecraft {
    when (mappingType) {
        "official" -> {
            val parchmentVersion = properties["parchmentVersion"]?.toString()
            if (parchmentVersion != null) {
                mappings("parchment", parchmentVersion)
            } else {
                mappings("official", gameVersion)
            }
        }

        "mcp-snapshot" -> {
            val mcpVersion: String by extra.properties
            mappings("snapshot", mcpVersion)
        }
    }

    if (useAccessTransformerBool) {
        accessTransformer(file("src/main/resources/META-INF/touchcontroller_at.cfg"))
    }

    runs {
        copyIdeResources = true

        configureEach {
            workingDirectory(project.file("run"))
            properties["forge.logging.markers"] = "REGISTRIES"
            properties["forge.logging.console.level"] = "debug"

            mods {
                create(modId) {
                    sources(sourceSets.main.get())
                }
            }
        }

        create("client") {
            workingDirectory(project.file("run"))
        }
    }
}

mixin {
    add(sourceSets.getByName("main"), "mixins.${modId}.refmap.json")
    config("${modId}.mixins.json")
}

tasks.withType<AddMixinsToJarTask> {
    enabled = false
}

configurations.create("shadow")

tasks.jar {
    archiveBaseName = "$modName-slim"
}

fun DependencyHandlerScope.shadeAndImplementation(dependency: Any) {
    add("shadow", dependency)
    implementation(dependency)
    minecraftLibrary(dependency)
}

fun <T : ModuleDependency> DependencyHandlerScope.shadeAndImplementation(
    dependency: T,
    dependencyConfiguration: T.() -> Unit,
) {
    add("shadow", dependency, dependencyConfiguration)
    implementation(dependency, dependencyConfiguration)
    minecraftLibrary(dependency, dependencyConfiguration)
}

dependencies {
    minecraft("net.minecraftforge:forge:$gameVersion-$forgeVersion")

    shadeAndImplementation(project(":mod:resources", "texture"))
    shadeAndImplementation(project(":mod:resources", "forge-icon"))
    if (legacyLanguageFormatBool) {
        shadeAndImplementation(project(":mod:resources", "legacy-lang"))
    } else {
        shadeAndImplementation(project(":mod:resources", "lang"))
    }

    shadeAndImplementation(project(":mod:common")) {
        exclude("org.slf4j")
    }
    shadeAndImplementation(project(":combine"))
    if (bridgeSlf4jBool) {
        shadeAndImplementation(project(":log4j-slf4j2-impl")) {
            exclude("org.apache.logging.log4j")
        }
    }
    if (minecraftVersion < MinecraftVersion(1, 19, 3)) {
        shadeAndImplementation(libs.joml)
    }

    if (useMixinBool && remapOutputBool) {
        annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    }
}

tasks.processResources {
    val modAuthorsList = modAuthors.split(",").map(String::trim).filter(String::isNotEmpty)
    val modContributorsList = modContributors.split(",").map(String::trim).filter(String::isNotEmpty)
    fun String.quote(quoteStartChar: Char = '"', quoteEndChar: Char = '"') = quoteStartChar + this + quoteEndChar
    val modAuthorsArray = modAuthorsList.joinToString(", ", transform = String::quote).drop(1).dropLast(1)
    val modContributorsArray = modContributorsList.joinToString(", ", transform = String::quote).drop(1).dropLast(1)

    val loaderVersion = forgeVersion.substringBefore('.')
    val properties = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version_full" to version,
        "mod_license" to modLicense,
        "mod_license_link" to modLicenseLink,
        "mod_issue_tracker" to modIssueTracker,
        "mod_homepage" to modHomepage,
        "mod_authors_string" to modAuthors,
        "mod_contributors_string" to modContributors,
        "mod_authors_array" to modAuthorsArray,
        "mod_contributors_array" to modContributorsArray,
        "forge_version" to forgeVersion,
        "mod_description" to modDescription,
        "game_version" to gameVersion,
        "loader_version" to loaderVersion,
    )

    inputs.properties(properties)

    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "mcmod.info")) {
        expand(properties)
    }

    from(File(rootDir, "LICENSE")) {
        rename { "${it}_${modName}" }
    }
}

tasks.withType<Jar> {
    manifest {
        val attributes = mutableMapOf<String, String>()
        if (useCoreModBool) {
            attributes += ("FMLCorePlugin" to "top.fifthlight.touchcontroller.TouchControllerCorePlugin")
            attributes += ("FMLCorePluginContainsFMLMod" to "true")
        }
        if (useAccessTransformerBool) {
            attributes += ("FMLAT" to "touchcontroller_at.cfg")
        }
        if (useMixinBool && !remapOutputBool) {
            attributes += ("MixinConfigs" to "touchcontroller.mixins.json")
        }
        attributes(attributes)
    }
}

tasks.compileJava {
    dependsOn("createMcpToSrg")
    dependsOn("extractSrg")
}

val minecraftShadow = configurations.create("minecraftShadow") {
    excludeR8.split(",").filter(String::isNotEmpty).forEach {
        if (it.contains(":")) {
            val (group, module) = it.split(":")
            exclude(group, module)
        } else {
            exclude(it)
        }
    }
    extendsFrom(configurations.minecraft.get())
}

gr8 {
    create("gr8") {
        addProgramJarsFrom(configurations.getByName("shadow"))
        addProgramJarsFrom(tasks.jar)

        if (!excludeR8Jar.isBlank()) {
            val excludeR8JarRegex = excludeR8Jar.toRegex()
            val collections = project.objects.fileCollection()
            collections.from(minecraftShadow)
            addClassPathJarsFrom(collections.filter {
                !it.name.matches(excludeR8JarRegex)
            })
        } else {
            addClassPathJarsFrom(minecraftShadow)
        }

        r8Version("8.9.21")
        proguardFile(rootProject.file("mod/common-forge/rules.pro"))
    }
}

// Create a Jar task to exclude some META-INF files and module-info.class from R8 output,
// and make ForgeGradle reobf task happy (FG requires JarTask for it's reobf input)
val gr8JarTask = tasks.register<Jar>("gr8Jar") {
    dependsOn("reobfJar")

    inputs.files(tasks.getByName("gr8Gr8ShadowedJar").outputs.files)
    archiveBaseName = "$modName-noreobf"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    val jarFile =
        tasks.getByName("gr8Gr8ShadowedJar").outputs.files.first { it.extension.equals("jar", ignoreCase = true) }

    val excludeWhitelist = listOf(
        "touchcontroller_at.cfg",
        "mods.toml",
        "org.slf4j.spi.SLF4JServiceProvider",
    )
    from(zipTree(jarFile)) {
        exclude { file ->
            val path = file.relativePath
            if (path.segments.first() == "META-INF") {
                excludeWhitelist.all { !path.endsWith(it) }
            } else {
                path.lastName == "module-info.class"
            }
        }
    }
    mixinRefmapFile?.let { from(it) }
}

tasks.getByName("gr8Gr8ShadowedJar") {
    dependsOn("addMixinsToJar")
}

val mixinMappingFile =
    layout.buildDirectory.file("mixinMapping/mappings.tsrg").takeIf { remapOutputBool && useMixinBool }
val mixinRefmapFile =
    layout.buildDirectory.file("mixinMapping/mixins.$modId.refmap.json").takeIf { remapOutputBool && useMixinBool }
tasks.compileJava {
    inputs.properties("remapOutput" to remapOutputBool)
    mixinMappingFile?.let {
        outputs.files(mixinMappingFile)
        doLast {
            val mappingFile = mixinMappingFile.get().asFile.also { it.parentFile.mkdirs() }
            layout.buildDirectory.file("tmp/compileJava/compileJava-mappings.tsrg").get().asFile.copyTo(mappingFile)
        }
    }
    mixinRefmapFile?.let {
        outputs.files(mixinRefmapFile)
        doLast {
            val refmapFile = mixinRefmapFile.get().asFile.also { it.parentFile.mkdirs() }
            layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json").get().asFile.copyTo(refmapFile)
        }
    }
}

reobf {
    if (remapOutputBool) {
        create("gr8Jar") {
            // Use mapping from compileJava, to avoid problems of @Shadow
            mixinMappingFile?.let { extraMappings.from(it) }
        }
    }
}

tasks.register<Copy>("renameOutputJar") {
    val (dependTask, outputFile) = if (remapOutputBool) {
        Pair("reobfGr8Jar", "build/reobfGr8Jar/output.jar")
    } else {
        Pair(gr8JarTask, gr8JarTask.map { it.outputs.files.first() })
    }
    dependsOn(dependTask)
    from(outputFile) {
        rename {
            "$modName-$version.jar"
        }
    }
    destinationDir = layout.buildDirectory.dir("libs").get().asFile
}

tasks.assemble {
    if (remapOutputBool) {
        dependsOn("reobfGr8Jar")
    }
    dependsOn("renameOutputJar")
}
