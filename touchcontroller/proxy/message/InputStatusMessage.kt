package top.fifthlight.touchcontroller.proxy.message

import top.fifthlight.touchcontroller.proxy.message.input.TextInputState
import top.fifthlight.touchcontroller.proxy.message.input.TextRange
import java.nio.ByteBuffer

private fun String.convertUtf16RangeToUtf8(start: Int, length: Int): Pair<Int, Int> {
    require(start >= 0 && start <= this.length) { "UTF-16 start out of bounds" }
    require(length >= 0 && start + length <= this.length) { "UTF-16 length out of bounds" }

    val prefix = this.substring(0, start)
    val byteStart = prefix.toByteArray(Charsets.UTF_8).size

    val segment = this.substring(start, start + length)
    val byteLength = segment.toByteArray(Charsets.UTF_8).size

    return Pair(byteStart, byteLength)
}

private fun String.convertUtf8RangeToUtf16(start: Int, length: Int): Pair<Int, Int> {
    val allBytes = this.toByteArray(Charsets.UTF_8)
    if (start < 0 || start > allBytes.size) {
        throw IndexOutOfBoundsException("UTF-8 start out of bounds")
    }
    if (length < 0 || start + length > allBytes.size) {
        throw IndexOutOfBoundsException("UTF-8 length out of bounds")
    }

    val beforeBytes = allBytes.copyOfRange(0, start)
    val beforeString = beforeBytes.toString(Charsets.UTF_8)
    val utf16Start = beforeString.length

    val segmentBytes = allBytes.copyOfRange(start, start + length)
    val segmentString = segmentBytes.toString(Charsets.UTF_8)
    val utf16Length = segmentString.length

    return Pair(utf16Start, utf16Length)
}

data class InputStatusMessage(
    val status: TextInputState?,
): ProxyMessage() {
    override val type: Int = 7

    override val wrapInLargeMessage: Boolean
        get() = true

    override fun encode(buffer: ByteBuffer) {
        super.encode(buffer)
        if (status == null) {
            buffer.put(0)
        } else {
            buffer.put(1) // 1
            val encoded = status.text.encodeToByteArray()
            buffer.putInt(encoded.size) // 5
            buffer.put(encoded) // 5 + len

            val (compStart, compLen) = status.text.convertUtf16RangeToUtf8(
                status.composition.start,
                status.composition.length
            )
            buffer.putInt(compStart) // 9 + len
            buffer.putInt(compLen)   // 13 + len

            val (selStart, selLen) = status.text.convertUtf16RangeToUtf8(
                status.selection.start,
                status.selection.length
            )
            buffer.putInt(selStart)  // 17 + len
            buffer.putInt(selLen)    // 21 + len

            buffer.put(if (status.selectionLeft) 1 else 0) // 22 + len
        }
    }

    object Decoder : ProxyMessageDecoder<InputStatusMessage>() {
        override fun decode(payload: ByteBuffer): InputStatusMessage {
            val hasData = payload.get() != 0.toByte()
            if (!hasData) {
                return InputStatusMessage(null)
            }

            if (payload.remaining() < 20) {
                throw BadMessageLengthException(
                    expected = 20,
                    actual = payload.remaining()
                )
            }

            val textLength = payload.getInt()
            if (payload.remaining() < textLength + 17) {
                throw BadMessageLengthException(
                    expected = textLength + 18,
                    actual = payload.remaining()
                )
            }

            val textArray = ByteArray(textLength)
            payload.get(textArray)
            val text = textArray.decodeToString()

            val compositionStartUtf8 = payload.getInt()
            val compositionLengthUtf8 = payload.getInt()
            val selectionStartUtf8 = payload.getInt()
            val selectionLengthUtf8 = payload.getInt()
            val selectionLeft = payload.get() != 0.toByte()

            val (compositionStart, compositionLength) = text.convertUtf8RangeToUtf16(
                compositionStartUtf8,
                compositionLengthUtf8
            )
            val (selectionStart, selectionLength) = text.convertUtf8RangeToUtf16(
                selectionStartUtf8,
                selectionLengthUtf8
            )

            return InputStatusMessage(
                status = TextInputState(
                    text = text,
                    composition = TextRange(
                        start = compositionStart,
                        length = compositionLength
                    ),
                    selection = TextRange(
                        start = selectionStart,
                        length = selectionLength
                    ),
                    selectionLeft = selectionLeft,
                )
            )
        }
    }
}
