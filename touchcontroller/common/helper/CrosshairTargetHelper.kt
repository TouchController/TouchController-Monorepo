package top.fifthlight.touchcontroller.common.helper

import org.joml.Matrix4f
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector4d
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import kotlin.math.asin
import kotlin.math.atan2

object CrosshairTargetHelper : KoinComponent {
    private val controllerHudModel: ControllerHudModel by inject()

    var lastCrosshairDirection = Vector3d()

    private fun getNdc(): Vector4d {
        val crosshairStatus = controllerHudModel.result.crosshairStatus

        return if (crosshairStatus == null) {
            Vector4d(0.0, 0.0, -1.0, 1.0)
        } else {
            val screen = Vector2d(crosshairStatus.positionX.toDouble(), crosshairStatus.positionY.toDouble())
            Vector4d(2 * screen.x - 1, 1 - 2 * screen.y, -1.0, 1.0)
        }
    }

    @JvmStatic
    fun getCrosshairDirection(
        projectionMatrix: Matrix4f,
        cameraPitchRadians: Double,
        cameraYawRadians: Double
    ): Vector3d {
        val ndc = getNdc()
        val inverseProjectionMatrix = projectionMatrix.invertPerspective()
        val pointerDirection = ndc.mul(inverseProjectionMatrix)
        return Vector3d(-pointerDirection.x, pointerDirection.y, 1.0).apply {
            rotateX(cameraPitchRadians)
            rotateY(-cameraYawRadians)
            normalize()
        }
    }

    @JvmStatic
    fun calculatePlayerRotation(direction: Vector3d): Pair<Float, Float> {
        var yaw = Math.toDegrees(atan2(-direction.x, direction.z))
        var pitch = Math.toDegrees(asin(-direction.y))
        when {
            pitch > 90.0 -> {
                pitch = 180.0 - pitch
                yaw += 180.0
            }

            pitch < -90.0 -> {
                pitch = -180.0 - pitch
                yaw += 180.0
            }
        }
        return Pair(yaw.toFloat(), pitch.toFloat())
    }
}
