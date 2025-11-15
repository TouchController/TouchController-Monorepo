package top.fifthlight.fabazel.jijmerger

import kotlinx.serialization.Serializable

@Serializable
data class FabricModJson(
    val schemaVersion: Int,
    val id: String,
    val version: String,
    val name: String,
    val custom: Map<String, Boolean>?,
)
