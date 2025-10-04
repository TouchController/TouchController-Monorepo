package top.fifthlight.blazerod.model.gltf

import top.fifthlight.blazerod.model.ModelFileLoader
import top.fifthlight.blazerod.model.util.readAll
import top.fifthlight.blazerod.model.util.readToBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class GltfBinaryLoader : ModelFileLoader {
    override val extensions = mapOf(
        "glb" to setOf(
            ModelFileLoader.Ability.MODEL,
            ModelFileLoader.Ability.EMBED_ANIMATION,
        ),
        "vrm" to setOf(
            ModelFileLoader.Ability.MODEL,
            ModelFileLoader.Ability.EMBED_ANIMATION,
            ModelFileLoader.Ability.EMBED_THUMBNAIL,
        ),
    )

    companion object {
        private const val GLTF_BINARY_MAGIC = 0x46546c67
    }

    override val probeLength = 4
    override fun probe(buffer: ByteBuffer): Boolean {
        if (buffer.remaining() < 4) return false
        val slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        return slice.getInt() == GLTF_BINARY_MAGIC
    }

    private enum class ChunkType(
        val id: UInt?,
    ) {
        JSON(0x4E4F534Au),
        BINARY(0x004E4942u),
        UNKNOWN(null),
    }

    internal data class ChunkData(
        val offset: Long,
        val length: Long,
    )

    internal data class ParsedGltfBinary(
        val json: String,
        val binaryChunkData: ChunkData? = null,
    )

    private fun parseGltfBinary(channel: FileChannel): ParsedGltfBinary {
        val readBuffer = ByteBuffer.allocate(16)
        readBuffer.order(ByteOrder.LITTLE_ENDIAN)

        fun readBytes(len: Int) {
            readBuffer.clear()
            readBuffer.limit(len)
            channel.readAll(readBuffer)
            readBuffer.flip()
        }

        readBytes(4)
        val magic = readBuffer.getInt()
        if (magic != GLTF_BINARY_MAGIC) {
            throw GltfLoadException(
                "Bad magic: want ${GLTF_BINARY_MAGIC.toString(16).padStart(8, '0')}, " +
                        "but got 0x${magic.toString(16).padStart(8, '0')}"
            )
        }

        readBytes(8)
        val version = readBuffer.getInt()
        if (version != 2) {
            throw GltfLoadException("Bad GLTF version: want 2, but get $version")
        }
        val totalLength = readBuffer.getInt().toUInt().toLong()
        if (totalLength < channel.size()) {
            throw GltfLoadException("Bad total length: file has ${channel.size()}, but total length is $totalLength")
        }

        fun readChunk(wantedType: ChunkType): ChunkData? {
            if (channel.position() == totalLength) {
                return null
            }
            readBytes(8)
            val length = readBuffer.getInt().toUInt().toLong()
            val type = readBuffer.getInt().toUInt()
            if (channel.position() + length > totalLength) {
                throw GltfLoadException("Bad chunk length: total length is $totalLength, current location is at ${channel.position()}, but want to read $length")
            }

            val chunkType = ChunkType.entries.firstOrNull { it.id == type } ?: ChunkType.UNKNOWN
            if (chunkType != wantedType) {
                throw GltfLoadException("Bad chunk type: $chunkType, want $wantedType")
            }

            val chunkPosition = channel.position()
            channel.position(chunkPosition + length)

            return ChunkData(
                offset = chunkPosition,
                length = length,
            )
        }

        val jsonChunk = readChunk(ChunkType.JSON)
            ?: throw GltfLoadException("Missing JSON chunk in binary GLTF")
        val json = StandardCharsets.UTF_8.decode(
            channel.readToBuffer(
                offset = jsonChunk.offset,
                length = jsonChunk.length,
                readSizeLimit = 4 * 1024 * 1024,
            )
        ).toString()
        val binaryChunkData = readChunk(ChunkType.BINARY)

        return ParsedGltfBinary(
            json = json,
            binaryChunkData = binaryChunkData,
        )
    }

    override fun load(path: Path, basePath: Path): ModelFileLoader.LoadResult {
        val json: String
        val binaryBuffer: ByteBuffer?
        FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val parsedGltf: ParsedGltfBinary = parseGltfBinary(channel)
            json = parsedGltf.json
            binaryBuffer = parsedGltf.binaryChunkData?.let {
                channel.readToBuffer(
                    offset = it.offset,
                    length = it.length,
                    readSizeLimit = 256 * 1024 * 1024,
                )
            }
        }

        val context = GltfLoader(
            buffer = binaryBuffer,
            filePath = path,
            basePath = path.parent,
        )
        return context.load(json)
    }

    override fun getThumbnail(path: Path, basePath: Path?): ModelFileLoader.ThumbnailResult {
        val json: String
        val binaryBuffer: ChunkData
        FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val parsedGltf: ParsedGltfBinary = parseGltfBinary(channel)
            json = parsedGltf.json
            binaryBuffer = parsedGltf.binaryChunkData ?: return ModelFileLoader.ThumbnailResult.None
        }
        val context = GltfLoader(
            buffer = null,
            filePath = path,
            basePath = path.parent,
        )
        return context.getThumbnail(json, binaryBuffer)
    }
}
