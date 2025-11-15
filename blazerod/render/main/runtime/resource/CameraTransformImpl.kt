package top.fifthlight.blazerod.runtime.resource

import org.joml.*
import top.fifthlight.blazerod.api.resource.CameraTransform
import top.fifthlight.blazerod.model.Camera

sealed class CameraTransformImpl : CameraTransform {
    override val rotationQuaternion = Quaternionf()
    override val rotationEulerAngles = Vector3f()
    override val position = Vector3f()

    companion object {
        fun of(camera: Camera) = when (camera) {
            is Camera.MMD -> MMD(
                targetPosition = Vector3f(0f),
                fov = 70f,
                distance = 1f,
                rotationEulerAngles = Vector3f(),
            )

            is Camera.Perspective -> Perspective(
                yfov = camera.yfov,
                zfar = camera.zfar,
                znear = camera.znear
            )

            is Camera.Orthographic -> Orthographic(
                xmag = camera.ymag,
                ymag = camera.xmag,
                zfar = camera.zfar,
                znear = camera.znear
            )
        }
    }

    class MMD(
        override val targetPosition: Vector3f,
        override var fov: Float,
        override var distance: Float,
        rotationEulerAngles: Vector3fc,
    ) : CameraTransformImpl(), CameraTransform.MMD {
        private val offsetCache = Vector3f()

        init {
            this.rotationEulerAngles.set(rotationEulerAngles)
            update(Matrix4f())
        }

        override fun update(matrix: Matrix4fc) {
            rotationQuaternion.rotationZYX(
                this.rotationEulerAngles.z(),
                this.rotationEulerAngles.y(),
                this.rotationEulerAngles.x()
            )
            rotationQuaternion.normalize()
            val cameraLocalOffset = offsetCache.set(0f, 0f, -distance)
            val rotatedCameraOffset = rotationQuaternion.transform(cameraLocalOffset)
            matrix.getTranslation(position).add(targetPosition).add(rotatedCameraOffset)
        }

        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            matrix.perspective(fov, aspectRatio, 0.05f, farPlaneDistance)
        }
    }

    class Perspective(
        override var yfov: Float,
        override var zfar: Float? = null,
        override var znear: Float,
    ) : CameraTransformImpl(), CameraTransform.Perspective {
        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            matrix.perspective(yfov, aspectRatio, znear, zfar ?: farPlaneDistance)
        }
    }

    class Orthographic(
        override var xmag: Float,
        override var ymag: Float,
        override var zfar: Float,
        override var znear: Float,
    ) : CameraTransformImpl(), CameraTransform.Orthographic {
        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            val xmag = ymag * aspectRatio
            matrix.ortho(-xmag, xmag, -ymag, ymag, znear, zfar)
        }
    }

    open fun update(matrix: Matrix4fc) {
        matrix.getTranslation(position)
        matrix.getUnnormalizedRotation(rotationQuaternion)
        rotationQuaternion.getEulerAnglesXYZ(rotationEulerAngles)
    }
}

