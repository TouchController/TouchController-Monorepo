package top.fifthlight.combine.modifier.placement

import top.fifthlight.combine.input.focus.FocusStateListener
import top.fifthlight.combine.input.key.KeyEventReceiver
import top.fifthlight.combine.input.pointer.PointerEventReceiver
import top.fifthlight.combine.input.text.TextInputReceiver
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.node.LayoutNode
import top.fifthlight.combine.node.WrapperFactory
import top.fifthlight.combine.node.WrapperLayoutNode
import top.fifthlight.combine.node.WrapperModifierNode

interface PlaceListeningModifierNode : WrapperModifierNode {
    fun onPlaced(placeable: Placeable)

    companion object {
        private class OnPlacedWrapperNode(
            node: LayoutNode,
            children: WrapperLayoutNode,
            private val modifierNode: PlaceListeningModifierNode,
        ) : WrapperLayoutNode.PositionWrapper(node, children),
            PointerEventReceiver by children,
            FocusStateListener by children,
            TextInputReceiver by children,
            KeyEventReceiver by children {

            override fun measure(constraints: Constraints): Placeable {
                val result = super.measure(constraints)
                return object : Placeable by result {
                    override fun placeAt(x: Int, y: Int) {
                        result.placeAt(x, y)
                        modifierNode.onPlaced(node)
                    }
                }
            }
        }

        val wrapperFactory = WrapperFactory<PlaceListeningModifierNode> { node, children, modifier ->
            OnPlacedWrapperNode(node, children, modifier)
        }
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}
