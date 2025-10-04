package top.fifthlight.blazerod.model

import org.joml.Vector3f
import org.joml.Vector3fc

data class IkTarget(
    val limitRadian: Float,
    val loopCount: Int,
    val joints: List<IkJoint>,
    val effectorNodeId: NodeId,
) {
    data class IkJoint(
        val nodeId: NodeId,
        val limit: Limits?,
    ) {
        data class Limits(
            val min: Vector3fc,
            val max: Vector3fc,
        ) {
            enum class Axis(
                val axis: Vector3fc,
            ) {
                X(axis = Vector3f(1f, 0f, 0f)),
                Y(axis = Vector3f(0f, 1f, 0f)),
                Z(axis = Vector3f(0f, 0f, 1f)),
            }

            val singleAxis: Axis? = run {
                val xRestricted = min.x() == 0f && max.x() == 0f
                val yRestricted = min.y() == 0f && max.y() == 0f
                val zRestricted = min.z() == 0f && max.z() == 0f
                when {
                    xRestricted && yRestricted && !zRestricted -> Axis.Z
                    xRestricted && !yRestricted && zRestricted -> Axis.Y
                    !xRestricted && yRestricted && zRestricted -> Axis.X
                    else -> null
                }
            }
        }
    }
}
