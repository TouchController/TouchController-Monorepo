package top.fifthlight.combine.modifier.key

import top.fifthlight.combine.input.focus.FocusStateListener
import top.fifthlight.combine.input.key.KeyEvent
import top.fifthlight.combine.input.key.KeyEventReceiver
import top.fifthlight.combine.input.pointer.PointerEventReceiver
import top.fifthlight.combine.input.text.TextInputReceiver
import top.fifthlight.combine.node.LayoutNode
import top.fifthlight.combine.node.WrapperFactory
import top.fifthlight.combine.node.WrapperLayoutNode
import top.fifthlight.combine.node.WrapperModifierNode

interface KeyInputModifierNode : KeyEventReceiver, WrapperModifierNode {
    companion object {
        private class KeyInputWrapperNode(
            node: LayoutNode,
            children: WrapperLayoutNode,
            private val modifierNode: KeyInputModifierNode,
        ) : WrapperLayoutNode.PositionWrapper(node, children),
            PointerEventReceiver by children,
            FocusStateListener by children,
            TextInputReceiver by children {

            override fun onKeyEvent(event: KeyEvent) = modifierNode.onKeyEvent(event)
        }

        val wrapperFactory = WrapperFactory<KeyInputModifierNode> { node, children, modifier ->
            KeyInputWrapperNode(node, children, modifier)
        }
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}
