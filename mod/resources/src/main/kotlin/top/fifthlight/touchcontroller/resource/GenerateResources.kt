package top.fifthlight.touchcontroller.resource

import java.nio.file.Path
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val buildInfo = args[0].split('\n').associate { line -> val (name, value) = line.split(":"); Pair(name, value) }
    val (resourcesDir, generateSourcesDir, generateAtlasFile, generateLegacyLangDir) = args.toMutableList()
        .apply { removeFirst() }.map(Path::of)

    val textureDir = resourcesDir.resolve("texture")
    val atlasPaths = listOf(
        "control",
        "icon",
        "outside",
        "widget",
        "empty"
    ).map(textureDir::resolve)

    generateBuildInfo(buildInfo, generateSourcesDir)
    generateLegacyText(resourcesDir.resolve("lang"), generateLegacyLangDir)
    generateTextBinding(resourcesDir.resolve("lang/en_us.json"), generateSourcesDir)
    val (placedTextures, atlas) = generateTextureAtlas(textureDir, atlasPaths)
    ImageIO.write(atlas, "PNG", generateAtlasFile.toFile())
    generateTexturesBinding(placedTextures, generateSourcesDir)
    generateEmptyTextureBinding(placedTextures, generateSourcesDir)
    generateTextureSet(placedTextures, textureDir.resolve("control"), generateSourcesDir)
}