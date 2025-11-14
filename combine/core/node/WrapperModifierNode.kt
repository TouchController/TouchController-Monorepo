package top.fifthlight.combine.node

import top.fifthlight.combine.modifier.Modifier

fun interface WrapperFactory<T: Modifier> {
    fun createWrapper(node: LayoutNode, children: WrapperLayoutNode, modifier: T): WrapperLayoutNode
}

@Suppress("UNCHECKED_CAST")
internal fun <T: Modifier> WrapperFactory<T>.unsafeCreateWrapper(node: LayoutNode, children: WrapperLayoutNode, modifier: Modifier) =
    createWrapper(node, children, modifier as T)

fun <T: Modifier> WrapperFactory<in T>.chain(outer: WrapperFactory<in T>) = WrapperFactory<T> { node, children, modifier ->
    val wrapper = createWrapper(node, children, modifier)
    outer.createWrapper(node, wrapper, modifier)
}

operator fun <T: Modifier> WrapperFactory<in T>.plus(outer: WrapperFactory<in T>) = this.chain(outer)

interface WrapperModifierNode: Modifier {
    val wrapperFactory: WrapperFactory<*>
}
