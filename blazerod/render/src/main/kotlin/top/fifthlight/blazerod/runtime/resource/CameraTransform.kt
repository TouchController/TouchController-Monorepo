package top.fifthlight.blazerod.runtime.resource

import org.joml.*
import top.fifthlight.blazerod.model.Camera

sealed class CameraTransform {
    val rotationQuaternion: Quaternionf = Quaternionf()
    var rotationEulerAngles: Vector3f = Vector3f()
    val position: Vector3f = Vector3f()

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

    abstract fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float)

    class MMD(
        val targetPosition: Vector3f,
        var fov: Float,
        var distance: Float,
        rotationEulerAngles: Vector3fc,
    ) : CameraTransform() {
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
        var yfov: Float,
        var zfar: Float? = null,
        var znear: Float,
    ) : CameraTransform() {
        override fun getMatrix(matrix: Matrix4f, aspectRatio: Float, farPlaneDistance: Float) {
            matrix.perspective(yfov, aspectRatio, znear, zfar ?: farPlaneDistance)
        }
    }

    class Orthographic(
        var xmag: Float,
        var ymag: Float,
        var zfar: Float,
        var znear: Float,
    ) : CameraTransform() {
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

