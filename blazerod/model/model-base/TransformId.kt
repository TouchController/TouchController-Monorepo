package top.fifthlight.blazerod.model

enum class TransformId: Comparable<TransformId> {
    /// Initial transform.
    ABSOLUTE,
    /// Relative transform for VMD animation.
    RELATIVE_ANIMATION,
    /// Influence from other node.
    INFLUENCE,
    /// IK.
    IK,
    /// Deform from external parent.
    EXTERNAL_PARENT_DEFORM,
    /// Physics.
    PHYSICS;

    companion object {
        val FIRST = entries.first()
        val LAST = entries.last()
    }

    val prev
        get() = entries[ordinal - 1]

    val next
        get() = entries[ordinal + 1]
}
