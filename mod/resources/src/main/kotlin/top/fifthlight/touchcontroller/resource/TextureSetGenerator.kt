package top.fifthlight.touchcontroller.resource

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension

private fun String.snakeToCamelCase(firstChatUppercase: Boolean = false): String {
    return this.split('_')
        .joinToString("") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        .replaceFirstChar {
            if (firstChatUppercase) {
                it.uppercaseChar()
            } else {
                it.lowercaseChar()
            }
        }
}

private data class TextureItem(
    val identifier: String,
    val texture: PlacedTexture,
) {
    val name
        get() = texture.relativePath.fileName.nameWithoutExtension
}

@Serializable
private data class TextureSetInfo(
    @SerialName("gray_when_active")
    val grayWhenActive: Boolean,
    @SerialName("base")
    val base: String? = null,
    @SerialName("fallback")
    val fallback: Map<String, String> = mapOf()
)

private data class TextureSet(
    val info: TextureSetInfo,
    val items: Map<String, TextureItem>,
)

@OptIn(ExperimentalSerializationApi::class)
fun generateTextureSet(textures: Map<String, PlacedTexture>, basePath: Path, outputDir: Path) {
    val textureSets = textures
        .filterValues { it.relativePath.startsWith("control") }
        .map { (key, value) -> TextureItem(key, value) }
        .groupBy { (_, texture) -> texture.relativePath.getName(1).toString() }
        .mapValues { (textureSet, textures) ->
            val textureSetInfoPath = basePath.resolve(textureSet).resolve("texture_set.json")
            val textureSetInfo = Json.decodeFromStream<TextureSetInfo>(textureSetInfoPath.inputStream())
            val items = textures.associate { item ->
                Pair(item.name.snakeToCamelCase(), item)
            }
            TextureSet(
                info = textureSetInfo,
                items = items,
            )
        }

    val textureTypeName = ClassName("top.fifthlight.combine.data", "Texture")
    val textureItemBuilder = TypeSpec
        .classBuilder("TextureSet")
        .addModifiers(KModifier.SEALED)
    val textureItems = textureSets
        .values
        .flatMap { it.items.entries }
        .distinctBy { it.key }
        .sortedBy { it.key }
    for ((itemName, _) in textureItems) {
        PropertySpec
            .builder(itemName, textureTypeName)
            .apply {
                if (itemName.endsWith("Active")) {
                    initializer(itemName.removeSuffix("Active"))
                    addModifiers(KModifier.OPEN)
                } else {
                    addModifiers(KModifier.ABSTRACT)
                }
            }
            .build()
            .let(textureItemBuilder::addProperty)
    }
    val textureSetTypeName = ClassName("top.fifthlight.touchcontroller.assets", "TextureSet")
    for ((setName, set) in textureSets) {
        val className = setName.snakeToCamelCase(true)
        TypeSpec.classBuilder(className).run {
            addModifiers(KModifier.OPEN)
            set.info.base?.let { base ->
                superclass(
                    ClassName(
                        "top.fifthlight.touchcontroller.assets",
                        "TextureSet",
                        base.snakeToCamelCase(true)
                    )
                )
            } ?: run {
                superclass(textureSetTypeName)
            }
            for ((itemName, textureItem) in set.items) {
                PropertySpec
                    .builder(itemName, textureTypeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("Textures.%L", textureItem.identifier)
                    .build()
                    .let(this::addProperty)
            }
            for ((target, fallback) in set.info.fallback) {
                PropertySpec
                    .builder(target.snakeToCamelCase(), textureTypeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%L", fallback.snakeToCamelCase())
                    .build()
                    .let(this::addProperty)
            }
            addType(
                TypeSpec.companionObjectBuilder().run {
                    addProperty(
                        PropertySpec
                            .builder(
                                "INSTANCE",
                                ClassName(
                                    "top.fifthlight.touchcontroller.assets",
                                    "TextureSet",
                                    className
                                )
                            )
                            .initializer("%L()", className)
                            .build()
                    )
                    addProperty(
                        PropertySpec
                            .builder(
                                "key",
                                ClassName(
                                    "top.fifthlight.touchcontroller.assets",
                                    "TextureSet",
                                    "TextureSetKey"
                                )
                            )
                            .initializer("TextureSetKey.%L", setName.uppercase())
                            .build()
                    )
                    build()
                }
            )
            build()
        }.let(textureItemBuilder::addType)
    }
    textureItemBuilder.addType(
        TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec
                    .builder("all", List::class.asClassName().parameterizedBy(textureSetTypeName))
                    .initializer("listOf(%L)", textureSets.keys.joinToString(",\n") {
                        val setName = it.snakeToCamelCase(true)
                        "$setName.INSTANCE"
                    })
                    .build()
            )
            .build()
    )
    val textureKeyTypeName = ClassName("top.fifthlight.touchcontroller.assets", "TextureSet", "TextureKey")
    textureItemBuilder.addType(
        TypeSpec.classBuilder("TextureKey").run {
            addModifiers(KModifier.SEALED)
            addAnnotation(Serializable::class)
            val getLambdaType = LambdaTypeName.get(
                parameters = arrayOf(textureSetTypeName),
                returnType = textureTypeName,
            )
            addProperty(
                PropertySpec
                    .builder("get", getLambdaType)
                    .addModifiers(KModifier.ABSTRACT)
                    .build()
            )
            for ((_, item) in textureItems) {
                val itemName = item.name
                addType(
                    TypeSpec.objectBuilder(itemName.snakeToCamelCase(true))
                        .superclass(textureKeyTypeName)
                        .addModifiers(KModifier.DATA)
                        .addAnnotation(Serializable::class)
                        .addAnnotation(
                            AnnotationSpec
                                .builder(SerialName::class)
                                .addMember("%S", itemName)
                                .build()
                        )
                        .addProperty(
                            PropertySpec
                                .builder("get", getLambdaType)
                                .addModifiers(KModifier.OVERRIDE)
                                .initializer("TextureSet::%L", itemName.snakeToCamelCase())
                                .build()
                        )
                        .build()
                )
            }
            addType(
                TypeSpec.companionObjectBuilder()
                    .addProperty(
                        PropertySpec
                            .builder("all", List::class.asClassName().parameterizedBy(textureKeyTypeName))
                            .initializer(
                                "listOf(%L)",
                                textureItems.joinToString(",\n") { it.value.name.snakeToCamelCase(true) })
                            .build()
                    )
                    .build()
            )
            build()
        }
    )
    textureItemBuilder.addType(
        TypeSpec.enumBuilder("TextureSetKey").run {
            addAnnotation(Serializable::class)
            val identifierTypeName = ClassName("top.fifthlight.combine.data", "Identifier")
            primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addParameter(
                        ParameterSpec
                            .builder("nameText", identifierTypeName)
                            .build()
                    )
                    .addParameter(
                        ParameterSpec
                            .builder("textureSet", textureSetTypeName)
                            .build()
                    )
                    .build()
            )
            addProperty(
                PropertySpec
                    .builder("nameText", identifierTypeName)
                    .initializer("nameText")
                    .build()
            )
            addProperty(
                PropertySpec
                    .builder("textureSet", textureSetTypeName)
                    .initializer("textureSet")
                    .build()
            )
            for (setName in textureSets.keys.sorted()) {
                addEnumConstant(
                    setName.uppercase(),
                    TypeSpec
                        .anonymousClassBuilder()
                        .addAnnotation(
                            AnnotationSpec
                                .builder(SerialName::class)
                                .addMember("%S", setName)
                                .build()
                        )
                        .addSuperclassConstructorParameter("Texts.TEXTURE_SET_%L_NAME", setName.uppercase())
                        .addSuperclassConstructorParameter("%L.INSTANCE", setName.snakeToCamelCase(true))
                        .build()
                )
            }
            build()
        }
    )
    FileSpec
        .builder("top.fifthlight.touchcontroller.assets", "TextureSet")
        .addAnnotation(
            AnnotationSpec
                .builder(Suppress::class)
                .addMember("%S", "RedundantVisibilityModifier")
                .build()
        )
        .addType(textureItemBuilder.build())
        .build()
        .writeTo(outputDir)
}