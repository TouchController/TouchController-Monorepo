package top.fifthlight.touchcontroller.resource

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree
import kotlin.math.max

private fun Path.makeParentDirs() {
    Files.createDirectories(parent)
}

private data class Texture(
    val path: Path,
    val transformedPath: String,
    val ninePatch: NinePatch?,
    val image: BufferedImage,
) {
    val size: IntSize
        get() = IntSize(
            image.width,
            image.height,
        ) - if (ninePatch != null) {
            2
        } else {
            0
        }

    fun place(position: IntOffset) = PlacedTexture(
        position = position,
        size = size,
        ninePatch = ninePatch,
    )
}

val atlasSize = IntSize(512, 512)

fun main(args: Array<String>) {
    val (textureDirPath, outputGuiTextureAtlasPath, outputGuiTextureAtlasJsonPath) = args

    val textureDir = Path.of(textureDirPath)
    val outputGuiTextureAtlasFile = Path.of(outputGuiTextureAtlasPath)
    val outputGuiTextureAtlasJsonFile = Path.of(outputGuiTextureAtlasJsonPath)

    val textures = arrayListOf<Texture>()
    @OptIn(ExperimentalPathApi::class)
    textureDir.visitFileTree {
        onVisitFile { file, _ ->
            val relativePath = file.relativeTo(textureDir)
            val fileName = file.fileName.toString()
            when {
                fileName.endsWith(".9.png", true) -> {
                    val transformedPath = relativePath.joinToString("_").uppercase().removeSuffix(".9.PNG")
                    val image = ImageIO.read(file.toFile())
                    val ninePatch = NinePatch(image)
                    textures.add(
                        Texture(
                            path = file,
                            transformedPath = transformedPath,
                            image = image,
                            ninePatch = ninePatch,
                        )
                    )
                }

                fileName.endsWith(".png", true) -> {
                    val transformedPath = relativePath.joinToString("_").uppercase().removeSuffix(".PNG")
                    val image = ImageIO.read(file.toFile())
                    textures.add(
                        Texture(
                            path = file,
                            transformedPath = transformedPath,
                            image = image,
                            ninePatch = null,
                        )
                    )
                }
            }
            FileVisitResult.CONTINUE
        }
    }
    textures.sortByDescending { texture ->
        texture.size.width * texture.size.height
    }

    val outputImage = BufferedImage(atlasSize.width, atlasSize.height, TYPE_INT_ARGB)
    val outputGraphics = outputImage.createGraphics()
    val placedTextures = hashMapOf<String, PlacedTexture>()
    var cursorPosition = IntOffset(0, 0)
    var maxLineHeight = 0
    for (texture in textures) {
        if (texture.size.width > atlasSize.width) {
            error("Texture ${texture.transformedPath} too big: ${texture.size}")
        }
        if (texture.size.height + cursorPosition.y > atlasSize.height) {
            error("No space left for texture ${texture.transformedPath}")
        }
        if (cursorPosition.x + texture.size.width > atlasSize.width) {
            if (maxLineHeight == 0) {
                error("Texture ${texture.transformedPath} too big: ${texture.size}")
            }
            cursorPosition = IntOffset(0, cursorPosition.y + maxLineHeight)
            maxLineHeight = 0
        }
        maxLineHeight = max(maxLineHeight, texture.size.height)
        placedTextures[texture.transformedPath] = texture.place(cursorPosition)
        if (texture.ninePatch != null) {
            outputGraphics.drawImage(
                texture.image,
                cursorPosition.x,
                cursorPosition.y,
                cursorPosition.x + texture.size.width,
                cursorPosition.y + texture.size.height,
                1,
                1,
                1 + texture.size.width,
                1 + texture.size.height,
                null
            )
        } else {
            outputGraphics.drawImage(texture.image, cursorPosition.x, cursorPosition.y, null)
        }
        cursorPosition = IntOffset(cursorPosition.x + texture.size.width, cursorPosition.y)
    }
    outputGraphics.dispose()

    outputGuiTextureAtlasFile.makeParentDirs()
    outputGuiTextureAtlasJsonFile.makeParentDirs()

    @OptIn(ExperimentalSerializationApi::class)
    Json.encodeToStream(placedTextures, outputGuiTextureAtlasJsonFile.outputStream())
    ImageIO.write(outputImage, "png", outputGuiTextureAtlasFile.outputStream())
}