package top.fifthlight.touchcontroller.common.gal

enum class CrosshairTarget {
    BLOCK,
    ENTITY,
    MISS
}

interface ViewActionProvider {
    fun getCrosshairTarget(): CrosshairTarget?
    fun getCurrentBreakingProgress(): Float
}