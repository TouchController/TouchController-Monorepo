@file:Suppress("NOTHING_TO_INLINE")

package top.fifthlight.blazerod.util.bitmap

@JvmInline
value class BitmapItem(val inner: Int = 0) {
    @JvmInline
    value class Element(val mask: Int) {
        companion object {
            fun of(index: Int) = Element(1 shl index)
        }
    }

    inline operator fun plus(element: Element) = BitmapItem(inner or element.mask)

    inline operator fun minus(element: Element) = BitmapItem(inner and element.mask.inv())

    inline operator fun contains(element: Element) = (inner and element.mask) != 0
}

