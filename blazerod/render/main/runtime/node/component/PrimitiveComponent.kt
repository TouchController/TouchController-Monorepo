package top.fifthlight.blazerod.runtime.node.component

import top.fifthlight.blazerod.model.Mesh
import top.fifthlight.blazerod.runtime.ModelInstanceImpl
import top.fifthlight.blazerod.runtime.node.RenderNodeImpl
import top.fifthlight.blazerod.runtime.node.UpdatePhase
import top.fifthlight.blazerod.runtime.node.getWorldTransform
import top.fifthlight.blazerod.runtime.resource.RenderPrimitive

class PrimitiveComponent(
    val primitiveIndex: Int,
    val primitive: RenderPrimitive,
    val skinIndex: Int?,
    val morphedPrimitiveIndex: Int?,
    val firstPersonFlag: Mesh.FirstPersonFlag = Mesh.FirstPersonFlag.BOTH,
) : RenderNodeComponent<PrimitiveComponent>() {
    init {
        primitive.increaseReferenceCount()
    }

    override fun onClosed() {
        primitive.decreaseReferenceCount()
    }

    override val type: Type<PrimitiveComponent>
        get() = Type.Primitive

    companion object {
        private val updatePhases = listOf(UpdatePhase.Type.RENDER_DATA_UPDATE)
    }

    override val updatePhases
        get() = Companion.updatePhases

    override fun update(
        phase: UpdatePhase,
        node: RenderNodeImpl,
        instance: ModelInstanceImpl,
    ) {
        if (phase is UpdatePhase.RenderDataUpdate) {
            if (skinIndex != null) {
                return
            }
            instance.modelData.localMatricesBuffer.edit {
                setMatrix(primitiveIndex, instance.getWorldTransform(node))
            }
        }
    }
}