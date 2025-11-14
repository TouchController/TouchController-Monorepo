package top.fifthlight.combine.ui.style

import top.fifthlight.combine.paint.Drawable

data class DrawableSet(
    val normal: Drawable = Drawable.Empty,
    val focus: Drawable = normal,
    val hover: Drawable = focus,
    val active: Drawable = hover,
    val disabled: Drawable = normal,
) {
    companion object {
        val Empty = DrawableSet()
    }
}
