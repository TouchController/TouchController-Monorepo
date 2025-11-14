package top.fifthlight.combine.backend.swing

import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.data.IntSize
import java.awt.Graphics

class SwingTextMeasurer(val graphics: Graphics): TextMeasurer {
    override fun measure(text: String): IntSize {
        TODO("Not yet implemented")
    }

    override fun measure(text: String, maxWidth: Int): IntSize {
        TODO("Not yet implemented")
    }

    override fun measure(text: Text): IntSize {
        TODO("Not yet implemented")
    }

    override fun measure(text: Text, maxWidth: Int): IntSize {
        TODO("Not yet implemented")
    }
}