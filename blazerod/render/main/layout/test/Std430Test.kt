package top.fifthlight.blazerod.layout.test

import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.joml.*
import top.fifthlight.blazerod.layout.GpuDataLayout
import top.fifthlight.blazerod.layout.LayoutStrategy
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Std430Test {
    private fun ByteBuffer.assertInts(offset: Int, stride: Int = 4, vararg expected: Int) =
        expected.forEachIndexed { index, intVal ->
            assertEquals(expected[index], getInt(offset + index * stride))
        }

    private fun ByteBuffer.assertFloats(offset: Int, stride: Int = 4, vararg expected: Float) =
        expected.forEachIndexed { index, floatVal ->
            assertEquals(expected[index], getFloat(offset + index * stride), "Offset ${offset + index * stride}")
        }

    private object SimpleLayout : GpuDataLayout<SimpleLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std430LayoutStrategy
        var intField by int()       // offset: 0, size: 4, align: 4
        var floatField by float()   // offset: 4, size: 4, align: 4
        // total size: 8
    }

    @Test
    fun testSimpleLayout() {
        assertEquals(8, SimpleLayout.totalSize)
        val buffer = ByteBuffer.allocate(SimpleLayout.totalSize)
        SimpleLayout.withBuffer(buffer) {
            intField = 4
            floatField = 2.5f
        }
        assertEquals(4, buffer.getInt(0))
        assertEquals(2.5f, buffer.getFloat(4))
    }

    private object VectorLayout : GpuDataLayout<VectorLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std430LayoutStrategy
        var vec2Field by vec2()     // offset: 0, size: 8, align: 8
        var vec3Field by vec3()     // offset: 16, size: 12, align: 16
        var vec4Field by vec4()     // offset: 32, size: 16, align: 16
        // total size: 48
    }

    @Test
    fun testVectorLayout() {
        assertEquals(48, VectorLayout.totalSize)
        val buffer = ByteBuffer.allocate(VectorLayout.totalSize)
        VectorLayout.withBuffer(buffer) {
            vec2Field = Vector2f(1.0f, 2.0f)
            vec3Field = Vector3f(3.0f, 4.0f, 5.0f)
            vec4Field = Vector4f(6.0f, 7.0f, 8.0f, 9.0f)
        }

        buffer.assertFloats(0, 4, 1.0f, 2.0f)
        buffer.assertFloats(16, 4, 3.0f, 4.0f, 5.0f)
        buffer.assertFloats(32, 4, 6.0f, 7.0f, 8.0f, 9.0f)
    }

    private object MatrixLayout : GpuDataLayout<MatrixLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std430LayoutStrategy
        var mat2Field by mat2()     // offset: 0, size: 16, align: 8
        var mat3Field by mat3()     // offset: 16, size: 48, align: 16
        var mat4Field by mat4()     // offset: 80, size: 64, align: 16
        // total size: 128
    }

    @Test
    fun testMatrixLayout() {
        assertEquals(128, MatrixLayout.totalSize)
        val buffer = ByteBuffer.allocate(MatrixLayout.totalSize)
        MatrixLayout.withBuffer(buffer) {
            mat2Field = Matrix2f(
                1.0f, 3.0f,
                2.0f, 4.0f
            )
            mat3Field = Matrix3f(
                5.0f, 8.0f, 11.0f,
                6.0f, 9.0f, 12.0f,
                7.0f, 10.0f, 13.0f
            )
            mat4Field = Matrix4f(
                14.0f, 18.0f, 22.0f, 26.0f,
                15.0f, 19.0f, 23.0f, 27.0f,
                16.0f, 20.0f, 24.0f, 28.0f,
                17.0f, 21.0f, 25.0f, 29.0f
            )
        }

        buffer.assertFloats(0, 4, 1.0f, 3.0f)
        buffer.assertFloats(8, 4, 2.0f, 4.0f)

        buffer.assertFloats(16, 4, 5.0f, 8.0f, 11.0f)
        buffer.assertFloats(32, 4, 6.0f, 9.0f, 12.0f)
        buffer.assertFloats(48, 4, 7.0f, 10.0f, 13.0f)

        buffer.assertFloats(64, 4, 14.0f, 18.0f, 22.0f, 26.0f)
        buffer.assertFloats(80, 4, 15.0f, 19.0f, 23.0f, 27.0f)
        buffer.assertFloats(96, 4, 16.0f, 20.0f, 24.0f, 28.0f)
        buffer.assertFloats(112, 4, 17.0f, 21.0f, 25.0f, 29.0f)
    }

    private object MixedLayout : GpuDataLayout<MixedLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std430LayoutStrategy
        var intField by int()       // offset: 0, size: 4, align: 4
        var vec3Field by vec3()     // offset: 16, size: 12, align: 16
        var floatField by float()   // offset: 28, size: 4, align: 4
        var mat2Field by mat2()     // offset: 32, size: 16, align: 8
        // total size: 48
    }

    @Test
    fun testMixedLayout() {
        assertEquals(48, MixedLayout.totalSize)
        val buffer = ByteBuffer.allocate(MixedLayout.totalSize)
        MixedLayout.withBuffer(buffer) {
            intField = 1
            vec3Field = Vector3f(2.0f, 3.0f, 4.0f)
            floatField = 5.0f
            mat2Field = Matrix2f(
                6.0f, 8.0f,
                7.0f, 9.0f
            )
        }

        assertEquals(1, buffer.getInt(0))
        buffer.assertFloats(16, 4, 2.0f, 3.0f, 4.0f)
        assertEquals(5.0f, buffer.getFloat(28))
        buffer.assertFloats(32, 4, 6.0f, 8.0f)
        buffer.assertFloats(40, 4, 7.0f, 9.0f)
    }

    private object ArrayLayout : GpuDataLayout<ArrayLayout>() {
        override val strategy: LayoutStrategy
            get() = LayoutStrategy.Std430LayoutStrategy
        var intArray by intArray(4)         // offset: 0, stride: 4, size: 16, align: 4
        var floatArray by floatArray(4)     // offset: 16, stride: 4, size: 16, align: 4
        var vec2Array by vec2Array(4)       // offset: 32, stride: 8, size: 32, align: 8
        var vec3Array by vec3Array(4)       // offset: 64, stride: 16, size: 64, align: 16
        var vec4Array by vec4Array(4)       // offset: 128, stride: 16, size: 64, align: 16
        var mat4Array by mat4Array(2)       // offset: 192, stride: 64, size: 128, align: 16
        // total size: 320
    }

    @Test
    fun testArrayLayout() {
        assertEquals(320, ArrayLayout.totalSize)
        val buffer = ByteBuffer.allocate(ArrayLayout.totalSize)
        ArrayLayout.withBuffer(buffer) {
            intArray = IntArrayList(listOf(1, 2, 3, 4))
            floatArray = FloatArrayList(listOf(1.0f, 2.0f, 3.0f, 4.0f))
            vec2Array = mutableListOf(
                Vector2f(1.0f, 2.0f),
                Vector2f(3.0f, 4.0f),
                Vector2f(5.0f, 6.0f),
                Vector2f(7.0f, 8.0f)
            )
            vec3Array = mutableListOf(
                Vector3f(1.0f, 2.0f, 3.0f),
                Vector3f(4.0f, 5.0f, 6.0f),
                Vector3f(7.0f, 8.0f, 9.0f),
                Vector3f(10.0f, 11.0f, 12.0f)
            )
            vec4Array = mutableListOf(
                Vector4f(1.0f, 2.0f, 3.0f, 4.0f),
                Vector4f(5.0f, 6.0f, 7.0f, 8.0f),
                Vector4f(9.0f, 10.0f, 11.0f, 12.0f),
                Vector4f(13.0f, 14.0f, 15.0f, 16.0f)
            )
            mat4Array = mutableListOf(
                Matrix4f(
                    1.0f, 5.0f, 9.0f, 13.0f,
                    2.0f, 6.0f, 10.0f, 14.0f,
                    3.0f, 7.0f, 11.0f, 15.0f,
                    4.0f, 8.0f, 12.0f, 16.0f
                ),
                Matrix4f(
                    17.0f, 21.0f, 25.0f, 29.0f,
                    18.0f, 22.0f, 26.0f, 30.0f,
                    19.0f, 23.0f, 27.0f, 31.0f,
                    20.0f, 24.0f, 28.0f, 32.0f
                )
            )

            assertFailsWith(IndexOutOfBoundsException::class) {
                intArray[4] = 5
            }
            assertFailsWith(IndexOutOfBoundsException::class) {
                mat4Array[2] = Matrix4f()
            }
        }

        buffer.assertInts(0, 4, 1, 2, 3, 4)

        buffer.assertFloats(16, 4, 1.0f, 2.0f, 3.0f, 4.0f)

        repeat(4) { i ->
            buffer.assertFloats(32 + i * 8, 4, (i * 2 + 1).toFloat(), (i * 2 + 2).toFloat())
        }

        repeat(4) { i ->
            buffer.assertFloats(64 + i * 16, 4, (i * 3 + 1).toFloat(), (i * 3 + 2).toFloat(), (i * 3 + 3).toFloat())
        }

        repeat(4) { i ->
            buffer.assertFloats(
                128 + i * 16,
                4,
                (i * 4 + 1).toFloat(),
                (i * 4 + 2).toFloat(),
                (i * 4 + 3).toFloat(),
                (i * 4 + 4).toFloat()
            )
        }

        repeat(2) { i ->
            val baseOffset = 192 + i * 64
            val startVal = (i * 16 + 1).toFloat()
            buffer.assertFloats(
                baseOffset, 4,
                startVal + 0, startVal + 4, startVal + 8, startVal + 12
            )
            buffer.assertFloats(
                baseOffset + 16, 4,
                startVal + 1, startVal + 5, startVal + 9, startVal + 13
            )
            buffer.assertFloats(
                baseOffset + 32, 4,
                startVal + 2, startVal + 6, startVal + 10, startVal + 14
            )
            buffer.assertFloats(
                baseOffset + 48, 4,
                startVal + 3, startVal + 7, startVal + 11, startVal + 15
            )
        }
    }
}
