package top.fifthlight.fabazel.accesswidenerextractor

import kotlinx.serialization.Serializable

@Serializable
data class FabricModJson(
    val schemaVersion: Int,
    val accessWidener: String? = null,
) {
    init {
        require(schemaVersion == 1) { "Only schema version 1 is support, but got $schemaVersion" }
    }
}
