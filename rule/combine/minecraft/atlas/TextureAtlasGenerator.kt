package top.fifthlight.combine.resources.altas

import top.fifthlight.bazel.worker.api.WorkRequest
import top.fifthlight.bazel.worker.api.Worker
import top.fifthlight.combine.resources.NinePatch
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.math.max

private data class Texture(
    val path: Path,
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
        path = path,
        identifier = identifier,
        position = position,
        size = size,
        ninePatch = ninePatch,
    )
}

private val atlasSize = IntSize(512, 512)

object TextureAtlasGenerator: Worker() {
    @JvmStatic
    fun main(vararg args: String) = run(*args)

    override fun handleRequest(
        request: WorkRequest,
        out: PrintWriter,
    ): Int {
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
    }
}
