package top.fifthlight.touchcontroller.common.gal

data class EntityFeatures(
    val haveCamel: Boolean,
    val haveLlama: Boolean,
    val haveStrider: Boolean,
)

data class GameFeatures(
    val dualWield: Boolean,
    val entity: EntityFeatures
)