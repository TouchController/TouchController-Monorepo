package top.fifthlight.touchcontroller.resource

import com.squareup.kotlinpoet.*
import java.nio.file.Path

fun generateBuildInfo(properties: Map<String, String>, outputDir: Path) {
    val buildInfo = TypeSpec.objectBuilder("BuildInfo").apply {
        properties.forEach { (name, value) ->
            val transformedName = buildString {
                name.trim().forEach {
                    if (it.isUpperCase()) {
                        append('_')
                    }
                    append(it.uppercase())
                }
            }
            addProperty(
                PropertySpec
                    .builder(transformedName, String::class)
                    .initializer("%S", value.trim())
                    .addModifiers(KModifier.CONST)
                    .build()
            )
        }
    }.build()

    val file = FileSpec
        .builder("top.fifthlight.touchcontroller", "BuildInfo")
        .addAnnotation(
            AnnotationSpec
                .builder(Suppress::class)
                .addMember("%S", "RedundantVisibilityModifier")
                .build()
        )
        .addType(buildInfo)
        .build()
    file.writeTo(outputDir)
}