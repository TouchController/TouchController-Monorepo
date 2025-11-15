package top.fifthlight.blazerod.model.gltf.format

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.animation.AnimationInterpolation
import top.fifthlight.blazerod.model.gltf.format.extension.VrmV0Extension
import top.fifthlight.blazerod.model.gltf.format.extension.VrmV1Extension
import top.fifthlight.blazerod.model.Texture as CommonTexture

private abstract class IntEnumSerializer<T>(
    serialName: String,
    vararg mappings: Pair<T, Int>,
) : KSerializer<T> {
    private val map = mapOf(*mappings)
    private val reverseMap = mappings.associate { (a, b) -> Pair(b, a) }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeInt()
        return reverseMap[value] ?: throw SerializationException("Invalid value: $value")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(map[value] ?: throw SerializationException("No value for $value"))
    }
}

private abstract class StringEnumSerializer<T>(
    serialName: String,
    vararg mappings: Pair<T, String>,
) : KSerializer<T> {
    private val map = mapOf(*mappings)
    private val reverseMap = mappings.associate { (a, b) -> Pair(b, a) }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeString()
        return reverseMap[value] ?: throw SerializationException("Invalid value: $value")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(map[value] ?: throw SerializationException("No value for $value"))
    }
}

@Serializable
internal data class Gltf(
    val extensionsUsed: List<String>? = null,
    val extensionsRequired: List<String>? = null,
    val accessors: List<GltfAccessor>? = null,
    val animations: List<GltfAnimation>? = null,
    val asset: GltfAsset,
    val buffers: List<GltfBuffer>? = null,
    val bufferViews: List<GltfBufferView>? = null,
    val cameras: List<GltfCamera>? = null,
    val images: List<GltfImage>? = null,
    val materials: List<GltfMaterial>? = null,
    val meshes: List<GltfMesh>? = null,
    val nodes: List<GltfNode>? = null,
    val samplers: List<GltfTextureSampler>? = null,
    val scene: Int? = null,
    val scenes: List<GltfScene>? = null,
    val skins: List<GltfSkin>? = null,
    val textures: List<GltfTexture>? = null,
    val extensions: GltfExtension? = null,
)

@Serializable
internal data class GltfExtension(
    @SerialName("VRM")
    val vrmV0: VrmV0Extension? = null,
    @SerialName("VRMC_vrm")
    val vrmV1: VrmV1Extension? = null,
)

@Serializable
internal data class GltfAsset(
    val version: String,
    val minVersion: String? = null,
    val generator: String? = null,
    val copyright: String? = null,
)

@Serializable
internal data class GltfBuffer(
    val byteLength: Int,
    val uri: String? = null
) {
    init {
        require(byteLength > 0) { "Buffer's length less than 0: $byteLength" }
    }
}

@Serializable
internal data class GltfBufferView(
    val buffer: Int,
    val byteLength: Int,
    val byteOffset: Int? = null,
    val byteStride: Int? = null,
    @Serializable(with = BufferViewTargetSerializer::class)
    val target: BufferView.Target? = null,
) {
    init {
        require(byteLength > 0) { "Buffer's length less than 0: $byteLength" }
    }

    private class BufferViewTargetSerializer : IntEnumSerializer<BufferView.Target>(
        "top.fifthlight.blazerod.model.BufferView.Target",
        BufferView.Target.ARRAY_BUFFER to 34962,
        BufferView.Target.ELEMENT_ARRAY_BUFFER to 34963,
    )
}

@Serializable
internal data class GltfAccessor(
    val bufferView: Int? = null,
    val byteOffset: Int = 0,
    @Serializable(with = ComponentTypeSerializer::class)
    val componentType: Accessor.ComponentType,
    val normalized: Boolean = false,
    val count: Int,
    val type: Accessor.AccessorType,
    val max: List<Float>? = null,
    val min: List<Float>? = null,
    val sparse: GltfAccessorSparse? = null,
    val name: String? = null,
) {
    init {
        require(count > 0) { "Invalid accessor count: $count" }
    }

    private class ComponentTypeSerializer : IntEnumSerializer<Accessor.ComponentType>(
        "top.fifthlight.blazerod.model.Accessor.ComponentType",
        Accessor.ComponentType.BYTE to 5120,
        Accessor.ComponentType.UNSIGNED_BYTE to 5121,
        Accessor.ComponentType.SHORT to 5122,
        Accessor.ComponentType.UNSIGNED_SHORT to 5123,
        Accessor.ComponentType.UNSIGNED_INT to 5125,
        Accessor.ComponentType.FLOAT to 5126,
    )
}

@Serializable
internal data class GltfAccessorSparse(
    val count: Int,
    val indices: GltfSparseIndices,
    val values: GltfSparseValues,
)

@Serializable
internal data class GltfSparseIndices(
    val bufferView: Int,
    val byteOffset: Int = 0,
    val componentType: Int
)

@Serializable
internal data class GltfSparseValues(
    val bufferView: Int,
    val byteOffset: Int = 0
)

@Serializable
internal data class GltfAnimation(
    val name: String? = null,
    val channels: List<GltfAnimationChannel>,
    val samplers: List<GltfAnimationSampler>,
)

@Serializable
internal data class GltfAnimationChannel(
    val sampler: Int,
    val target: GltfAnimationTarget,
)

@Serializable
internal data class GltfAnimationTarget(
    val node: Int? = null,
    @Serializable(with = PathSerializer::class)
    val path: Path,
) {
    data class ComponentTypeItem(
        val type: Accessor.ComponentType,
        val normalized: Boolean,
    )

    enum class Path(
        val allowedAccessorTypes: Set<Accessor.AccessorType>,
        val allowedComponentType: Set<ComponentTypeItem>,
    ) {
        TRANSLATION(
            allowedAccessorTypes = setOf(Accessor.AccessorType.VEC3),
            allowedComponentType = setOf(ComponentTypeItem(Accessor.ComponentType.FLOAT, false)),
        ),
        ROTATION(
            allowedAccessorTypes = setOf(Accessor.AccessorType.VEC4),
            allowedComponentType = setOf(
                ComponentTypeItem(Accessor.ComponentType.FLOAT, false),
                ComponentTypeItem(Accessor.ComponentType.BYTE, true),
                ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, true),
                ComponentTypeItem(Accessor.ComponentType.SHORT, true),
                ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, true),
            ),
        ),
        SCALE(
            allowedAccessorTypes = setOf(Accessor.AccessorType.VEC3),
            allowedComponentType = setOf(ComponentTypeItem(Accessor.ComponentType.FLOAT, false)),
        ),
        WEIGHTS(
            allowedAccessorTypes = setOf(Accessor.AccessorType.SCALAR),
            allowedComponentType = setOf(
                ComponentTypeItem(Accessor.ComponentType.FLOAT, false),
                ComponentTypeItem(Accessor.ComponentType.BYTE, true),
                ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, true),
                ComponentTypeItem(Accessor.ComponentType.SHORT, true),
                ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, true),
            ),
        );

        fun check(accessor: Accessor) {
            require(accessor.type in allowedAccessorTypes) {
                "Bad accessor type for animation target"
            }
            require(ComponentTypeItem(accessor.componentType, accessor.normalized) in allowedComponentType) {
                "Bad component type for animation target"
            }
        }
    }

    private class PathSerializer : StringEnumSerializer<Path>(
        "top.fifthlight.blazerod.model.gltf.format.GltfAnimationTarget.Path",
        Path.TRANSLATION to "translation",
        Path.ROTATION to "rotation",
        Path.WEIGHTS to "weights",
        Path.SCALE to "scale",
    )
}

@Serializable
internal data class GltfAnimationSampler(
    val input: Int,
    val output: Int,
    @Serializable(with = AnimationInterpolationSerializer::class)
    val interpolation: AnimationInterpolation,
) {
    private class AnimationInterpolationSerializer : StringEnumSerializer<AnimationInterpolation>(
        "top.fifthlight.blazerod.model.AnimationInterpolation",
        AnimationInterpolation.linear to "LINEAR",
        AnimationInterpolation.step to "STEP",
        AnimationInterpolation.cubicSpline to "CUBICSPLINE",
    )
}

@Serializable
internal data class GltfCamera(
    val type: GltfCameraType,
    val perspective: Perspective? = null,
    val orthographic: Orthographic? = null
) {
    init {
        require(
            (type == GltfCameraType.PERSPECTIVE && perspective != null) ||
                    (type == GltfCameraType.ORTHOGRAPHIC && orthographic != null)
        ) {
            "Camera type and properties mismatch"
        }
    }

    @Serializable
    internal data class Perspective(
        val aspectRatio: Float? = null,
        val yfov: Float,
        val zfar: Float? = null,
        val znear: Float
    ) {
        init {
            require(yfov > 0) { "Invalid yfov: $yfov" }
            require(znear > 0) { "Invalid znear: $znear" }
        }
    }

    @Serializable
    internal data class Orthographic(
        val xmag: Float,
        val ymag: Float,
        val zfar: Float,
        val znear: Float
    ) {
        init {
            require(zfar > znear) { "zfar must be greater than znear" }
        }
    }
}

@Serializable
internal enum class GltfCameraType {
    @SerialName("perspective")
    PERSPECTIVE,

    @SerialName("orthographic")
    ORTHOGRAPHIC,
}

@Serializable
internal data class GltfImage(
    val uri: String? = null,
    val mimeType: String? = null,
    val bufferView: Int? = null,
)

@Serializable
internal data class GltfMaterial(
    val name: String? = null,
    val pbrMetallicRoughness: GltfPbrMetallicRoughness = GltfPbrMetallicRoughness.default,
    val normalTexture: GltfNormalTextureInfo? = null,
    // TODO occlusionTexture
    val emissiveTexture: GltfTextureInfo? = null,
    @Serializable(with = Vector3fSerializer::class)
    val emissiveFactor: Vector3f = Vector3f(0f),
    val alphaMode: Material.AlphaMode = Material.AlphaMode.OPAQUE,
    val alphaCutoff: Float = 0.5f,
    val doubleSided: Boolean = false,
    val extensions: Extension = Extension.empty,
) {
    @Serializable
    internal data class Extension(
        @SerialName("KHR_materials_unlit")
        val unlit: Unlit? = null,
    ) {
        @Serializable
        internal data object Unlit

        companion object {
            internal val empty = Extension()
        }
    }
}

@Serializable
internal data class GltfPbrMetallicRoughness(
    @Serializable(with = RgbaColorSerializer::class)
    val baseColorFactor: RgbaColor = RgbaColor(1f, 1f, 1f, 1f),
    val baseColorTexture: GltfTextureInfo? = null,
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    val metallicRoughnessTexture: GltfTextureInfo? = null,
) {
    companion object {
        val default = GltfPbrMetallicRoughness()
    }
}

@Serializable
internal data class GltfTextureInfo(
    val index: Int,
    val texCoord: Int = 0
)

@Serializable
internal data class GltfNormalTextureInfo(
    val index: Int,
    val texCoord: Int = 0,
    val scale: Float = 1f
)

@Serializable
internal data class GltfTextureSampler(
    @Serializable(with = MagFilterSerializer::class)
    val magFilter: CommonTexture.Sampler.MagFilter = CommonTexture.Sampler.MagFilter.LINEAR,
    @Serializable(with = MinFilterSerializer::class)
    val minFilter: CommonTexture.Sampler.MinFilter = CommonTexture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST,
    @Serializable(with = WrapModeSerializer::class)
    val wrapS: CommonTexture.Sampler.WrapMode = CommonTexture.Sampler.WrapMode.REPEAT,
    @Serializable(with = WrapModeSerializer::class)
    val wrapT: CommonTexture.Sampler.WrapMode = CommonTexture.Sampler.WrapMode.REPEAT,
) {
    private class MagFilterSerializer : IntEnumSerializer<CommonTexture.Sampler.MagFilter>(
        "top.fifthlight.blazerod.model.Texture.Sampler.MagFilter",
        CommonTexture.Sampler.MagFilter.NEAREST to 9728,
        CommonTexture.Sampler.MagFilter.LINEAR to 9729,
    )

    private class MinFilterSerializer : IntEnumSerializer<CommonTexture.Sampler.MinFilter>(
        "top.fifthlight.blazerod.model.Texture.Sampler.MinFilter",
        CommonTexture.Sampler.MinFilter.NEAREST to 9728,
        CommonTexture.Sampler.MinFilter.LINEAR to 9729,
        CommonTexture.Sampler.MinFilter.NEAREST_MIPMAP_NEAREST to 9984,
        CommonTexture.Sampler.MinFilter.LINEAR_MIPMAP_NEAREST to 9985,
        CommonTexture.Sampler.MinFilter.NEAREST_MIPMAP_LINEAR to 9986,
        CommonTexture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR to 9987,
    )

    private class WrapModeSerializer : IntEnumSerializer<CommonTexture.Sampler.WrapMode>(
        "top.fifthlight.blazerod.model.Texture.Sampler.WrapMode",
        CommonTexture.Sampler.WrapMode.CLAMP_TO_EDGE to 33071,
        CommonTexture.Sampler.WrapMode.MIRRORED_REPEAT to 33648,
        CommonTexture.Sampler.WrapMode.REPEAT to 10497,
    )
}

@Serializable
internal data class GltfSkin(
    val name: String? = null,
    val inverseBindMatrices: Int? = null,
    val skeleton: Int? = null,
    val joints: List<Int>,
) {
    init {
        require(joints.isNotEmpty()) { "Skin must have at least one joint" }
    }
}

@Serializable
internal data class GltfTexture(
    val name: String? = null,
    val sampler: Int? = null,
    val source: Int? = null
)

@Serializable(with = GltfAttributeKey.Serializer::class)
internal sealed class GltfAttributeKey {
    data object Position : GltfAttributeKey()
    data object Normal : GltfAttributeKey()
    data object Tangent : GltfAttributeKey()
    data class TexCoord(val index: Int) : GltfAttributeKey()
    data class Color(val index: Int) : GltfAttributeKey()
    data class Joints(val index: Int) : GltfAttributeKey()
    data class Weights(val index: Int) : GltfAttributeKey()
    data class Unknown(val name: String) : GltfAttributeKey()

    internal class Serializer : KSerializer<GltfAttributeKey> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            serialName = "top.fifthlight.blazerod.model.gltf.format.AttributeKey",
            kind = PrimitiveKind.STRING
        )

        override fun deserialize(decoder: Decoder): GltfAttributeKey {
            val name = decoder.decodeString()
            fun Int.toValidIndex() = takeIf { it >= 0 } ?: throw SerializationException("Bad index: $this")
            return when {
                name == "POSITION" -> Position
                name == "NORMAL" -> Normal
                name == "TANGENT" -> Tangent
                name.startsWith("TEXCOORD_") -> TexCoord(name.removePrefix("TEXCOORD_").toInt().toValidIndex())
                name.startsWith("COLOR_") -> Color(name.removePrefix("COLOR_").toInt().toValidIndex())
                name.startsWith("JOINTS_") -> Joints(name.removePrefix("JOINTS_").toInt().toValidIndex())
                name.startsWith("WEIGHTS_") -> Weights(name.removePrefix("WEIGHTS_").toInt().toValidIndex())
                else -> Unknown(name)
            }
        }

        override fun serialize(encoder: Encoder, value: GltfAttributeKey) = encoder.encodeString(
            when (value) {
                Position -> "POSITION"
                Normal -> "NORMAL"
                Tangent -> "TANGENT"
                is TexCoord -> "TEXCOORD_${value.index}"
                is Color -> "COLOR_${value.index}"
                is Joints -> "JOINTS_${value.index}"
                is Weights -> "WEIGHTS_${value.index}"
                is Unknown -> value.name
            }
        )
    }
}

@Serializable
internal data class GltfPrimitive(
    val attributes: Map<GltfAttributeKey, Int>,
    val indices: Int? = null,
    val material: Int? = null,
    @Serializable(with = PrimitiveModeSerializer::class)
    val mode: Primitive.Mode = Primitive.Mode.TRIANGLES,
    val targets: List<Map<GltfAttributeKey, Int>>? = null,
) {
    private class PrimitiveModeSerializer : IntEnumSerializer<Primitive.Mode>(
        "top.fifthlight.blazerod.model.Primitive.Mode",
        Primitive.Mode.POINTS to 0,
        Primitive.Mode.LINES to 1,
        Primitive.Mode.LINE_LOOP to 2,
        Primitive.Mode.LINE_STRIP to 3,
        Primitive.Mode.TRIANGLES to 4,
        Primitive.Mode.TRIANGLE_STRIP to 5,
        Primitive.Mode.TRIANGLE_FAN to 6,
    )
}

@Serializable
internal data class GltfMesh(
    val primitives: List<GltfPrimitive>,
    val weights: List<Float>? = null,
)

private class Matrix4fSerializer : KSerializer<Matrix4f> {
    private val delegated = ListSerializer(Float.serializer())

    override val descriptor: SerialDescriptor = delegated.descriptor

    override fun serialize(encoder: Encoder, value: Matrix4f) =
        delegated.serialize(encoder, (0 until 16).map { value.get(it / 4, it % 4) })

    override fun deserialize(decoder: Decoder): Matrix4f {
        val list = delegated.deserialize(decoder)
        if (list.size != 16) {
            throw SerializationException("Bad matrix: $list")
        }
        return Matrix4f().also {
            it.set(list.toFloatArray())
        }
    }
}

private class QuaternionfSerializer : KSerializer<Quaternionf> {
    private val delegated = ListSerializer(Float.serializer())

    override val descriptor: SerialDescriptor = delegated.descriptor

    override fun serialize(encoder: Encoder, value: Quaternionf) =
        delegated.serialize(encoder, listOf(value.x, value.y, value.z, value.w))

    override fun deserialize(decoder: Decoder): Quaternionf {
        val list = delegated.deserialize(decoder)
        if (list.size != 4) {
            throw SerializationException("Bad Quaternionf: $list")
        }
        return Quaternionf().also {
            it.set(list[0], list[1], list[2], list[3])
        }
    }
}

private class RgbaColorSerializer : KSerializer<RgbaColor> {
    private val delegated = ListSerializer(Float.serializer())

    override val descriptor: SerialDescriptor = delegated.descriptor

    override fun serialize(encoder: Encoder, value: RgbaColor) =
        delegated.serialize(encoder, listOf(value.r, value.g, value.b, value.a))

    override fun deserialize(decoder: Decoder): RgbaColor {
        val list = delegated.deserialize(decoder)
        if (list.size != 4) {
            throw SerializationException("Bad RgbaColor: $list")
        }
        return RgbaColor(list[0], list[1], list[2], list[3])
    }
}

internal class Vector3fSerializer : KSerializer<Vector3f> {
    private val delegated = ListSerializer(Float.serializer())

    override val descriptor: SerialDescriptor = delegated.descriptor

    override fun serialize(encoder: Encoder, value: Vector3f) =
        delegated.serialize(encoder, (0 until 3).map { value.get(it) })

    override fun deserialize(decoder: Decoder): Vector3f {
        val list = delegated.deserialize(decoder)
        if (list.size != 3) {
            throw SerializationException("Bad Vector3f: $list")
        }
        return Vector3f().also {
            it.set(list.toFloatArray())
        }
    }
}

@Serializable
internal data class GltfNode(
    val name: String? = null,
    val camera: Int? = null,
    val children: List<Int>? = null,
    val skin: Int? = null,
    val mesh: Int? = null,
    @Serializable(with = Matrix4fSerializer::class)
    val matrix: Matrix4f? = null,
    @Serializable(with = QuaternionfSerializer::class)
    val rotation: Quaternionf? = null,
    @Serializable(with = Vector3fSerializer::class)
    val scale: Vector3f? = null,
    @Serializable(with = Vector3fSerializer::class)
    val translation: Vector3f? = null,
    val weights: List<Float>? = null,
)

@Serializable
internal data class GltfScene(
    val name: String? = null,
    val nodes: List<Int>? = null,
)
