package top.fifthlight.touchcontroller.proxy.message

import java.nio.ByteBuffer

data class InputAreaMessage(
    val inputAreaRect: FloatRect? = null,
): ProxyMessage() {
    override val type: Int = 11

    override fun encode(buffer: ByteBuffer) {
        super.encode(buffer)
        if (inputAreaRect != null) {
            buffer.put(1)
            buffer.putFloat(inputAreaRect.left)
            buffer.putFloat(inputAreaRect.top)
            buffer.putFloat(inputAreaRect.width)
            buffer.putFloat(inputAreaRect.height)
        } else {
            buffer.put(0)
        }
    }

    object Decoder : ProxyMessageDecoder<InputAreaMessage>() {
        override fun decode(payload: ByteBuffer): InputAreaMessage {
            if (payload.remaining() < 1) {
                throw BadMessageLengthException(
                    expected = 1,
                    actual = payload.remaining()
                )
            }
            val hasData = payload.get() != 0.toByte()
            if (!hasData) {
                return InputAreaMessage(null)
            }

            if (payload.remaining() != 16) {
                throw BadMessageLengthException(
                    expected = 16,
                    actual = payload.remaining()
                )
            }
            return InputAreaMessage(
                FloatRect(
                    left = payload.getFloat(),
                    top = payload.getFloat(),
                    width = payload.getFloat(),
                    height = payload.getFloat(),
                )
            )
        }
    }
}

