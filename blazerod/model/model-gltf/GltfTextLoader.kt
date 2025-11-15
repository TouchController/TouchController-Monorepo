package top.fifthlight.blazerod.model.gltf

import top.fifthlight.blazerod.model.ModelFileLoader
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.readText

class GltfTextLoader : ModelFileLoader {
    override val extensions = mapOf(
        "gltf" to setOf(
            ModelFileLoader.Ability.MODEL,
            ModelFileLoader.Ability.EMBED_ANIMATION,
        ),
    )

    // There is no reliable way to probe a glTF file
    override val probeLength: Int? = null
    override fun probe(buffer: ByteBuffer) = false

    override fun load(
        path: Path,
        basePath: Path,
    ): ModelFileLoader.LoadResult {
        val json = path.readText()
        return GltfLoader(
            buffer = null,
            filePath = path,
            basePath = basePath
        ).load(json)
    }
}