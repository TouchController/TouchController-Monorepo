package top.fifthlight.blazerod.runtime.node.component

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import top.fifthlight.blazerod.api.refcount.AbstractRefCount
import top.fifthlight.blazerod.runtime.ModelInstanceImpl
import top.fifthlight.blazerod.runtime.node.RenderNodeImpl
import top.fifthlight.blazerod.runtime.node.UpdatePhase
import java.util.*

sealed class RenderNodeComponent<C : RenderNodeComponent<C>> : AbstractRefCount() {
    companion object {
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

    override val typeId: String
        get() = "node"

    sealed class Type<C : RenderNodeComponent<C>> {
        object Primitive : Type<top.fifthlight.blazerod.runtime.node.component.PrimitiveComponent>()
        object Joint : Type<top.fifthlight.blazerod.runtime.node.component.JointComponent>()
        object InfluenceSource : Type<top.fifthlight.blazerod.runtime.node.component.InfluenceSourceComponent>()
        object Camera : Type<top.fifthlight.blazerod.runtime.node.component.CameraComponent>()
        object IkTarget : Type<top.fifthlight.blazerod.runtime.node.component.IkTargetComponent>()
    }

    abstract val type: Type<C>

    abstract val updatePhases: List<UpdatePhase.Type>
    abstract fun update(phase: UpdatePhase, node: RenderNodeImpl, instance: ModelInstanceImpl)

    lateinit var node: RenderNodeImpl
        internal set
}

