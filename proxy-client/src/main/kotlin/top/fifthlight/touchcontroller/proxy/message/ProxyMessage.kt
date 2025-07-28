package top.fifthlight.touchcontroller.proxy.message

import java.nio.ByteBuffer

sealed class ProxyMessage {
    abstract val type: Int
    open val wrapInLargeMessage: Boolean
        get() = false

    open fun encode(buffer: ByteBuffer) {
        buffer.putInt(type)
    }

    companion object {
        fun decode(type: Int, payload: ByteBuffer): ProxyMessage = when (type) {
            1 -> AddPointerMessage.Decoder
            2 -> RemovePointerMessage.Decoder
            3 -> ClearPointerMessage.Decoder
            4 -> VibrateMessage.Decoder
            5 -> CapabilityMessage.Decoder
            6 -> LargeMessage.Decoder
            7 -> InputStatusMessage.Decoder
            8 -> KeyboardShowMessage.Decoder
            9 -> InputCursorMessage.Decoder
            10 -> InitializeMessage.Decoder
            11 -> InputAreaMessage.Decoder
            12 -> MoveViewMessage.Decoder
            else -> throw BadMessageTypeException(type)
        }.decode(payload)
    }
}

abstract class ProxyMessageDecoder<M : ProxyMessage> {
    abstract fun decode(payload: ByteBuffer): M
}

abstract class MessageDecodeException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
class BadMessageException(message: String) : MessageDecodeException(message)
class BadMessageTypeException(type: Int) : MessageDecodeException("Bad type: $type")
class BadMessageLengthException(expected: Int, actual: Int) :
    MessageDecodeException("Bad message length: expected $expected bytes, but got $actual bytes")
