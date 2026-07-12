package top.fifthlight.blazerod.runtime.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.fifthlight.blazerod.runtime.PhysicsTransformUtil
import kotlin.math.sqrt

class PhysicsTransformUtilTest {
    @Test
    fun antipodalQuaternionsRemainFiniteAtHalfInterpolation() {
        val quaternion = normalizedQuaternion(0.2f, 0.3f, 0.4f, 0.842615f)
        val previous = transform(quaternion)
        val current = transform(quaternion.map { -it }.toFloatArray())
        val destination = FloatArray(7)

        PhysicsTransformUtil.interpolate(previous, current, destination, 1, 0.5f)

        assertTrue(PhysicsTransformUtil.isValidTransform(destination, 0))
        assertQuaternionEquivalent(quaternion, destination.copyOfRange(3, 7))
    }

    @Test
    fun invalidCurrentQuaternionFallsBackToPreviousRotation() {
        val quaternion = normalizedQuaternion(0.1f, -0.2f, 0.3f, 0.9f)
        val previous = transform(quaternion)
        val current = transform(floatArrayOf(Float.NaN, 0f, 0f, 0f))
        val destination = FloatArray(7)

        PhysicsTransformUtil.interpolate(previous, current, destination, 1, 0.5f)

        assertTrue(PhysicsTransformUtil.isValidTransform(destination, 0))
        assertQuaternionEquivalent(quaternion, destination.copyOfRange(3, 7))
    }

    @Test
    fun zeroQuaternionIsRejected() {
        assertFalse(PhysicsTransformUtil.isValidTransform(transform(floatArrayOf(0f, 0f, 0f, 0f)), 0))
    }

    @Test
    fun invalidPositionIsRejected() {
        val value = transform(floatArrayOf(0f, 0f, 0f, 1f))
        value[1] = Float.POSITIVE_INFINITY

        assertFalse(PhysicsTransformUtil.isValidTransform(value, 0))
    }

    @Test
    fun nonFiniteAlphaKeepsPreviousFiniteTransform() {
        val previous = transform(floatArrayOf(0f, 0f, 0f, 1f))
        val current = previous.copyOf().also { it[0] = 20f }
        val destination = FloatArray(7)

        PhysicsTransformUtil.interpolate(previous, current, destination, 1, Float.NaN)

        assertTrue(PhysicsTransformUtil.isValidTransform(destination, 0))
        assertEquals(previous[0], destination[0])
    }

    private fun transform(quaternion: FloatArray) = floatArrayOf(
        1f, 2f, 3f,
        quaternion[0], quaternion[1], quaternion[2], quaternion[3],
    )

    private fun normalizedQuaternion(x: Float, y: Float, z: Float, w: Float): FloatArray {
        val inverseLength = 1f / sqrt(x * x + y * y + z * z + w * w)
        return floatArrayOf(x * inverseLength, y * inverseLength, z * inverseLength, w * inverseLength)
    }

    private fun assertQuaternionEquivalent(expected: FloatArray, actual: FloatArray) {
        val dot =
            expected[0] * actual[0] +
                expected[1] * actual[1] +
                expected[2] * actual[2] +
                expected[3] * actual[3]
        assertEquals(1f, kotlin.math.abs(dot), 1e-5f)
    }
}
