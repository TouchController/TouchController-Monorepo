package top.fifthlight.combine.modifier.pointer

import top.fifthlight.combine.input.focus.FocusStateListener
import top.fifthlight.combine.input.key.KeyEventReceiver
import top.fifthlight.combine.input.pointer.PointerEvent
import top.fifthlight.combine.input.text.TextInputReceiver
import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.node.LayoutNode
import top.fifthlight.combine.node.WrapperFactory
import top.fifthlight.combine.node.WrapperLayoutNode
import top.fifthlight.combine.node.WrapperModifierNode

interface PointerInputModifierNode: WrapperModifierNode {
    fun onPointerEvent(
        event: PointerEvent,
        node: Placeable,
        layoutNode: LayoutNode,
        children: (PointerEvent) -> Boolean
    ): Boolean

    companion object {
        private class PointerInputWrapperNode(
            node: LayoutNode,
            children: WrapperLayoutNode,
            private val modifierNode: PointerInputModifierNode,
        ) : WrapperLayoutNode.PositionWrapper(node, children),
            FocusStateListener by children,
            TextInputReceiver by children,
            KeyEventReceiver by children {

            override fun onPointerEvent(event: PointerEvent): Boolean {
                val accepted = modifierNode.onPointerEvent(event, this, node) {
                    children.onPointerEvent(it)
                }
                return if (accepted) {
                    true
                } else {
                    children.onPointerEvent(event)
                }
            }
        }

        val wrapperFactory = WrapperFactory<PointerInputModifierNode> { node, children, modifier ->
            PointerInputWrapperNode(node, children, modifier)
        }
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}
