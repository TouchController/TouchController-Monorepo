package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.input.focus.FocusStateListener
import top.fifthlight.combine.input.key.KeyEventReceiver
import top.fifthlight.combine.input.pointer.PointerEventReceiver
import top.fifthlight.combine.input.text.TextInputReceiver
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.layout.measure.Measurable
import top.fifthlight.combine.layout.measure.MeasureResult
import top.fifthlight.combine.layout.measure.MeasureScope
import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.node.LayoutNode
import top.fifthlight.combine.node.WrapperFactory
import top.fifthlight.combine.node.WrapperLayoutNode
import top.fifthlight.combine.node.WrapperModifierNode
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.withState

interface LayoutModifierNode : WrapperModifierNode {
    fun measure(measurable: Measurable, constraints: Constraints) =
        with(MeasureScope) { this.measure(measurable, constraints) }

    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult

    fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int =
        throw UnsupportedOperationException("minIntrinsicWidth not implemented")

    fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int =
        throw UnsupportedOperationException("minIntrinsicHeight not implemented")

    fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicWidth not implemented")

    fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicHeight not implemented")

    fun minIntrinsicWidth(measurable: Measurable, height: Int): Int =
        with(MeasureScope) { this.minIntrinsicWidth(measurable, height) }

    fun maxIntrinsicWidth(measurable: Measurable, height: Int): Int =
        with(MeasureScope) { this.maxIntrinsicWidth(measurable, height) }

    fun minIntrinsicHeight(measurable: Measurable, width: Int): Int =
        with(MeasureScope) { this.minIntrinsicHeight(measurable, width) }

    fun maxIntrinsicHeight(measurable: Measurable, width: Int): Int =
        with(MeasureScope) { this.maxIntrinsicHeight(measurable, width) }

    companion object {
        private class LayoutWrapperNode(
            node: LayoutNode,
            val children: WrapperLayoutNode,
            val modifierNode: LayoutModifierNode,
        ) : WrapperLayoutNode(node),
            PointerEventReceiver by children,
            FocusStateListener by children,
            TextInputReceiver by children,
            KeyEventReceiver by children {
            override val parentData: Any? = children.parentData

            override var width: Int = 0
            override var height: Int = 0
            override var x: Int = 0
            override var y: Int = 0
            override val absoluteX: Int
                get() = (parentPlaceable?.absoluteX ?: 0) + x
            override val absoluteY: Int
                get() = (parentPlaceable?.absoluteY ?: 0) + y

            override fun placeAt(x: Int, y: Int) {
                this.x = x
                this.y = y
            }

            override fun measure(constraints: Constraints): Placeable {
                // Clear minimum constraints, so they will not be passed to children layout
                val result = modifierNode.measure(children, constraints)

                width = result.width
                height = result.height
                result.placer.placeChildren()

                return coerceConstraintBounds(constraints, this)
            }

            override fun minIntrinsicWidth(height: Int): Int = modifierNode.minIntrinsicWidth(children, height)
            override fun minIntrinsicHeight(width: Int): Int = modifierNode.minIntrinsicHeight(children, width)
            override fun maxIntrinsicWidth(height: Int): Int = modifierNode.maxIntrinsicWidth(children, height)
            override fun maxIntrinsicHeight(width: Int): Int = modifierNode.maxIntrinsicHeight(children, width)

            override fun Canvas.render() {
                withState {
                    translate(x, y)
                    children.run { render() }
                }
            }
        }


        val wrapperFactory = WrapperFactory<LayoutModifierNode> { node, children, modifier ->
            LayoutWrapperNode(node, children, modifier)
        }
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}
