package top.fifthlight.fabazel.accesswidenerextractor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.fabricmc.accesswidener.AccessWidenerReader
import net.fabricmc.accesswidener.AccessWidenerWriter
import net.fabricmc.accesswidener.TransitiveOnlyFilter
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.reader
import kotlin.io.path.writer

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val useParamFile = args.first().startsWith('@')
    val params = if (useParamFile) {
        val paramFile = Path.of(args.first().removePrefix("@"))
        paramFile.reader().use { it.readLines() }.filter { it.isNotEmpty() }
    } else {
        args.toList()
    }

    val outputPath = Path.of(params[0])
    val format = Json {
        ignoreUnknownKeys = true
    }
    val writer = AccessWidenerWriter()
    for (index in 1 until params.size) {
        val arg = params[index]
        try {
            JarFile(arg, false).use { jarFile ->
                val jsonEntry = jarFile.getJarEntry("fabric.mod.json") ?: return@use null
                val json = jarFile.getInputStream(jsonEntry).use { format.decodeFromStream<FabricModJson>(it) }
                val accessWidenerPath = json.accessWidener ?: return@use null
                val accessWidenerEntry = jarFile.getJarEntry(accessWidenerPath)
                    ?: error("Bad access widener path: $accessWidenerPath")
                val reader = AccessWidenerReader(TransitiveOnlyFilter(writer))
                jarFile.getInputStream(accessWidenerEntry).use { reader.read(it.bufferedReader()) }
            }
        } catch (ex: Exception) {
            throw RuntimeException("Failed when processing $arg", ex)
        }
    }
    outputPath.writer().use {
        it.write(writer.writeString())
    }
}
