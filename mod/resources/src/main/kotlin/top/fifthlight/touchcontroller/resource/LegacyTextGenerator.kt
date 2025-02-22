package top.fifthlight.touchcontroller.resource

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writer

@OptIn(ExperimentalSerializationApi::class)
fun generateLegacyText(languageDir: Path, legacyLanguageDir: Path) {
    val languageFiles = languageDir.listDirectoryEntries("*.json")
    for (file in languageFiles) {
        val outputFile = legacyLanguageDir.resolve("${file.nameWithoutExtension}.lang")
        val map: Map<String, String> = Json.decodeFromStream(file.inputStream())
        outputFile.writer().use { writer ->
            map.entries.sortedBy { (key, _) -> key }.forEach { (key, value) ->
                writer.appendLine("$key=$value")
            }
        }
    }
}
