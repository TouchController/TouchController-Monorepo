package top.fifthlight.armorstand.ui.component

data class Insets(
    val top: Int = 0,
    val left: Int = 0,
    val bottom: Int = 0,
    val right: Int = 0,
) {
    constructor(all: Int) : this(all, all, all, all)
    constructor(vertical: Int = 0, horizonal: Int = 0) : this(vertical, horizonal, vertical, horizonal)

    companion object {
        val ZERO = Insets(0)
    }
}
