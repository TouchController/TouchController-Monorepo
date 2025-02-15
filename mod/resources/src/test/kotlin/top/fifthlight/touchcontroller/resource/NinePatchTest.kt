package top.fifthlight.touchcontroller.resource

import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

class NinePatchTest {
    fun loadImage(name: String): BufferedImage = javaClass.classLoader.getResourceAsStream(name).use(ImageIO::read)

    @Test
    fun testScaleArea() {
        val image = loadImage("scale.9.png")
        val ninePatch = NinePatch(image)
        assertEquals(
            expected = IntRect(
                offset = IntOffset(
                    x = 1,
                    y = 3,
                ),
                size = IntSize(
                    width = 4,
                    height = 8,
                ),
            ),
            actual = ninePatch.scaleArea
        )
        assertEquals(
            expected = IntPadding.ZERO,
            actual = ninePatch.padding,
        )
    }

    @Test
    fun testScaleAndPadding() {
        val image = loadImage("scale_and_padding.9.png")
        val ninePatch = NinePatch(image)
        assertEquals(
            expected = IntRect(
                offset = IntOffset(
                    x = 3,
                    y = 1,
                ),
                size = IntSize(
                    width = 8,
                    height = 4,
                ),
            ),
            actual = ninePatch.scaleArea
        )
        assertEquals(
            expected = IntPadding(
                left = 5,
                top = 2,
                right = 7,
                bottom = 3,
            ),
            actual = ninePatch.padding,
        )
    }
}