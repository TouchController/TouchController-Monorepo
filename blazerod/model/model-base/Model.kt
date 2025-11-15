package top.fifthlight.blazerod.model

data class Model(
    val scenes: List<Scene>,
    val defaultScene: Scene? = null,
    val skins: List<Skin>,
    val expressions: List<Expression> = listOf(),
) {
    init {
        require(scenes.isNotEmpty()) { "Bad model: no scene" }
    }
}
