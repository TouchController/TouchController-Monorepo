package top.fifthlight.blazerod.model

import org.joml.*

interface NodeTransformView {
    val matrix: Matrix4fc

    interface Matrix

    interface Decomposed {
        val translation: Vector3fc
        val scale: Vector3fc
        val rotation: Quaternionfc
    }

    interface Bedrock {
        val pivot: Vector3f
        val rotation: Quaternionf
        val translation: Vector3f
        val scale: Vector3f
    }

    fun clone(): NodeTransform
    fun toDecomposed(): NodeTransform.Decomposed
    fun toMatrix(): NodeTransform.Matrix = NodeTransform.Matrix(matrix)

    fun getTranslation(dest: Vector3f): Vector3f
    fun getRotation(dest: Quaternionf): Quaternionf
    fun getScale(dest: Vector3f): Vector3f
}

abstract class NodeTransform : NodeTransformView {
    @ConsistentCopyVisibility
    data class Matrix private constructor(
        override val matrix: Matrix4f,
    ) : NodeTransform(), NodeTransformView.Matrix {
        constructor() : this(Matrix4f())
        constructor(matrix: Matrix4fc) : this(Matrix4f(matrix))

        override fun clone() = Matrix(
            matrix = matrix,
        )

        override fun toDecomposed() = Decomposed(
            translation = matrix.getTranslation(Vector3f()),
            rotation = matrix.getNormalizedRotation(Quaternionf()),
            scale = matrix.getScale(Vector3f()),
        )

        override fun getTranslation(dest: Vector3f): Vector3f = matrix.getTranslation(dest)
        override fun getRotation(dest: Quaternionf): Quaternionf = matrix.getNormalizedRotation(dest)
        override fun getScale(dest: Vector3f): Vector3f = matrix.getScale(dest)
    }

    @ConsistentCopyVisibility
    data class Decomposed private constructor(
        override val translation: Vector3f,
        override val scale: Vector3f,
        override val rotation: Quaternionf,
    ) : NodeTransform(), NodeTransformView.Decomposed {
        constructor(
            translation: Vector3fc = Vector3f(0f),
            rotation: Quaternionfc = Quaternionf(),
            scale: Vector3fc = Vector3f(1f),
        ) : this(
            Vector3f(translation),
            Vector3f(scale),
            Quaternionf(rotation),
        )

        private val cacheMatrix = Matrix4f()
        override val matrix: Matrix4f
            get() = cacheMatrix.translationRotateScale(translation, rotation, scale)

        override fun getTranslation(dest: Vector3f): Vector3f = dest.set(translation)
        override fun getRotation(dest: Quaternionf): Quaternionf = dest.set(rotation)
        override fun getScale(dest: Vector3f): Vector3f = dest.set(scale)

        fun set(other: NodeTransformView.Decomposed) {
            translation.set(other.translation)
            rotation.set(other.rotation)
            scale.set(other.scale)
        }

        override fun clone() = Decomposed(
            translation = translation,
            rotation = rotation,
            scale = scale,
        )

        override fun toDecomposed() = clone()
    }

    @ConsistentCopyVisibility
    data class Bedrock private constructor(
        override val pivot: Vector3f,
        override val rotation: Quaternionf,
        override val translation: Vector3f,
        override val scale: Vector3f,
    ) : NodeTransform(), NodeTransformView.Bedrock {
        constructor(
            pivot: Vector3fc = Vector3f(0f),
            rotation: Quaternionfc = Quaternionf(),
            translation: Vector3fc = Vector3f(0f),
            scale: Vector3fc = Vector3f(1f),
        ) : this(
            Vector3f(pivot),
            Quaternionf(rotation),
            Vector3f(translation),
            Vector3f(scale),
        )

        private val cacheMatrix = Matrix4f()
        override val matrix: Matrix4f
            get() = cacheMatrix
                .scaling(scale)
                .translate(pivot)
                .rotate(rotation)
                .translate(-pivot.x, -pivot.y, -pivot.z)
                .translate(translation)

        override fun getTranslation(dest: Vector3f): Vector3f = dest.set(translation)
        override fun getRotation(dest: Quaternionf): Quaternionf = dest.set(rotation)
        override fun getScale(dest: Vector3f): Vector3f = dest.set(scale)

        override fun clone() = Bedrock(
            pivot = Vector3f(pivot),
            rotation = Quaternionf(rotation),
            translation = Vector3f(translation),
            scale = Vector3f(scale)
        )

        @Deprecated("There is no way to convert bedrock transform to decomposed transform")
        override fun toDecomposed() = Decomposed(
            translation = translation,
            rotation = rotation,
            scale = scale
        )
    }
}