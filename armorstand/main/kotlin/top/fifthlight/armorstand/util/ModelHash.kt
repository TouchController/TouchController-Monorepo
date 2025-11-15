package top.fifthlight.armorstand.util

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs

data class ModelHash(
    val hash: ByteArray,
) {
    companion object {
        val CODEC: PacketCodec<ByteBuf, ModelHash> = PacketCodecs.BYTE_ARRAY.xmap(::ModelHash, ModelHash::hash)
    }

    init {
        require(hash.size == 32) { "Bad model sha256 length: ${hash.size}" }
    }

    override fun toString() = hash.toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelHash

        return hash.contentEquals(other.hash)
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }
}