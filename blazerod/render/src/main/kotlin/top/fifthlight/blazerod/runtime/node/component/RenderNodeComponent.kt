package top.fifthlight.blazerod.runtime.node.component

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Identifier
import top.fifthlight.blazerod.runtime.ModelInstance
import top.fifthlight.blazerod.runtime.node.RenderNode
import top.fifthlight.blazerod.runtime.node.UpdatePhase
import top.fifthlight.blazerod.util.AbstractRefCount
import java.util.*

sealed class RenderNodeComponent<C : RenderNodeComponent<C>> : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "node")

        protected val DEBUG_RENDER_LAYER: RenderLayer.MultiPhase = RenderLayer.of(
            "blazerod_joint_debug_lines",
            1536,
            RenderPipelines.LINES,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(1.0)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .target(RenderPhase.ITEM_ENTITY_TARGET)
                .build(false)
        )
    }

    override val typeId: Identifier
        get() = TYPE_ID

    sealed class Type<C : RenderNodeComponent<C>> {
        object Primitive : Type<top.fifthlight.blazerod.runtime.node.component.Primitive>()
        object Joint : Type<top.fifthlight.blazerod.runtime.node.component.Joint>()
        object InfluenceSource : Type<top.fifthlight.blazerod.runtime.node.component.InfluenceSource>()
        object Camera : Type<top.fifthlight.blazerod.runtime.node.component.Camera>()
        object IkTarget : Type<top.fifthlight.blazerod.runtime.node.component.IkTarget>()
    }

    abstract val type: Type<C>

    abstract val updatePhases: List<UpdatePhase.Type>
    abstract fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance)

    lateinit var node: RenderNode
        internal set
}

