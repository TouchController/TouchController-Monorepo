package top.fifthlight.touchcontroller.resource

import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.math.max

private data class Texture(
    val relativePath: Path,
    val identifier: String,
    val ninePatch: NinePatch?,
    val image: BufferedImage,
) {
    val size: IntSize
        get() = IntSize(
            image.width,
            image.height,
        )

    fun place(position: IntOffset) = PlacedTexture(
        relativePath = relativePath,
        position = position,
        size = size,
        ninePatch = ninePatch,
    )
}

data class AtlasOutput(
    val placedTextures: Map<String, PlacedTexture>,
    val atlas: BufferedImage,
)

val atlasSize = IntSize(512, 512)

fun generateTextureAtlas(rootDir: Path, textureDirs: List<Path>): AtlasOutput {
    val textures = arrayListOf<Texture>()
    textureDirs.forEach { textureDir ->
        textureDir.walk().forEach { path ->
            val relativePath = path.relativeTo(rootDir)
            val transformedPath = relativePath.joinToString("_").uppercase()
            val fileName = path.fileName.toString()
            when {
                fileName.endsWith(".9.png", true) -> {
                    val image = ImageIO.read(path.toFile())
                    val ninePatch = NinePatch(image)
                    val croppedImage = image.getSubimage(1, 1, image.width - 2, image.height - 2)
                    val (compressedNinePatch, compressedImage) = compressNinePatch(ninePatch, croppedImage)
                    textures.add(
                        Texture(
                            relativePath = relativePath,
                            identifier = transformedPath.removeSuffix(".9.PNG"),
                            image = compressedImage,
                            ninePatch = compressedNinePatch,
                        )
                    )
                }

                fileName.endsWith(".png", true) -> {
                    val image = ImageIO.read(path.toFile())
                    textures.add(
                        Texture(
                            relativePath = relativePath,
                            identifier = transformedPath.removeSuffix(".PNG"),
                            image = image,
                            ninePatch = null,
                        )
                    )
                }
            }
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
            error("Texture ${texture.relativePath} too big: ${texture.size}")
        }
        if (texture.size.height + cursorPosition.y > atlasSize.height) {
            error("No space left for texture ${texture.relativePath}")
        }
        if (cursorPosition.x + texture.size.width > atlasSize.width) {
            if (maxLineHeight == 0) {
                error("Texture ${texture.relativePath} too big: ${texture.size}")
            }
            cursorPosition = IntOffset(0, cursorPosition.y + maxLineHeight)
            maxLineHeight = 0
        }
        maxLineHeight = max(maxLineHeight, texture.size.height)
        placedTextures[texture.identifier] = texture.place(cursorPosition)
        outputGraphics.drawImage(texture.image, cursorPosition.x, cursorPosition.y, null)
        cursorPosition = IntOffset(cursorPosition.x + texture.size.width, cursorPosition.y)
    }
    outputGraphics.dispose()

    return AtlasOutput(
        placedTextures = placedTextures,
        atlas = outputImage,
    )
}