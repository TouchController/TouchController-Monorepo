package top.fifthlight.blazerod.runtime

import kotlin.math.sqrt

object PhysicsTransformUtil {
    private const val COMPONENTS_PER_TRANSFORM = 7
    private const val MIN_QUATERNION_LENGTH_SQUARED = 1e-12f

    fun isValidTransform(transforms: FloatArray, offset: Int): Boolean {
        if (offset < 0 || offset + COMPONENTS_PER_TRANSFORM > transforms.size) {
            return false
        }
        if (!transforms[offset].isFinite() ||
            !transforms[offset + 1].isFinite() ||
            !transforms[offset + 2].isFinite()
        ) {
            return false
        }
        return quaternionLengthSquared(transforms, offset + 3).let {
            it.isFinite() && it > MIN_QUATERNION_LENGTH_SQUARED
        }
    }

    fun interpolate(
        previous: FloatArray,
        current: FloatArray,
        destination: FloatArray,
        count: Int,
        alpha: Float,
    ) {
        require(count >= 0) { "Transform count must not be negative" }
        val requiredSize = count * COMPONENTS_PER_TRANSFORM
        require(previous.size >= requiredSize) { "Previous transform array is too small" }
        require(current.size >= requiredSize) { "Current transform array is too small" }
        require(destination.size >= requiredSize) { "Destination transform array is too small" }

        val t = if (alpha.isFinite()) alpha.coerceIn(0f, 1f) else 0f
        for (i in 0 until count) {
            val offset = i * COMPONENTS_PER_TRANSFORM
            destination[offset] = interpolateFinite(previous[offset], current[offset], t)
            destination[offset + 1] = interpolateFinite(previous[offset + 1], current[offset + 1], t)
            destination[offset + 2] = interpolateFinite(previous[offset + 2], current[offset + 2], t)
            interpolateQuaternion(previous, current, destination, offset, t)
        }
    }

    private fun interpolateFinite(previous: Float, current: Float, alpha: Float): Float {
        if (!previous.isFinite()) return if (current.isFinite()) current else 0f
        if (!current.isFinite()) return previous

        val interpolated = previous + (current - previous) * alpha
        return if (interpolated.isFinite()) interpolated else if (alpha < 0.5f) previous else current
    }

    private fun interpolateQuaternion(
        previous: FloatArray,
        current: FloatArray,
        destination: FloatArray,
        offset: Int,
        alpha: Float,
    ) {
        val previousOffset = offset + 3
        val previousLengthSquared = quaternionLengthSquared(previous, previousOffset)
        val currentLengthSquared = quaternionLengthSquared(current, previousOffset)
        val previousValid = previousLengthSquared.isFinite() &&
            previousLengthSquared > MIN_QUATERNION_LENGTH_SQUARED
        val currentValid = currentLengthSquared.isFinite() &&
            currentLengthSquared > MIN_QUATERNION_LENGTH_SQUARED

        if (!previousValid && !currentValid) {
            writeIdentityQuaternion(destination, previousOffset)
            return
        }
        if (!previousValid) {
            writeNormalizedQuaternion(current, previousOffset, destination, previousOffset)
            return
        }
        if (!currentValid) {
            writeNormalizedQuaternion(previous, previousOffset, destination, previousOffset)
            return
        }

        val dot =
            previous[previousOffset] * current[previousOffset] +
                previous[previousOffset + 1] * current[previousOffset + 1] +
                previous[previousOffset + 2] * current[previousOffset + 2] +
                previous[previousOffset + 3] * current[previousOffset + 3]
        val currentSign = if (dot < 0f) -1f else 1f

        val qx = previous[previousOffset] +
            (current[previousOffset] * currentSign - previous[previousOffset]) * alpha
        val qy = previous[previousOffset + 1] +
            (current[previousOffset + 1] * currentSign - previous[previousOffset + 1]) * alpha
        val qz = previous[previousOffset + 2] +
            (current[previousOffset + 2] * currentSign - previous[previousOffset + 2]) * alpha
        val qw = previous[previousOffset + 3] +
            (current[previousOffset + 3] * currentSign - previous[previousOffset + 3]) * alpha
        val lengthSquared = qx * qx + qy * qy + qz * qz + qw * qw

        if (!lengthSquared.isFinite() || lengthSquared <= MIN_QUATERNION_LENGTH_SQUARED) {
            writeNormalizedQuaternion(previous, previousOffset, destination, previousOffset)
            return
        }

        val inverseLength = 1f / sqrt(lengthSquared)
        destination[previousOffset] = qx * inverseLength
        destination[previousOffset + 1] = qy * inverseLength
        destination[previousOffset + 2] = qz * inverseLength
        destination[previousOffset + 3] = qw * inverseLength
    }

    private fun quaternionLengthSquared(transforms: FloatArray, offset: Int): Float =
        transforms[offset] * transforms[offset] +
            transforms[offset + 1] * transforms[offset + 1] +
            transforms[offset + 2] * transforms[offset + 2] +
            transforms[offset + 3] * transforms[offset + 3]

    private fun writeNormalizedQuaternion(
        source: FloatArray,
        sourceOffset: Int,
        destination: FloatArray,
        destinationOffset: Int,
    ) {
        val inverseLength = 1f / sqrt(quaternionLengthSquared(source, sourceOffset))
        destination[destinationOffset] = source[sourceOffset] * inverseLength
        destination[destinationOffset + 1] = source[sourceOffset + 1] * inverseLength
        destination[destinationOffset + 2] = source[sourceOffset + 2] * inverseLength
        destination[destinationOffset + 3] = source[sourceOffset + 3] * inverseLength
    }

    private fun writeIdentityQuaternion(destination: FloatArray, offset: Int) {
        destination[offset] = 0f
        destination[offset + 1] = 0f
        destination[offset + 2] = 0f
        destination[offset + 3] = 1f
    }
}
