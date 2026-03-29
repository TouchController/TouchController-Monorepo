package top.fifthlight.blazerod.model.pmx

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3fc
import org.slf4j.LoggerFactory
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.loader.LoadContext
import top.fifthlight.blazerod.model.loader.LoadParam
import top.fifthlight.blazerod.model.loader.LoadResult
import top.fifthlight.blazerod.model.loader.ModelFileLoader
import top.fifthlight.blazerod.model.loader.util.MMD_SCALE
import top.fifthlight.blazerod.model.loader.util.readAll
import top.fifthlight.blazerod.model.pmx.format.*
import top.fifthlight.blazerod.model.pmx.format.PmxMorphGroup.MorphItem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.channels.FileChannel
import java.nio.charset.CodingErrorAction
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

class PmxLoadException(message: String) : Exception(message)

// PMX loader.
// Format from https://gist.github.com/felixjones/f8a06bd48f9da9a4539f
class PmxLoader : ModelFileLoader {
    override val extensions = mapOf(
        "pmx" to setOf(ModelFileLoader.Ability.MODEL),
    )

    companion object {
        private val PMX_SIGNATURE = byteArrayOf(0x50, 0x4D, 0x58, 0x20)
        private val VALID_INDEX_SIZES = listOf(1, 2, 4)

        //                                             POS NORM UV
        private const val BASE_VERTEX_ATTRIBUTE_SIZE = (3 + 3 + 2) * 4

        //                                           JOINT WEIGHT
        private const val SKIN_VERTEX_ATTRIBUTE_SIZE = (4 + 4) * 4
        private const val VERTEX_ATTRIBUTE_SIZE = BASE_VERTEX_ATTRIBUTE_SIZE + SKIN_VERTEX_ATTRIBUTE_SIZE
        private val logger = LoggerFactory.getLogger(PmxLoader::class.java)
    }

    override val probeLength = PMX_SIGNATURE.size
    override fun probe(buffer: ByteBuffer): Boolean {
        if (buffer.remaining() < PMX_SIGNATURE.size) return false
        val signatureBytes = ByteArray(PMX_SIGNATURE.size)
        buffer.get(signatureBytes, 0, PMX_SIGNATURE.size)
        return signatureBytes.contentEquals(PMX_SIGNATURE)
    }

    private class MaterialData(
        val material: PmxMaterial,
        val vertexAttributes: Primitive.Attributes.Primitive,
        val indexBufferView: BufferView,
        val vertices: Int,
    )

    private class Context(
        private val context: LoadContext,
        private val param: LoadParam,
    ) {
        private var version: Float = 0f
        private lateinit var globals: PmxGlobals
        private val decoder by lazy {
            globals.textEncoding.charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
        }

        private lateinit var vertexBuffer: ByteBuffer
        private var vertices: Int = -1

        private lateinit var indexBuffer: IntArray
        private lateinit var indexBufferType: Accessor.ComponentType
        private var indices: Int = -1

        private lateinit var textures: List<Texture?>
        private lateinit var materials: List<MaterialData?>
        private lateinit var vertexToMaterialMap: VertexMaterialTable
        private lateinit var bones: List<PmxBone>
        private val effectorToIkDataMap = mutableMapOf<Int, MutableList<PmxBone.IkData>>()
        private val sourceToInheritMap = mutableMapOf<Int, MutableList<PmxBone.InheritData>>()
        private lateinit var morphTargets: List<PmxMorph>
        private lateinit var morphTargetGroups: List<PmxMorphGroup>
        private val childBoneMap = mutableMapOf<Int, MutableList<Int>>()
        private val rootBones = mutableListOf<Int>()
        private lateinit var rigidBodies: List<PmxRigidBody>
        private var boneToRigidBodyMap = mutableMapOf<Int, MutableList<Int>>()
        private lateinit var joints: List<PmxJoint>
        private var isKoikatsu: Boolean = false
        internal var spineIndex: Int = -1
        internal var pelvisIndex: Int = -1

        private fun loadRgbColor(buffer: ByteBuffer): RgbColor {
            if (buffer.remaining() < 3 * 4) {
                throw PmxLoadException("Bad file: want to read Vec3 (12 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return RgbColor(
                r = buffer.getFloat(),
                g = buffer.getFloat(),
                b = buffer.getFloat(),
            )
        }

        private fun loadRgbaColor(buffer: ByteBuffer): RgbaColor {
            if (buffer.remaining() < 4 * 4) {
                throw PmxLoadException("Bad file: want to read Vec4 (16 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return RgbaColor(
                r = buffer.getFloat(),
                g = buffer.getFloat(),
                b = buffer.getFloat(),
                a = buffer.getFloat(),
            )
        }

        private fun loadVector3f(buffer: ByteBuffer): Vector3f {
            if (buffer.remaining() < 3 * 4) {
                throw PmxLoadException("Bad file: want to read Vec3 (12 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat())
        }

        private fun loadSignature(buffer: ByteBuffer) {
            if (buffer.remaining() < PMX_SIGNATURE.size) {
                throw PmxLoadException("Bad file: signature is ${PMX_SIGNATURE.size} bytes, but only ${buffer.remaining()} bytes in buffer")
            }
            if (PMX_SIGNATURE.any { buffer.get() != it }) {
                throw PmxLoadException("Bad PMX signature")
            }
        }

        private fun loadGlobal(buffer: ByteBuffer) = PmxGlobals(
            textEncoding = when (val encoding = buffer.get().toUByte().toInt()) {
                0 -> PmxGlobals.TextEncoding.UTF16LE
                1 -> PmxGlobals.TextEncoding.UTF8
                else -> throw PmxLoadException("Bad text encoding: $encoding")
            },
            additionalVec4Count = buffer.get().toUByte().toInt().also {
                if (it !in 0..4) {
                    throw PmxLoadException("Bad additional vec4 count: $it, should be in [0, 4]")
                }
            },
            vertexIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad vertex index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            },
            textureIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad texture index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            },
            materialIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad material index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            },
            boneIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad bone index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            },
            morphIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad morph index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            },
            rigidBodyIndexSize = buffer.get().toUByte().toInt().also {
                if (it !in VALID_INDEX_SIZES) {
                    throw PmxLoadException("Bad rigid body index size: $it, should be ${VALID_INDEX_SIZES.joinToString(", ")}")
                }
            }
        )

        private fun loadBoneIndex(buffer: ByteBuffer): Int = when (globals.boneIndexSize) {
            1 -> buffer.get().toInt()
            2 -> buffer.getShort().toInt()
            4 -> buffer.getInt()
            else -> throw PmxLoadException("Bad bone index size: ${globals.boneIndexSize}")
        }

        private fun loadTextureIndex(buffer: ByteBuffer): Int = when (globals.textureIndexSize) {
            1 -> buffer.get().toInt()
            2 -> buffer.getShort().toInt()
            4 -> buffer.getInt()
            else -> throw PmxLoadException("Bad texture index size: ${globals.textureIndexSize}")
        }

        private fun loadMorphIndex(buffer: ByteBuffer) = when (globals.morphIndexSize) {
            1 -> buffer.get().toUByte().toInt()
            2 -> buffer.getShort().toUShort().toInt()
            4 -> buffer.getInt()
            else -> throw PmxLoadException("Bad morph index size: ${globals.morphIndexSize}")
        }

        private fun loadVertexIndex(buffer: ByteBuffer): Int = when (globals.vertexIndexSize) {
            1 -> buffer.get().toUByte().toInt()
            2 -> buffer.getShort().toUShort().toInt()
            4 -> buffer.getInt()
            else -> throw PmxLoadException("Bad vertex index size: ${globals.vertexIndexSize}")
        }

        private fun loadRigidBodyIndex(buffer: ByteBuffer): Int = when (globals.rigidBodyIndexSize) {
            1 -> buffer.get().toUByte().toInt()
            2 -> buffer.getShort().toUShort().toInt()
            4 -> buffer.getInt()
            else -> throw PmxLoadException("Bad rigid body index size: ${globals.rigidBodyIndexSize}")
        }

        private fun loadString(buffer: ByteBuffer): String {
            if (buffer.remaining() < 4) {
                throw PmxLoadException("No space for string index: want at least 4, but got ${buffer.remaining()}")
            }
            val length = buffer.getInt()
            if (length < 0) {
                throw PmxLoadException("Bad string size, should be at least 0: $length")
            }
            if (buffer.remaining() < length) {
                throw PmxLoadException("No enough data for string: want $length bytes, but only have ${buffer.remaining()} bytes")
            }
            val stringBuffer = buffer.slice(buffer.position(), length).order(ByteOrder.LITTLE_ENDIAN)
            return decoder.decode(stringBuffer).toString().also {
                buffer.position(buffer.position() + length)
            }
        }

        private fun loadHeader(buffer: ByteBuffer): PmxHeader {
            loadSignature(buffer)
            if (buffer.remaining() < 5) {
                throw PmxLoadException("Bad PMX signature")
            }
            val version = buffer.getFloat()
            if (version < 2.0f) {
                throw PmxLoadException("Bad PMX version: at least 2.0, but get $version")
            }
            this.version = version
            val globalsCount = buffer.get().toUByte().toInt()
            if (globalsCount < 8) {
                throw PmxLoadException("Bad global count: $globalsCount, at least 8")
            }
            globals = loadGlobal(buffer.slice(buffer.position(), globalsCount).order(ByteOrder.LITTLE_ENDIAN))
            buffer.position(buffer.position() + globalsCount)
            return PmxHeader(
                version = version,
                globals = globals,
                modelNameLocal = loadString(buffer),
                modelNameUniversal = loadString(buffer),
                commentLocal = loadString(buffer),
                commentUniversal = loadString(buffer),
            )
        }

        private fun wrapVertexBuffer(
            bufferName: String,
            buffer: ByteBuffer,
            vertexCount: Int,
        ): Primitive.Attributes.Primitive {
            val buffer = Buffer(
                name = bufferName,
                buffer = buffer,
            )
            val bufferView = BufferView(
                buffer = buffer,
                byteLength = buffer.buffer.remaining(),
                byteOffset = 0,
                byteStride = VERTEX_ATTRIBUTE_SIZE,
            )
            return Primitive.Attributes.Primitive(
                position = Accessor(
                    bufferView = bufferView,
                    byteOffset = 0,
                    componentType = Accessor.ComponentType.FLOAT,
                    normalized = false,
                    count = vertexCount,
                    type = Accessor.AccessorType.VEC3,
                ),
                normal = Accessor(
                    bufferView = bufferView,
                    byteOffset = 3 * 4,
                    componentType = Accessor.ComponentType.FLOAT,
                    normalized = false,
                    count = vertexCount,
                    type = Accessor.AccessorType.VEC3,
                ),
                texcoords = listOf(
                    Accessor(
                        bufferView = bufferView,
                        byteOffset = (3 + 3) * 4,
                        componentType = Accessor.ComponentType.FLOAT,
                        normalized = false,
                        count = vertexCount,
                        type = Accessor.AccessorType.VEC2,
                    )
                ),
                joints = listOf(
                    Accessor(
                        bufferView = bufferView,
                        byteOffset = (3 + 3 + 2) * 4,
                        componentType = Accessor.ComponentType.UNSIGNED_INT,
                        normalized = false,
                        count = vertexCount,
                        type = Accessor.AccessorType.VEC4,
                    )
                ),
                weights = listOf(
                    Accessor(
                        bufferView = bufferView,
                        byteOffset = (3 + 3 + 2 + 4) * 4,
                        componentType = Accessor.ComponentType.FLOAT,
                        normalized = false,
                        count = vertexCount,
                        type = Accessor.AccessorType.VEC4,
                    )
                )
            )
        }

        // Read all vertices from PMX file.
        private fun loadVertices(buffer: ByteBuffer) {
            val vertexCount = buffer.getInt()
            if (vertexCount <= 0) {
                throw PmxLoadException("Bad vertex count: $vertexCount, should be greater than 0")
            }

            val additionalVec4Size = globals.additionalVec4Count * 4 * 4
            val boneIndexSize = globals.boneIndexSize

            val outputBuffer =
                ByteBuffer.allocateDirect(vertexCount * VERTEX_ATTRIBUTE_SIZE).order(ByteOrder.nativeOrder())
            var outputPosition = 0
            var inputPosition = buffer.position()

            fun readFloat(): Float = buffer.getFloat(inputPosition).also {
                inputPosition += 4
            }

            fun readBoneIndex(): Int {
                val index = when (boneIndexSize) {
                    1 -> buffer.get(inputPosition).toInt()
                    2 -> buffer.getShort(inputPosition).toInt()
                    4 -> buffer.getInt(inputPosition)
                    else -> throw AssertionError()
                }
                inputPosition += boneIndexSize
                return index
            }

            fun readWeight(): Float = buffer.getFloat(inputPosition).also { inputPosition += 4 }
            fun readVector3f(dst: Vector3f) = dst.also {
                dst.set(
                    buffer.getFloat(inputPosition),
                    buffer.getFloat(inputPosition + 4),
                    buffer.getFloat(inputPosition + 8)
                )
                inputPosition += 12
            }

            val copyBaseVertexSize = BASE_VERTEX_ATTRIBUTE_SIZE - 24
            for (i in 0 until vertexCount) {
                // Read position data, transform xyz
                val x = buffer.getFloat(inputPosition)
                val y = buffer.getFloat(inputPosition + 4)
                val z = buffer.getFloat(inputPosition + 8)
                outputBuffer.putFloat(outputPosition, x * -MMD_SCALE)
                outputBuffer.putFloat(outputPosition + 4, y * MMD_SCALE)
                outputBuffer.putFloat(outputPosition + 8, z * MMD_SCALE)
                outputPosition += 12
                inputPosition += 12

                // Read normal data, transform x
                val nx = buffer.getFloat(inputPosition)
                val ny = buffer.getFloat(inputPosition + 4)
                val nz = buffer.getFloat(inputPosition + 8)
                outputBuffer.putFloat(outputPosition, -nx)
                outputBuffer.putFloat(outputPosition + 4, ny)
                outputBuffer.putFloat(outputPosition + 8, nz)
                outputPosition += 12
                inputPosition += 12

                // POSITION_NORMAL_UV_JOINT_WEIGHT
                outputBuffer.put(outputPosition, buffer, inputPosition, copyBaseVertexSize)
                outputPosition += copyBaseVertexSize
                inputPosition += copyBaseVertexSize

                // Skip additionalVec4
                inputPosition += additionalVec4Size

                // Weight deform type
                val weightDeformType = buffer.get(inputPosition).toUByte().toInt()
                inputPosition += 1

                val vec = Vector3f()
                // TODO: keep track of vertices without bone, to exclude non-skinned vertices out
                when (weightDeformType) {
                    // BDEF1
                    0 -> {
                        var index1 = readBoneIndex()
                        if (isKoikatsu && index1 == 0) {
                            // Weight scrubbing: If a vertex is weighted to Bone 0, it stays at world origin.
                            // We don't have per-vertex bone names, so we can't be ultra-selective here,
                            // but usually only "floating" parts or incorrectly ported ones have this issue.
                            // In this loader, we just let it be, but BDEF2/4 are more surgical.
                        }
                        outputBuffer.putInt(outputPosition, index1)
                        if (index1 != -1) {
                            outputBuffer.putFloat(outputPosition + 16, 1f)
                        }
                    }
                    // BDEF2
                    1 -> {
                        var index1 = readBoneIndex()
                        var index2 = readBoneIndex()
                        val weight1 = readWeight()

                        if (isKoikatsu) {
                            // Origin Weight Scrubber: If heavily weighted to Bone 0, redirect to skeleton anchors
                            // to prevent vertices sticking to (0,0,0) world origin.
                            if (index1 == 0 && weight1 > 0.9f) {
                                index1 = if (y > 1.25f) this.spineIndex else if (y > 0.8f) this.pelvisIndex else 0
                            }
                            if (index2 == 0 && (1f - weight1) > 0.9f) {
                                index2 = if (y > 1.25f) this.spineIndex else if (y > 0.8f) this.pelvisIndex else 0
                            }
                            
                            // Normal redirection for mixed weights
                            if (index1 == 0 && index2 > 0) index1 = index2
                            else if (index2 == 0 && index1 > 0) index2 = index1
                        }

                        outputBuffer.putInt(outputPosition, index1)
                        outputBuffer.putInt(outputPosition + 4, index2)
                        if (index1 != -1) {
                            outputBuffer.putFloat(outputPosition + 16, weight1)
                        }
                        if (index2 != -1) {
                            outputBuffer.putFloat(outputPosition + 20, 1f - weight1)
                        }
                    }
                    // BDEF4, or not really supported QDEF
                    2, 4 -> {
                        var index1 = readBoneIndex()
                        var index2 = readBoneIndex()
                        var index3 = readBoneIndex()
                        var index4 = readBoneIndex()
                        val weight1 = readWeight()
                        val weight2 = readWeight()
                        val weight3 = readWeight()
                        val weight4 = readWeight()

                        if (isKoikatsu) {
                            // Find first non-zero bone to replace any Bone 0 weights
                            var fallback = listOf(index1, index2, index3, index4).firstOrNull { it > 0 } ?: 0
                            
                            // If everything is Bone 0, use skeletal anchors
                            if (fallback == 0) {
                                fallback = if (y > 1.25f) this.spineIndex else if (y > 0.8f) this.pelvisIndex else 0
                            }

                            if (index1 == 0) index1 = fallback
                            if (index2 == 0) index2 = fallback
                            if (index3 == 0) index3 = fallback
                            if (index4 == 0) index4 = fallback
                        }

                        outputBuffer.putInt(outputPosition, index1)
                        outputBuffer.putInt(outputPosition + 4, index2)
                        outputBuffer.putInt(outputPosition + 8, index3)
                        outputBuffer.putInt(outputPosition + 12, index4)
                        if (index1 != -1) {
                            outputBuffer.putFloat(outputPosition + 16, weight1)
                        }
                        if (index2 != -1) {
                            outputBuffer.putFloat(outputPosition + 20, weight2)
                        }
                        if (index3 != -1) {
                            outputBuffer.putFloat(outputPosition + 24, weight3)
                        }
                        if (index4 != -1) {
                            outputBuffer.putFloat(outputPosition + 28, weight4)
                        }
                    }

                    3 -> {
                        // SDEF, not really supported, just treat as BDEF2
                        var index1 = readBoneIndex()
                        var index2 = readBoneIndex()
                        val weight1 = readWeight()

                        if (isKoikatsu) {
                            if (index1 == 0 && index2 > 0) index1 = index2
                            else if (index2 == 0 && index1 > 0) index2 = index1
                        }

                        outputBuffer.putInt(outputPosition, index1)
                        outputBuffer.putInt(outputPosition + 4, index2)
                        if (index1 != -1) {
                            outputBuffer.putFloat(outputPosition + 16, weight1)
                        }
                        if (index2 != -1) {
                            outputBuffer.putFloat(outputPosition + 20, 1f - weight1)
                        }
                        val c = readVector3f(vec)
                        val r0 = readVector3f(vec)
                        val r1 = readVector3f(vec)
                    }
                }
                outputPosition += SKIN_VERTEX_ATTRIBUTE_SIZE

                // Skin edge scale
                inputPosition += 4
            }
            require(outputPosition == outputBuffer.capacity()) { "Bug: Not filled the entire output buffer" }

            vertices = vertexCount
            vertexBuffer = outputBuffer
            buffer.position(inputPosition)
        }

        private fun loadSurfaces(buffer: ByteBuffer) {
            val surfaceCount = buffer.getInt()
            if (surfaceCount % 3 != 0) {
                throw PmxLoadException("Bad surface count: $surfaceCount % 3 != 0")
            }
            val triangleCount = surfaceCount / 3
            val vertexIndexSize = globals.vertexIndexSize
            val indexBufferSize = vertexIndexSize * surfaceCount
            if (buffer.remaining() < indexBufferSize) {
                throw PmxLoadException("Bad surface data: should have $indexBufferSize bytes, but only ${buffer.remaining()} bytes available")
            }

            val outputIndicesArray = IntArray(surfaceCount)
            val outputIndices = IntBuffer.wrap(outputIndicesArray)
            // PMX use clockwise indices, but OpenGL use counterclockwise indices, so let's invert the order here.
            when (vertexIndexSize) {
                1 -> {
                    indexBufferType = Accessor.ComponentType.UNSIGNED_BYTE
                    for (i in 0 until triangleCount) {
                        outputIndices.put(buffer.get().toUByte().toInt())
                        val a = buffer.get().toUByte().toInt()
                        val b = buffer.get().toUByte().toInt()
                        outputIndices.put(b)
                        outputIndices.put(a)
                    }
                }

                2 -> {
                    indexBufferType = Accessor.ComponentType.UNSIGNED_SHORT
                    for (i in 0 until triangleCount) {
                        outputIndices.put(buffer.getShort().toUShort().toInt())
                        val a = buffer.getShort()
                        val b = buffer.getShort()
                        outputIndices.put(b.toUShort().toInt())
                        outputIndices.put(a.toUShort().toInt())
                    }
                }

                4 -> {
                    indexBufferType = Accessor.ComponentType.UNSIGNED_INT
                    for (i in 0 until triangleCount) {
                        outputIndices.put(buffer.getInt())
                        val a = buffer.getInt()
                        val b = buffer.getInt()
                        outputIndices.put(b)
                        outputIndices.put(a)
                    }
                }

                else -> throw AssertionError()
            }
            indexBuffer = outputIndicesArray
            indices = surfaceCount
        }

        private fun loadTextures(buffer: ByteBuffer) {
            val textureCount = buffer.getInt()
            if (textureCount < 0) {
                throw PmxLoadException("Bad texture count: $textureCount, should be at least zero")
            }
            textures = (0 until textureCount).map {
                try {
                    val pathString = loadString(buffer)
                    val buffer = context.loadExternalResource(
                        path = pathString,
                        type = LoadContext.ResourceType.TEXTURE,
                        caseInsensitive = true,
                        maxSize = 256 * 1024 * 1024,
                    )
                    Texture(
                        name = pathString,
                        bufferView = BufferView(
                            buffer = Buffer(
                                name = "Texture $pathString",
                                buffer = buffer,
                            ),
                            byteLength = buffer.remaining(),
                            byteOffset = 0,
                            byteStride = 0,
                        ),
                        sampler = Texture.Sampler(
                            magFilter = param.samplerMagFilter ?: Texture.Sampler.MagFilter.LINEAR,
                            minFilter = param.samplerMinFilter ?: Texture.Sampler.MinFilter.LINEAR,
                        ),
                    )
                } catch (ex: Exception) {
                    logger.warn("Failed to load PMX texture", ex)
                    return@map null
                }
            }
        }

        private fun loadMaterials(buffer: ByteBuffer) {
            val materialCount = buffer.getInt()

            fun loadDrawingFlags(buffer: ByteBuffer): PmxMaterial.DrawingFlags {
                val byte = buffer.get().toUByte().toInt()
                fun loadBitfield(index: Int): Boolean = (byte and (1 shl index)) != 0
                return PmxMaterial.DrawingFlags(
                    noCull = loadBitfield(0),
                    groundShadow = loadBitfield(1),
                    drawShadow = loadBitfield(2),
                    receiveShadow = loadBitfield(3),
                    hasEdge = loadBitfield(4),
                    vertexColor = loadBitfield(5),
                    pointDrawing = loadBitfield(6),
                    lineDrawing = loadBitfield(7),
                )
            }

            val vertexToMaterialMap = VertexMaterialTable(vertices, materialCount)

            var indexOffset = 0
            materials = (0 until materialCount).map { materialIndex ->
                val pmxMaterial = PmxMaterial(
                    nameLocal = loadString(buffer),
                    nameUniversal = loadString(buffer),
                    diffuseColor = loadRgbaColor(buffer),
                    specularColor = loadRgbColor(buffer),
                    specularStrength = buffer.getFloat(),
                    ambientColor = loadRgbColor(buffer),
                    drawingFlags = loadDrawingFlags(buffer),
                    edgeColor = loadRgbaColor(buffer),
                    edgeScale = buffer.getFloat(),
                    textureIndex = loadTextureIndex(buffer),
                    environmentIndex = loadTextureIndex(buffer),
                    environmentBlendMode = when (val mode = buffer.get().toInt()) {
                        0 -> PmxMaterial.EnvironmentBlendMode.DISABLED
                        1 -> PmxMaterial.EnvironmentBlendMode.MULTIPLY
                        2 -> PmxMaterial.EnvironmentBlendMode.ADDICTIVE
                        3 -> PmxMaterial.EnvironmentBlendMode.ADDITIONAL_VEC4
                        else -> throw PmxLoadException("Unsupported environment blend mode: $mode")
                    },
                    toonReference = when (val type = buffer.get().toInt()) {
                        0 -> PmxMaterial.ToonReference.Texture(index = loadTextureIndex(buffer))
                        1 -> PmxMaterial.ToonReference.Internal(index = buffer.get().toUByte())
                        else -> throw PmxLoadException("Unsupported toon reference: $type")
                    },
                    metadata = loadString(buffer),
                    surfaceCount = buffer.getInt().also {
                        if (it < 0) {
                            throw PmxLoadException("Material with $it vertices. Should be greater than zero.")
                        }
                        if (it % 3 != 0) {
                            throw PmxLoadException("Material with $it % 3 != 0 vertices.")
                        }
                    },
                )

                if (pmxMaterial.surfaceCount == 0) {
                    return@map null
                }

                var nextRemappedVertexIndex = 0
                val remappedIndices = ByteBuffer.allocateDirect(pmxMaterial.surfaceCount * indexBufferType.byteLength)
                    .order(ByteOrder.nativeOrder())
                val remappedVertices = ByteBuffer.allocateDirect(pmxMaterial.surfaceCount * VERTEX_ATTRIBUTE_SIZE)
                for (index in indexOffset until (indexOffset + pmxMaterial.surfaceCount)) {
                    val vertexIndex = indexBuffer[index]
                    if (vertexIndex >= vertices) {
                        throw PmxLoadException("Vertex index $vertexIndex out of bounds")
                    }
                    var remappedIndex = vertexToMaterialMap.getLocalIndex(vertexIndex, materialIndex)
                    if (remappedIndex == -1) {
                        remappedIndex = nextRemappedVertexIndex++
                        vertexToMaterialMap.setLocalIndex(vertexIndex, materialIndex, remappedIndex)

                        vertexBuffer.position(vertexIndex * VERTEX_ATTRIBUTE_SIZE)
                        vertexBuffer.limit(vertexBuffer.position() + VERTEX_ATTRIBUTE_SIZE)
                        remappedVertices.put(vertexBuffer)
                        vertexBuffer.clear()
                    }
                    when (indexBufferType) {
                        Accessor.ComponentType.UNSIGNED_BYTE -> remappedIndices.put(remappedIndex.toByte())
                        Accessor.ComponentType.UNSIGNED_SHORT -> remappedIndices.putShort(remappedIndex.toShort())
                        Accessor.ComponentType.UNSIGNED_INT -> remappedIndices.putInt(remappedIndex)
                        else -> throw AssertionError()
                    }
                }

                indexOffset += pmxMaterial.surfaceCount
                remappedVertices.flip()
                remappedIndices.flip()
                vertexBuffer.clear()

                MaterialData(
                    material = pmxMaterial,
                    vertexAttributes = wrapVertexBuffer(
                        bufferName = "Vertex buffer for material ${pmxMaterial.nameLocal}",
                        buffer = remappedVertices,
                        vertexCount = nextRemappedVertexIndex,
                    ),
                    indexBufferView = BufferView(
                        buffer = Buffer(
                            name = "Index buffer for material ${pmxMaterial.nameLocal}",
                            buffer = remappedIndices,
                        ),
                        byteLength = remappedIndices.remaining(),
                        byteOffset = 0,
                        byteStride = 0,
                    ),
                    vertices = nextRemappedVertexIndex,
                )
            }

            this.vertexToMaterialMap = vertexToMaterialMap
        }

        private fun Vector3f.transformPosition() = also {
            mul(MMD_SCALE)
            x = -x
        }

        private fun loadBones(buffer: ByteBuffer) {
            val boneCount = buffer.getInt()
            if (boneCount < 0) {
                throw PmxLoadException("Bad PMX model: bones count less than zero")
            }

            fun loadBoneFlags(buffer: ByteBuffer): PmxBone.Flags {
                val flags = buffer.getShort().toInt()
                fun loadBitfield(index: Int): Boolean = (flags and (1 shl index)) != 0
                return PmxBone.Flags(
                    indexedTailPosition = loadBitfield(0),
                    rotatable = loadBitfield(1),
                    translatable = loadBitfield(2),
                    isVisible = loadBitfield(3),
                    enabled = loadBitfield(4),
                    ik = loadBitfield(5),
                    inheritLocal = loadBitfield(7),
                    inheritRotation = loadBitfield(8),
                    inheritTranslation = loadBitfield(9),
                    fixedAxis = loadBitfield(10),
                    localCoordinate = loadBitfield(11),
                    physicsAfterDeform = loadBitfield(12),
                    externalParentDeform = loadBitfield(13),
                )
            }

            fun loadBone(index: Int, buffer: ByteBuffer): PmxBone {
                val nameLocal = loadString(buffer)
                val nameUniversal = loadString(buffer)
                val position = loadVector3f(buffer).transformPosition()
                val parentBoneIndex = loadBoneIndex(buffer)
                val layer = buffer.getInt()
                val flags = loadBoneFlags(buffer)
                val tailPosition = if (flags.indexedTailPosition) {
                    PmxBone.TailPosition.Indexed(loadBoneIndex(buffer))
                } else {
                    PmxBone.TailPosition.Scalar(loadVector3f(buffer).transformPosition())
                }
                val inheritParent = if (flags.inheritRotation || flags.inheritTranslation) {
                    Pair(loadBoneIndex(buffer), buffer.getFloat())
                } else {
                    null
                }
                val axisDirection = if (flags.fixedAxis) {
                    loadVector3f(buffer).transformPosition()
                } else {
                    null
                }
                val localCoordinate = if (flags.localCoordinate) {
                    PmxBone.LocalCoordinate(
                        loadVector3f(buffer).transformPosition(),
                        loadVector3f(buffer).transformPosition()
                    )
                } else {
                    null
                }
                val externalParentIndex = if (flags.externalParentDeform) {
                    loadBoneIndex(buffer)
                } else {
                    null
                }
                val ikData = if (flags.ik) {
                    val targetIndex = loadBoneIndex(buffer)
                    val loopCount = buffer.getInt()
                    val limitRadian = buffer.getFloat()
                    val linkCount = buffer.getInt()
                    val links = (0 until linkCount).map {
                        val index = loadBoneIndex(buffer)
                        val limits = if (buffer.get() != 0.toByte()) {
                            PmxBone.IkLink.Limits(
                                limitMin = loadVector3f(buffer),
                                limitMax = loadVector3f(buffer),
                            )
                        } else {
                            null
                        }
                        PmxBone.IkLink(
                            index = index,
                            limits = limits,
                        )
                    }
                    PmxBone.IkData(
                        effectorIndex = index,
                        targetIndex = targetIndex,
                        loopCount = loopCount,
                        limitRadian = limitRadian,
                        links = links,
                    ).also {
                        effectorToIkDataMap.getOrPut(index, ::mutableListOf).add(it)
                    }
                } else {
                    null
                }
                return PmxBone(
                    index = index,
                    nameLocal = nameLocal,
                    nameUniversal = nameUniversal,
                    position = position,
                    parentBoneIndex = parentBoneIndex.takeIf { it >= 0 },
                    layer = layer,
                    flags = flags,
                    tailPosition = tailPosition,
                    inheritParentIndex = inheritParent?.first,
                    inheritParentInfluence = inheritParent?.second,
                    axisDirection = axisDirection,
                    localCoordinate = localCoordinate,
                    externalParentIndex = externalParentIndex,
                    ikData = ikData,
                ).also { bone ->
                    bone.inheritData?.let { inheritData ->
                        sourceToInheritMap.getOrPut(inheritData.sourceIndex) { mutableListOf() }.add(inheritData)
                    }
                }
            }

            bones = (0 until boneCount).map { index ->
                loadBone(index, buffer).also { bone ->
                    bone.parentBoneIndex?.let { parentBoneIndex ->
                        childBoneMap.getOrPut(parentBoneIndex) { mutableListOf() }.add(index)
                    } ?: run {
                        rootBones.add(index)
                    }
                }
            }
            
            // isKoikatsu detection - broaden to catch all major prefixes and common Koikatsu naming patterns
            isKoikatsu = bones.any { 
                val n = it.nameLocal.lowercase()
                n.startsWith("cf_") || n.startsWith("ct_") || n.contains("k_f_") || n.contains("k_t_") ||
                n.contains("bust") || n.contains("breast") || n.contains("skirt") ||
                n.contains("胸") || n.contains("乳") || n.contains("髪") || n.contains("スカート")
            }
            if (isKoikatsu) {
                logger.info("[HIERARCHY-REPAIR] Koikatsu-style bone naming detected. Hierarchy repair active.")
            }
        }

        private fun repairKoikatsuHierarchy() {
            if (!isKoikatsu) return

            // PASS 1: Discovery. Find anchor bones regardless of their position in the file.
            // Using a scoring system to prioritize core Joints (cf_j_) over Sub-bones (cf_s_)
            // and prioritizing the "Animated Skeleton" (Indices 150+) over "Root Skeleton" (Indices 1-100).
            fun findAnchor(patterns: List<String>, ignorePatterns: List<String> = listOf("bnip", "tw", "adj", "bust")): Int {
                var bestIndex = -1
                var bestScore = -1.0

                for (i in bones.indices) {
                    val bone = bones[i]
                    val n = bone.nameLocal.lowercase()
                    
                    // Search all bones except index 0 (World Root)
                    if (i == 0) continue 
                    
                    // Check if name matches any pattern
                    if ((patterns.any { p -> n.contains(p) }) && !ignorePatterns.any { it in n }) {
                        var score = 0.0
                        
                        // Tier 1: Core Joints (cf_j_) are the highest priority for moving skeleton
                        if (n.startsWith("cf_j_")) score += 1000.0
                        
                        // Tier 2: Exact name match
                        if (patterns.any { p -> n == "cf_j_$p" || n == p }) score += 500.0
                        
                        // Tier 3: Animated Skeleton Bonus (Indices 100-350) 
                        // In Arslan ports, the character skeleton is often a secondary entity later in the file.
                        if (i > 100) score += 300.0

                        // Tier 4: Sub joints or Dynamic bones (cf_s_ / cf_d_)
                        if (n.startsWith("cf_s_") || n.startsWith("cf_d_")) score += 100.0
                        
                        // Tiebreaker: Higher index usually means the more specific/animated joint (waist02 vs hips)
                        score += i.toDouble() / 1000.0
                        
                        if (score > bestScore) {
                            bestScore = score
                            bestIndex = i
                        }
                    }
                }
                return bestIndex
            }

            // Diagnostic: Dump the skeletal range if in Koikatsu mode to detect ghost bones
            for (i in 150..minOf(bones.size - 1, 280)) {
                val b = bones[i]
                logger.debug("[HIERARCHY-DIAG] Bone $i: ${b.nameLocal} (Parent: ${b.parentBoneIndex})")
            }

            val spine03Index = findAnchor(listOf("spine03"))
            val spine02Index = findAnchor(listOf("spine02"))
            val spine01Index = findAnchor(listOf("spine01"))
            val spineIndex = findAnchor(listOf("spine", "脊髄"))

            val hipsIndex = findAnchor(listOf("hips"))
            val waist02Index = findAnchor(listOf("waist02"))
            val waist01Index = findAnchor(listOf("waist01"))
            val waistIndex = findAnchor(listOf("waist", "腰"))
            val pelvisIndex_alt = findAnchor(listOf("pelvis", "下半身", "lower"))

            val headIndex = findAnchor(listOf("head", "頭"))
            val neckIndex = findAnchor(listOf("neck", "首"))

            this.spineIndex = when {
                spine03Index != -1 -> spine03Index
                spine02Index != -1 -> spine02Index
                spine01Index != -1 -> spine01Index
                spineIndex != -1 -> spineIndex
                else -> -1
            }
            
            this.pelvisIndex = when {
                waist02Index != -1 -> waist02Index
                waist01Index != -1 -> waist01Index
                hipsIndex != -1 -> hipsIndex
                waistIndex != -1 -> waistIndex
                pelvisIndex_alt != -1 -> pelvisIndex_alt
                else -> -1
            }

            // PASS 2: Hierarchical Fallbacks. If anchors are still missing, look at parents of known extremities.
            if (this.pelvisIndex == -1) {
                val legFirst = findAnchor(listOf("leg_l", "足_l", "leg_r", "足_r"))
                if (legFirst != -1) {
                    this.pelvisIndex = bones[legFirst].parentBoneIndex ?: -1
                    logger.info("[HIERARCHY-REPAIR] Pelvis fallback to Leg Parent: ${this.pelvisIndex}")
                }
            }
            if (this.spineIndex == -1) {
                val neckFirst = findAnchor(listOf("neck", "首"))
                if (neckFirst != -1) {
                    this.spineIndex = bones[neckFirst].parentBoneIndex ?: -1
                    logger.info("[HIERARCHY-REPAIR] Spine fallback to Neck Parent: ${this.spineIndex}")
                }
            }

            val headAnchorIndex = when {
                headIndex != -1 -> headIndex
                neckIndex != -1 -> neckIndex
                this.spineIndex != -1 -> this.spineIndex
                else -> -1
            }

            if (this.spineIndex == -1 && this.pelvisIndex == -1) {
                logger.warn("[HIERARCHY-REPAIR] Koikatsu model detected but no anchor bones (spine/hips) found. Repair might be incomplete.")
            } else {
                val spineName = if (this.spineIndex != -1) bones[this.spineIndex].nameLocal else "NONE"
                val hipsName = if (this.pelvisIndex != -1) bones[this.pelvisIndex].nameLocal else "NONE"
                val headName = if (headAnchorIndex != -1) bones[headAnchorIndex].nameLocal else "NONE"
                logger.info("[HIERARCHY-REPAIR] Anchor bones found: Spine=${this.spineIndex} ($spineName), Hips=${this.pelvisIndex} ($hipsName), Head=$headAnchorIndex ($headName)")
            }

            logger.info("[HIERARCHY-REPAIR] Identifying and repairing dangling bones...")
            
            // PASS 3: Repair. Use the pre-discovered anchors to reparent orphan/anchor-locked bones.
            val newBones = bones.mapIndexed { index, bone ->
                val currentParent = bone.parentBoneIndex ?: -1
                val name = bone.nameLocal.lowercase()
                
                // Skeleton protection: Don't reparent the main movement roots
                val isCoreSkeleton = name.contains("センター") || name.contains("center") || 
                                     name.contains("グルーブ") || name.contains("groove") ||
                                     name.contains("全ての親")
                                     
                // V15 Surgical Check: Only reparent if bone is a true orphan (Parent <= 0)
                // If it's already part of the character skeleton (Parent > 100), leave it alone!
                if (currentParent <= 0 && !isCoreSkeleton) {
                    val newParent = when {
                        // V16: Specialized body part matching for Arslan/Kisara control nodes
                        (name.contains("bust") || name.contains("breast") || name.contains("胸") || name.contains("乳") || name.contains("胸操作")) && this.spineIndex != -1 -> this.spineIndex
                        
                        // Skirts and lower body dynamics
                        (name.contains("skirt") || name.contains("スカート") || name.contains("腰") || name.contains("腰操作") || name.contains("cf_j_sk_") || name.contains("cf_d_sk_")) && this.pelvisIndex != -1 -> this.pelvisIndex
                        
                        // Hair and face accessories (including hair-operation nodes)
                        (name.contains("hair") || name.contains("髪") || name.contains("髪操作") || name.contains("ribbon") || name.contains("ct_")) && headAnchorIndex != -1 -> headAnchorIndex
 
                        // General dynamic adjustment bones and twist helpers
                        (name.contains("cf_s_") || name.contains("cf_d_") || name.contains("cf_m_") || name.contains("捩") || name.contains("肩")) -> {
                            when {
                                name.contains("arm") || name.contains("shoulder") || name.contains("hand") || name.contains("bust") || name.contains("breast") || name.contains("胸") -> this.spineIndex
                                name.contains("leg") || name.contains("foot") || name.contains("skirt") || name.contains("hips") || name.contains("腰") -> this.pelvisIndex
                                else -> this.spineIndex // Fallback to upper body
                            }
                        }
                        else -> -1
                    }
                    if (newParent != -1 && newParent != index && newParent != currentParent) {
                        logger.info("[HIERARCHY-REPAIR] Reparenting anchor-locked bone ${bone.nameLocal} (index $index, parent $currentParent) to parent index $newParent")
                        
                        // Update childBoneMap for consistency
                        childBoneMap.getOrPut(newParent) { mutableListOf() }.add(index)
                        
                        // CRITICAL: Remove from old parent child map
                        if (currentParent != -1) {
                            childBoneMap[currentParent]?.remove(index)
                        }
                        
                        // Update rootBones - remove the bone from root list since it now has a parent
                        rootBones.removeIf { it == index }
                        
                        bone.copy(parentBoneIndex = newParent)
                    } else {
                        bone
                    }
                } else {
                    bone
                }
            }
            bones = newBones
            
            // Task 3: Verification - Log any remaining root bones for diagnostic purposes
            val remainingRoots = bones.indices.filter { bones[it].parentBoneIndex == null || bones[it].parentBoneIndex == -1 }
            if (remainingRoots.isNotEmpty()) {
                val rootNames = remainingRoots.take(10).joinToString { bones[it].nameLocal }
                val more = if (remainingRoots.size > 10) "... and ${remainingRoots.size - 10} more" else ""
                logger.info("[HIERARCHY-REPAIR] Repair complete. ${remainingRoots.size} bones remain as roots: $rootNames$more")
            } else {
                logger.info("[HIERARCHY-REPAIR] Repair complete. Zero root bones remain.")
            }
        }

        private fun loadMorphTargets(buffer: ByteBuffer) {
            val morphTargetCount = buffer.getInt()
            if (morphTargetCount < 0) {
                throw PmxLoadException("Bad PMX model: morph targets count less than zero")
            }

            val targets = mutableListOf<PmxMorph>()
            val morphGroups = mutableListOf<PmxMorphGroup>()
            for (index in 0 until morphTargetCount) {
                val nameLocal = loadString(buffer)
                val nameUniversal = loadString(buffer)
                val expressionTag =
                    Expression.Tag.fromPmxJapanese(nameLocal) ?: Expression.Tag.fromPmxEnglish(nameUniversal)
                val panelType = buffer.get().toInt()
                    .let { type -> PmxMorphPanelType.entries.firstOrNull { it.value == type } }
                    ?: throw PmxLoadException("Unknown panel type")
                val morphType = buffer.get().toInt()
                    .let { type -> PmxMorphType.entries.firstOrNull { it.value == type } }
                    ?: throw PmxLoadException("Unknown morph type")
                val offsetSize = buffer.getInt()
                if (offsetSize < 1) {
                    continue
                }
                when (morphType) {
                    PmxMorphType.VERTEX -> {
                        val dataMap = mutableMapOf<Int, BuildingVertexMorphTarget>()
                        for (i in 0 until offsetSize) {
                            // Get vertex index
                            val vertexIndex = loadVertexIndex(buffer)

                            // Push data into corresponding building morph target
                            val x = buffer.getFloat() * -MMD_SCALE
                            val y = buffer.getFloat() * MMD_SCALE
                            val z = buffer.getFloat() * MMD_SCALE

                            // Lookup each material
                            for (materialIndex in materials.indices) {
                                val material = materials[materialIndex] ?: continue
                                // Map global vertex index to material local
                                val materialLocalIndex = vertexToMaterialMap.getLocalIndex(vertexIndex, materialIndex)
                                if (materialLocalIndex == -1) {
                                    continue
                                }

                                // Fetch building morph target
                                val buildingTarget = dataMap.getOrPut(materialIndex) {
                                    BuildingVertexMorphTarget(material.vertices)
                                }
                                buildingTarget.setVertex(materialLocalIndex, x, y, z)
                            }
                        }
                        targets.add(
                            PmxMorph(
                                pmxIndex = index,
                                targetIndex = targets.size,
                                nameLocal = nameLocal.takeIf(String::isNotBlank),
                                nameUniversal = nameUniversal.takeIf(String::isNotBlank),
                                tag = expressionTag,
                                data = dataMap.mapNotNull { (materialIndex, value) ->
                                    val morphBuffer = value.finish()
                                    val material = materials[materialIndex] ?: return@mapNotNull null
                                    materialIndex to Primitive.Attributes.MorphTarget(
                                        position = Accessor(
                                            name = "Morph #$index material #$materialIndex vertex buffer",
                                            bufferView = BufferView(
                                                buffer = Buffer(
                                                    buffer = morphBuffer,
                                                ),
                                                byteLength = morphBuffer.capacity(),
                                                byteOffset = 0,
                                                byteStride = 12,
                                            ),
                                            componentType = Accessor.ComponentType.FLOAT,
                                            count = material.vertices,
                                            type = Accessor.AccessorType.VEC3,
                                        )
                                    )
                                }.toMap(),
                            )
                        )
                    }

                    PmxMorphType.GROUP -> {
                        morphGroups.add(
                            PmxMorphGroup(
                                nameLocal = nameLocal.takeIf(String::isNotBlank),
                                nameUniversal = nameUniversal.takeIf(String::isNotBlank),
                                tag = expressionTag,
                                items = (0 until offsetSize).map {
                                    MorphItem(
                                        index = loadMorphIndex(buffer),
                                        influence = buffer.getFloat(),
                                    )
                                },
                            )
                        )
                    }

                    PmxMorphType.UV, PmxMorphType.UV_EXT1, PmxMorphType.UV_EXT2, PmxMorphType.UV_EXT3, PmxMorphType.UV_EXT4 -> {
                        // Just skip, not really supported
                        val itemSize = globals.vertexIndexSize + 16
                        buffer.position(buffer.position() + itemSize * offsetSize)
                    }

                    PmxMorphType.BONE -> {
                        // Just skip, not really supported
                        val itemSize = globals.boneIndexSize + 28
                        buffer.position(buffer.position() + itemSize * offsetSize)
                    }

                    PmxMorphType.MATERIAL -> {
                        // Just skip, not really supported
                        val itemSize = globals.materialIndexSize + 113
                        buffer.position(buffer.position() + itemSize * offsetSize)
                    }

                    PmxMorphType.FLIP -> {
                        // Just skip, not really supported
                        val itemSize = globals.morphIndexSize + 4
                        buffer.position(buffer.position() + itemSize * offsetSize)
                    }

                    PmxMorphType.IMPULSE -> {
                        // Just skip, not really supported
                        val itemSize = globals.rigidBodyIndexSize + 25
                        buffer.position(buffer.position() + itemSize * offsetSize)
                    }
                }
            }
            morphTargets = targets
            morphTargetGroups = morphGroups
        }

        private fun loadDisplayFrames(buffer: ByteBuffer) {
            val displayFrameCount = buffer.getInt()
            if (displayFrameCount < 0) {
                throw PmxLoadException("Bad PMX model: display frames count less than zero")
            }
            val displayFrames = mutableListOf<PmxDisplayFrame>()
            repeat(displayFrameCount) {
                val nameLocal = loadString(buffer)
                val nameUniversal = loadString(buffer)
                val isSpecial = buffer.get() != 0.toByte()
                val frameCount = buffer.getInt()
                val frames = (0 until frameCount).map {
                    when (val type = buffer.get()) {
                        0.toByte() -> PmxDisplayFrame.FrameData.Bone(
                            boneIndex = loadBoneIndex(buffer),
                        )

                        1.toByte() -> PmxDisplayFrame.FrameData.Morph(
                            morphIndex = loadMorphIndex(buffer),
                        )

                        else -> throw PmxLoadException("Unknown frame type: $type")
                    }
                }
                displayFrames.add(
                    PmxDisplayFrame(
                        nameLocal = nameLocal,
                        nameUniversal = nameUniversal,
                        isSpecial = isSpecial,
                        frames = frames,
                    )
                )
            }
        }

        private fun loadRigidBodies(buffer: ByteBuffer) {
            val rigidBodyCount = buffer.getInt()
            if (rigidBodyCount < 0) {
                throw PmxLoadException("Bad PMX model: rigid bodies count less than zero")
            }

            fun loadShapeType(byte: Byte): PmxRigidBody.ShapeType = when (byte.toInt()) {
                0 -> PmxRigidBody.ShapeType.SPHERE
                1 -> PmxRigidBody.ShapeType.BOX
                2 -> PmxRigidBody.ShapeType.CAPSULE
                else -> throw PmxLoadException("Unsupported rigid body shape type: $byte")
            }

            fun loadPhysicsMode(byte: Byte): PmxRigidBody.PhysicsMode = when (byte.toInt()) {
                0 -> PmxRigidBody.PhysicsMode.FOLLOW_BONE
                1 -> PmxRigidBody.PhysicsMode.PHYSICS
                2 -> PmxRigidBody.PhysicsMode.PHYSICS_PLUS_BONE
                else -> throw PmxLoadException("Unsupported rigid body physics mode: $byte")
            }

            rigidBodies = (0 until rigidBodyCount).map { index ->
                PmxRigidBody(
                    nameLocal = loadString(buffer),
                    nameUniversal = loadString(buffer),
                    relatedBoneIndex = loadBoneIndex(buffer),
                    groupId = buffer.get().toUByte().toInt(),
                    nonCollisionGroup = buffer.getShort().toUShort().toInt(),
                    shape = loadShapeType(buffer.get()),
                    shapeSize = loadVector3f(buffer).mul(MMD_SCALE),
                    shapePosition = loadVector3f(buffer).transformPosition(),
                    shapeRotation = loadVector3f(buffer).also {
                        it.y *= -1
                        it.z *= -1
                    },
                    mass = buffer.getFloat(),
                    moveAttenuation = buffer.getFloat(),
                    rotationDamping = buffer.getFloat(),
                    repulsion = buffer.getFloat(),
                    frictionForce = buffer.getFloat(),
                    physicsMode = loadPhysicsMode(buffer.get())
                ).also {
                    if (it.relatedBoneIndex in bones.indices) {
                        boneToRigidBodyMap.getOrPut(it.relatedBoneIndex, ::mutableListOf).add(index)
                    } else if (bones.isNotEmpty()) {
                        // Allocate to first bone
                        // https://github.com/benikabocha/saba/blob/29b8efa8b31c8e746f9a88020fb0ad9dcdcf3332/src/Saba/Model/MMD/MMDPhysics.cpp#L434
                        boneToRigidBodyMap.getOrPut(0, ::mutableListOf).add(index)
                    } else {
                        // No bone? Ignore
                    }
                }
            }
        }

        private fun loadJoints(buffer: ByteBuffer) {
            val jointCount = buffer.getInt()
            if (jointCount < 0) {
                throw PmxLoadException("Bad PMX model: joints count less than zero")
            }

            fun loadJointType(byte: Byte): PmxJoint.JointType = PmxJoint.JointType.entries.firstOrNull {
                byte.toInt() == it.value
            } ?: throw PmxLoadException("Unsupported joint type: $byte")

            joints = (0 until jointCount).map {
                val nameLocal = loadString(buffer)
                val nameUniversal = loadString(buffer)
                val type = loadJointType(buffer.get())
                val rigidBodyIndexA = loadRigidBodyIndex(buffer)
                val rigidBodyIndexB = loadRigidBodyIndex(buffer)
                val position = loadVector3f(buffer).transformPosition()
                val rotation = loadVector3f(buffer).also {
                    it.y *= -1
                    it.z *= -1
                }

                val positionMinimumOrig = loadVector3f(buffer).transformPosition()
                val positionMaximumOrig = loadVector3f(buffer).transformPosition()
                val positionMinimum = Vector3f(positionMaximumOrig.x, positionMinimumOrig.y, positionMinimumOrig.z)
                val positionMaximum = Vector3f(positionMinimumOrig.x, positionMaximumOrig.y, positionMaximumOrig.z)

                val rotationMinimumOrig = loadVector3f(buffer)
                val rotationMaximumOrig = loadVector3f(buffer)
                val rotationMinimum = Vector3f(
                    rotationMinimumOrig.x,
                    -rotationMaximumOrig.y,
                    -rotationMaximumOrig.z,
                )
                val rotationMaximum = Vector3f(
                    rotationMaximumOrig.x,
                    -rotationMinimumOrig.y,
                    -rotationMinimumOrig.z,
                )
                val positionSpring = loadVector3f(buffer).div(MMD_SCALE)
                val rotationSpring = loadVector3f(buffer)

                PmxJoint(
                    nameLocal = nameLocal,
                    nameUniversal = nameUniversal,
                    type = type,
                    rigidBodyIndexA = rigidBodyIndexA,
                    rigidBodyIndexB = rigidBodyIndexB,
                    position = position,
                    rotation = rotation,
                    positionMinimum = positionMinimum,
                    positionMaximum = positionMaximum,
                    rotationMinimum = rotationMinimum,
                    rotationMaximum = rotationMaximum,
                    positionSpring = positionSpring,
                    rotationSpring = rotationSpring,
                )
            }
        }

        private data class MaterialMorphData(
            val materialIndex: Int,
            val morphIndex: Int,
        )

        fun load(buffer: ByteBuffer): LoadResult {
            val header = loadHeader(buffer)
            loadVertices(buffer)
            loadSurfaces(buffer)
            loadTextures(buffer)
            loadMaterials(buffer)
            loadBones(buffer)
            repairKoikatsuHierarchy()
            loadMorphTargets(buffer)
            loadDisplayFrames(buffer)
            loadRigidBodies(buffer)
            loadJoints(buffer)

            val modelId = UUID.randomUUID()
            val rootNodes = mutableListOf<Node>()

            fun addBone(index: Int, parentPosition: Vector3fc? = null, depth: Int = 0): Node {
                val bone = bones[index]
                val boneNodeId = NodeId(modelId, index)

                val children = childBoneMap[index]?.map {
                    addBone(it, bone.position, depth + 1)
                } ?: listOf()

                val components = buildList {
                    effectorToIkDataMap[index]?.forEach { data ->
                        add(
                            NodeComponent.IkTargetComponent(
                                ikTarget = IkTarget(
                                    limitRadian = data.limitRadian,
                                    loopCount = data.loopCount,
                                    joints = data.links.map { link ->
                                        IkTarget.IkJoint(
                                            nodeId = NodeId(modelId, link.index),
                                            limit = link.limits?.let {
                                                IkTarget.IkJoint.Limits(
                                                    min = Vector3f(
                                                        it.limitMin.x(),
                                                        -it.limitMax.y(),
                                                        -it.limitMax.z(),
                                                    ),
                                                    max = Vector3f(
                                                        it.limitMax.x(),
                                                        -it.limitMin.y(),
                                                        -it.limitMin.z(),
                                                    ),
                                                )
                                            }
                                        )
                                    },
                                    effectorNodeId = NodeId(modelId, data.effectorIndex),
                                ),
                                transformId = TransformId.IK,
                            )
                        )
                    }
                    sourceToInheritMap[index]?.forEach { data ->
                        add(
                            NodeComponent.InfluenceSourceComponent(
                                influence = Influence(
                                    target = NodeId(modelId, data.targetIndex),
                                    influence = data.influence,
                                    influenceRotation = data.inheritRotation,
                                    influenceTranslation = data.inheritTranslation,
                                    appendLocal = data.inheritLocal,
                                ),
                                transformId = TransformId.INFLUENCE,
                            )
                        )
                    }

                    val garmentGroupMask = rigidBodies.filter {
                        val n = it.nameLocal.lowercase()
                        n.contains("vest") || n.contains("shirt") || n.contains("coat") ||
                        n.contains("jacket") || n.contains("suit") || n.contains("dress") ||
                        n.contains("inner") || n.contains("outer") || n.contains("服") || n.contains("衣")
                    }.fold(0) { acc, rb -> acc or (1 shl rb.groupId) }

                    boneToRigidBodyMap[index]?.forEach { index ->
                        val rigidBody = rigidBodies[index]
                        val name = rigidBody.nameLocal.lowercase()
                        val isBreast = name.contains("乳") || name.contains("胸") || name.contains("bust") || name.contains("breast")
                        val isHair = name.contains("hair") || name.contains("发") || name.contains("髪") || name.contains("bang") || name.contains("strand") || name.contains("front") || name.contains("back") || name.contains("ahoge") || name.contains("side") || name.contains("tail")
                        val isSkirt = name.contains("skirt") || name.contains("スカート") || name.contains("ribbon")

                        // Task 2: Refine physics loading logic.
                        // Instead of skipping, we allow the component creation but force FOLLOW_BONE mode.
                        // This ensures the mesh has its associated physics metadata but doesn't fly away.
                        
                        add(
                            NodeComponent.RigidBodyComponent(
                                rigidBodyId = RigidBodyId(modelId, index),
                                rigidBody = rigidBodies[index].let { rigidBody ->
                                    val enableNameBasedOverrides = isKoikatsu
                                    val basePhysicsMode = when (rigidBody.physicsMode) {
                                        PmxRigidBody.PhysicsMode.FOLLOW_BONE -> RigidBody.PhysicsMode.FOLLOW_BONE
                                        PmxRigidBody.PhysicsMode.PHYSICS -> RigidBody.PhysicsMode.PHYSICS
                                        PmxRigidBody.PhysicsMode.PHYSICS_PLUS_BONE -> RigidBody.PhysicsMode.PHYSICS_PLUS_BONE
                                    }

                                    val nameLocal = rigidBody.nameLocal.lowercase()

                                    val adjustedPhysicsMode = if (enableNameBasedOverrides) {
                                        when {
                                            isBreast || isHair || isSkirt ->
                                                RigidBody.PhysicsMode.FOLLOW_BONE
                                            nameLocal.contains("skirt") || nameLocal.contains("スカート") || nameLocal.contains("腰") ->
                                                RigidBody.PhysicsMode.FOLLOW_BONE
                                            nameLocal.contains("hair") || nameLocal.contains("髪") || nameLocal.contains("ribbon") || nameLocal.contains("ct_") ->
                                                RigidBody.PhysicsMode.FOLLOW_BONE
                                            nameLocal.contains("bust") || nameLocal.contains("breast") || nameLocal.contains("胸") || nameLocal.contains("乳") ->
                                                RigidBody.PhysicsMode.FOLLOW_BONE
                                            nameLocal.startsWith("cf_j_", ignoreCase = true) ||
                                            nameLocal.startsWith("cf_s_", ignoreCase = true) ||
                                            nameLocal.startsWith("cf_d_", ignoreCase = true) ||
                                            nameLocal.startsWith("cf_m_", ignoreCase = true) ->
                                                RigidBody.PhysicsMode.FOLLOW_BONE
                                            else -> basePhysicsMode
                                        }
                                    } else {
                                        basePhysicsMode
                                    }

                                     val baseGroup = 1 shl rigidBody.groupId
                                     val baseCollisionMask = (rigidBody.nonCollisionGroup.inv() and 0xFFFF).toInt()
                                     
                                     // Identify all body-related rigid bodies to exclude from collision
                                     val bodyGroupMask = rigidBodies.indices
                                        .filter { i -> 
                                            val n = rigidBodies[i].nameLocal.lowercase()
                                            n.contains("body") || n.contains("体") || n.contains("腕") || n.contains("arm") || n.contains("肉") ||
                                            n.contains("leg") || n.contains("足") || n.contains("hand") || n.contains("手") ||
                                            n.contains("lower") || n.contains("upper") || n.contains("身") || n.contains("頭") || n.contains("首") ||
                                            n.contains("膝") || n.contains("肘") || n.contains("肩") || n.contains("ひざ") || n.contains("しり") ||
                                            n.contains("おしり") || n.contains("腰") || n.contains("センター") || n.contains("中心")
                                        }
                                        .fold(0) { acc, i -> acc or (1 shl rigidBodies[i].groupId) }
                                     
                                     val isBodyPart = (bodyGroupMask and (1 shl rigidBody.groupId)) != 0 &&
                                                      !name.contains("頭") && !name.contains("head") &&
                                                      !name.contains("首") && !name.contains("neck") &&
                                                      !name.contains("顔") && !name.contains("face")

                                     val collisionMask = when {
                                         isHair && depth <= 1 -> {
                                             baseCollisionMask and bodyGroupMask.inv()
                                         }
                                         isBreast -> baseCollisionMask and bodyGroupMask.inv()
                                         isHair -> baseCollisionMask or bodyGroupMask
                                         isSkirt -> baseCollisionMask
                                         else -> baseCollisionMask
                                     }
                                     val isPhysicsEnabled = adjustedPhysicsMode != RigidBody.PhysicsMode.FOLLOW_BONE
                                     
                                    // Identify fast-moving or thin bodies that need CCD (Continuous Collision Detection)
                                    val needsCCD = isPhysicsEnabled && (isBreast || isHair || isSkirt)

                                    val threshold = if (needsCCD) {
                                        // Set threshold based on the smallest dimension to catch tunneling
                                        val minSize = listOf(rigidBody.shapeSize.x, rigidBody.shapeSize.y, rigidBody.shapeSize.z).minOrNull() ?: 1f
                                        minSize * 0.5f 
                                    } else 0f

                                    val sweptRadius = if (needsCCD) {
                                        val minSize = listOf(rigidBody.shapeSize.x, rigidBody.shapeSize.y, rigidBody.shapeSize.z).minOrNull() ?: 1f
                                        minSize * 0.2f
                                    } else 0f
                                    
                                    val safetyDamping = if (isPhysicsEnabled) 0.2f else 0f
                                    val finalMoveAttenuation = rigidBody.moveAttenuation.coerceAtLeast(safetyDamping)
                                    val finalRotationDamping = rigidBody.rotationDamping.coerceAtLeast(safetyDamping)
                                    val finalPhysicsMode = if (isSkirt && adjustedPhysicsMode == RigidBody.PhysicsMode.PHYSICS) {
                                        RigidBody.PhysicsMode.PHYSICS_PLUS_BONE
                                    } else {
                                        adjustedPhysicsMode
                                    }

                                    RigidBody(
                                        name = rigidBody.nameLocal.takeIf(String::isNotBlank),
                                        collisionGroup = baseGroup,
                                        collisionMask = collisionMask,
                                        shape = when (rigidBody.shape) {
                                            PmxRigidBody.ShapeType.SPHERE -> RigidBody.ShapeType.SPHERE
                                            PmxRigidBody.ShapeType.BOX -> RigidBody.ShapeType.BOX
                                            PmxRigidBody.ShapeType.CAPSULE -> RigidBody.ShapeType.CAPSULE
                                        },
                                         shapeSize = rigidBody.shapeSize,
                                        shapePosition = rigidBody.shapePosition,
                                        shapeRotation = rigidBody.shapeRotation,
                                        mass = if (isHair) {
                                            // Mass Gradient: Heavy at root, feather-light at tips (Expert setting)
                                            (rigidBody.mass * (1.0f / (depth + 1))).coerceAtLeast(0.05f)
                                        } else if (isBreast) {
                                            rigidBody.mass // Increased mass for better inertia and less shaking
                                        } else {
                                            rigidBody.mass
                                        },
                                        moveAttenuation = finalMoveAttenuation,
                                        rotationDamping = if (isHair) {
                                            0.1f
                                        } else if (isBreast) {
                                            0.95f
                                        } else finalRotationDamping,
                                        repulsion = 0.0f, 
                                        frictionForce = if (isHair) 0.1f else rigidBody.frictionForce, 
                                        physicsMode = finalPhysicsMode,
                                        ccdMotionThreshold = threshold,
                                        ccdSweptSphereRadius = sweptRadius,
                                         collisionMargin = if (isHair || isBreast || isSkirt) 0.03f else 0.04f
                                    )
                                },
                            )
                        )
                    }
                }

                return Node(
                    name = bone.nameLocal,
                    id = boneNodeId,
                    transform = NodeTransform.Decomposed(
                        translation = Vector3f().set(bone.position).also {
                            if (parentPosition != null) {
                                it.sub(parentPosition)
                            }
                        },
                        rotation = Quaternionf(),
                        scale = Vector3f(1f),
                    ),
                    children = children,
                    components = components,
                )
            }

            rootBones.forEach { index ->
                rootNodes.add(addBone(index))
            }

            var nextNodeIndex = bones.size

            val joints = mutableListOf<NodeId>()
            val inverseBindMatrices = mutableListOf<Matrix4f>()
            val jointHumanoidTags = mutableListOf<HumanoidTag?>()

            for (boneIndex in bones.indices) {
                val bone = bones[boneIndex]
                val nodeId = NodeId(modelId, boneIndex)
                joints.add(nodeId)

                val inverseBindMatrix = Matrix4f().translation(bone.position).invertAffine()
                inverseBindMatrices.add(inverseBindMatrix)

                jointHumanoidTags.add(
                    HumanoidTag.fromPmxJapanese(bone.nameLocal)
                        ?: HumanoidTag.fromPmxEnglish(bone.nameUniversal)
                )
            }

            val skin = Skin(
                name = "PMX skin",
                joints = joints,
                inverseBindMatrices = inverseBindMatrices,
                jointHumanoidTags = jointHumanoidTags,
            )

            val pmxMorphToMaterialMorphIndexMap = mutableMapOf<Int, MutableList<MaterialMorphData>>()
            val materialMorphMap = mutableMapOf<Int, MutableList<Primitive.Attributes.MorphTarget>>()
            for ((morphIndex, pmxTarget) in morphTargets.withIndex()) {
                val materialMorphIndexList = pmxMorphToMaterialMorphIndexMap.getOrPut(morphIndex, ::mutableListOf)
                for ((materialIndex, target) in pmxTarget.data) {
                    val materialMorphList = materialMorphMap.getOrPut(materialIndex, ::mutableListOf)
                    val materialMorphIndex = materialMorphList.size
                    materialMorphList.add(target)
                    materialMorphIndexList.add(MaterialMorphData(materialIndex, materialMorphIndex))
                }
            }

            val materialToMeshIds = mutableMapOf<Int, MeshId>()
            materials.forEachIndexed { materialIndex, materialData ->
                val nodeIndex = nextNodeIndex++
                val nodeId = NodeId(modelId, nodeIndex)
                val meshId = MeshId(modelId, nodeIndex)
                materialToMeshIds[materialIndex] = meshId

                val pmxMaterial = materialData?.material ?: return@forEachIndexed
                val material = Material.Unlit(
                    name = pmxMaterial.nameLocal,
                    baseColor = pmxMaterial.diffuseColor,
                    baseColorTexture = pmxMaterial.textureIndex.takeIf {
                        it >= 0 && it in textures.indices
                    }?.let {
                        textures.getOrNull(it)
                    }?.let {
                        Material.TextureInfo(it)
                    },
                    doubleSided = pmxMaterial.drawingFlags.noCull,
                )

                rootNodes.add(
                    Node(
                        name = "Node for material ${pmxMaterial.nameLocal}",
                        id = nodeId,
                        transform = null,
                        components = buildList {
                            add(
                                NodeComponent.MeshComponent(
                                    mesh = Mesh(
                                        id = meshId,
                                        primitives = listOf(
                                            Primitive(
                                                mode = Primitive.Mode.TRIANGLES,
                                                material = material,
                                                attributes = materialData.vertexAttributes,
                                                indices = Accessor(
                                                    bufferView = materialData.indexBufferView,
                                                    componentType = indexBufferType,
                                                    normalized = false,
                                                    count = pmxMaterial.surfaceCount,
                                                    type = Accessor.AccessorType.SCALAR,
                                                ),
                                                targets = materialMorphMap[materialIndex] ?: listOf(),
                                            )
                                        ),
                                        weights = null,
                                    )
                                )
                            )
                            add(
                                NodeComponent.SkinComponent(
                                    skin = skin,
                                    meshIds = listOf(meshId),
                                )
                            )
                        }
                    )
                )
            }

            val cameraNodeIndex = nextNodeIndex++
            rootNodes.add(
                Node(
                    name = "MMD Camera",
                    id = NodeId(modelId, cameraNodeIndex),
                    components = listOf(
                        NodeComponent.CameraComponent(
                            Camera.MMD(name = "MMD Camera")
                        )
                    )
                )
            )

            val scene = Scene(nodes = rootNodes)

            val pmxIndexToExpressions = mutableMapOf<Int, Expression.Target>()
            return LoadResult(
                metadata = Metadata(
                    title = header.modelNameLocal,
                    titleUniversal = header.modelNameUniversal,
                    comment = header.commentLocal,
                    commentUniversal = header.commentUniversal,
                ),
                model = Model(
                    scenes = listOf(scene),
                    skins = listOf(skin),
                    physicalJoints = this.joints.mapNotNull { joint ->
                        if (joint.rigidBodyIndexA !in rigidBodies.indices) {
                            return@mapNotNull null
                        }
                        if (joint.rigidBodyIndexB !in rigidBodies.indices) {
                            return@mapNotNull null
                        }
                        val rbA = rigidBodies[joint.rigidBodyIndexA]
                        val rbB = rigidBodies[joint.rigidBodyIndexB]
                        val nameCombo = (rbA.nameLocal + rbB.nameLocal).lowercase()
                        val isBreastJoint = nameCombo.let { n ->
                            n.contains("乳") || n.contains("胸") || n.contains("bust") || n.contains("breast") || n.contains("cf_s_bust")
                        }
                        val isHairJoint = nameCombo.let { n ->
                            n.contains("hair") || n.contains("发") || n.contains("髪") || n.contains("bang") || n.contains("strand") || n.contains("front") || n.contains("back") || n.contains("side") || n.contains("tail") || n.contains("ahoge") || n.contains("ct_") || n.contains("ribbon")
                        }
                        val isSkirtJoint = nameCombo.let { n ->
                            n.contains("skirt") || n.contains("スカート") || n.contains("cf_s_skirt") || n.contains("cf_j_sk_") || n.contains("cf_d_sk_") || n.contains("腰")
                        }
                        val isHelperJoint = nameCombo.let { n ->
                            n.contains("cf_m_") || n.contains("cf_t_") || n.contains("捩") || n.contains("肩")
                        }
                        
                        if (isKoikatsu && (isBreastJoint || isHairJoint || isSkirtJoint || isHelperJoint)) {
                            return@mapNotNull null
                        }

                        PhysicalJoint(
                            name = joint.nameLocal.takeIf(String::isNotBlank),
                            type = when (joint.type) {
                                PmxJoint.JointType.SPRING_6DOF -> PhysicalJoint.JointType.SPRING_6DOF
                            },
                            rigidBodyA = RigidBodyId(modelId, joint.rigidBodyIndexA),
                            rigidBodyB = RigidBodyId(modelId, joint.rigidBodyIndexB),
                            position = joint.position,
                            rotation = joint.rotation,
                            positionMin = if (isBreastJoint) Vector3f(0f, 0f, 0f) else joint.positionMinimum,
                            positionMax = if (isBreastJoint) Vector3f(0f, 0f, 0f) else joint.positionMaximum,
                            rotationMin = if (isBreastJoint) {
                                // Extremely tight rotation - almost completely static
                                Vector3f(-0.017f, -0.017f, -0.017f) // ~1 degree
                            } else joint.rotationMinimum,
                            rotationMax = if (isBreastJoint) {
                                Vector3f(0.017f, 0.017f, 0.017f)
                            } else joint.rotationMaximum,
                            positionSpring = joint.positionSpring,
                            rotationSpring = joint.rotationSpring, // Restore springs so hair holds its sculpted curve instead of acting like a heavy chain
                            softness = when {
                                isBreastJoint -> 0.5f // Soft repositioning
                                (rbA.nameLocal + rbB.nameLocal).lowercase().contains("hair") -> 0.1f // Natural spring bounce
                                else -> 1.0f
                            },
                            biasFactor = when {
                                isBreastJoint -> 0.4f // Stiff error correction for Spring2 (was 0.1)
                                (rbA.nameLocal + rbB.nameLocal).lowercase().contains("hair") -> 0.1f // Stop violent solver "flicks"
                                else -> 0.3f
                            },
                            relaxationFactor = 1.0f,
                        )
                    },
                    expressions = buildList {
                        for ((index, target) in morphTargets.withIndex()) {
                            val expression = Expression.Target(
                                name = target.nameLocal ?: target.nameUniversal,
                                tag = target.tag,
                                isBinary = false,
                                bindings = pmxMorphToMaterialMorphIndexMap[index]?.mapNotNull { (materialIndex, targetIndex) ->
                                    Expression.Target.Binding.MeshMorphTarget(
                                        meshId = materialToMeshIds[materialIndex] ?: return@mapNotNull null,
                                        index = targetIndex,
                                        weight = 0f,
                                    )
                                } ?: listOf(),
                            )
                            pmxIndexToExpressions[target.pmxIndex] = expression
                            add(expression)
                        }
                        for (group in morphTargetGroups) {
                            add(
                                Expression.Group(
                                    name = group.nameLocal ?: group.nameUniversal,
                                    tag = group.tag,
                                    targets = group.items.mapNotNull { item ->
                                        val pmxMorphIndex = item.index
                                        val target = pmxIndexToExpressions[pmxMorphIndex] ?: return@mapNotNull null
                                        Expression.Group.TargetItem(
                                            target = target,
                                            influence = item.influence,
                                        )
                                    }
                                )
                            )
                        }
                    },
                    defaultScene = scene,
                ),
                animations = listOf(),
            )
        }
    }

    override fun load(path: Path, context: LoadContext, param: LoadParam) =
        FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val fileSize = channel.size()
            val buffer = runCatching {
                channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize)
            }.getOrNull() ?: run {
                if (fileSize > 32 * 1024 * 1024) {
                    throw PmxLoadException("PMX model size too large: maximum allowed is 32M, current is $fileSize")
                }
                val fileSize = fileSize.toInt()
                val buffer = ByteBuffer.allocate(fileSize)
                channel.readAll(buffer)
                buffer.flip()
                buffer
            }
            val context = Context(context, param)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            context.load(buffer)
        }
}