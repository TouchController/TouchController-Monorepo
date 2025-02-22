package top.fifthlight.touchcontroller.resource

import com.squareup.kotlinpoet.*
import java.nio.file.Path

fun generateTexturesBinding(atlasTextures: Map<String, PlacedTexture>, outputDir: Path) {
    val texturesBuilder = TypeSpec.objectBuilder("Textures")

    val atlasSize = PropertySpec
        .builder("atlasSize", ClassName("top.fifthlight.data", "IntSize"))
        .initializer("IntSize(%L, %L)", atlasSize.width, atlasSize.height)
        .build()
    texturesBuilder.addProperty(atlasSize)

    for ((name, placed) in atlasTextures) {
        if (placed.ninePatch == null) {
            PropertySpec
                .builder(name, ClassName("top.fifthlight.combine.data", "Texture"))
                .initializer(
                    """
                            Texture(
                                size = IntSize(%L, %L),
                                atlasOffset = IntOffset(%L, %L),
                            )
                        """.trimIndent(),
                    placed.size.width,
                    placed.size.height,
                    placed.position.x,
                    placed.position.y,
                )
                .build()
        } else {
            PropertySpec
                .builder(name, ClassName("top.fifthlight.combine.data", "NinePatchTexture"))
                .initializer(
                    """
                            NinePatchTexture(
                                size = IntSize(%L, %L),
                                atlasOffset = IntOffset(%L, %L),
                                scaleArea = IntRect(
                                    offset = IntOffset(%L, %L),
                                    size = IntSize(%L, %L),
                                ),
                                padding = IntPadding(
                                    left = %L,
                                    top = %L,
                                    right = %L,
                                    bottom = %L,
                                )
                            )
                        """.trimIndent(),
                    placed.size.width,
                    placed.size.height,
                    placed.position.x,
                    placed.position.y,
                    placed.ninePatch.scaleArea.offset.x,
                    placed.ninePatch.scaleArea.offset.y,
                    placed.ninePatch.scaleArea.size.width,
                    placed.ninePatch.scaleArea.size.height,
                    placed.ninePatch.padding.left,
                    placed.ninePatch.padding.top,
                    placed.ninePatch.padding.right,
                    placed.ninePatch.padding.bottom,
                )
                .build()
        }.let(texturesBuilder::addProperty)
    }

    val textures = texturesBuilder.build()

    val file = FileSpec
        .builder("top.fifthlight.touchcontroller.assets", "Textures")
        .addAnnotation(
            AnnotationSpec
                .builder(Suppress::class)
                .addMember("%S", "RedundantVisibilityModifier")
                .build()
        )
        .addImport("top.fifthlight.data", "IntSize", "IntOffset", "IntRect", "IntPadding")
        .addType(textures)
        .build()
    file.writeTo(outputDir)
}