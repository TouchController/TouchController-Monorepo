import org.gradle.accessors.dm.LibrariesForLibs
import top.fifthlight.touchcontoller.gradle.MinecraftVersion

plugins {
    java
    id("fabric-loom")
}

val libs = the<LibrariesForLibs>()

val modVersion: String by extra.properties
val gameVersion: String by extra.properties
val useAccessWidener: String by extra.properties
val useAccessWidenerBool = useAccessWidener.toBoolean()
val parchmentVersion = extra.properties["parchmentVersion"]?.toString()
val minecraftVersion = MinecraftVersion(gameVersion)

version = "$modVersion+$gameVersion"
group = "top.fifthlight.touchcontroller"

dependencies {
    minecraft("com.mojang:minecraft:$gameVersion")
    if (parchmentVersion != null) {
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-$gameVersion:$parchmentVersion@zip")
        })
    } else {
        mappings(loom.officialMojangMappings())
    }
    modCompileOnly(libs.fabric.loader)

    compileOnly(project(":mod:common"))
    compileOnly(project(":combine"))
    compileOnly(project(":mod:common-lwjgl3"))
    if (minecraftVersion < MinecraftVersion(1, 19, 3)) {
        compileOnly(libs.joml)
    }
}

loom {
    if (useAccessWidenerBool) {
        accessWidenerPath.set(file("src/main/resources/touchcontroller.accesswidener"))
    }
}