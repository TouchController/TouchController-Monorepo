package top.fifthlight.armorstand.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import top.fifthlight.armorstand.util.ModelHash
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class ModelUpdateC2SPayload(
    val modelHash: ModelHash?,
) : CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "model_update")
        val ID = CustomPayload.Id<ModelUpdateC2SPayload>(PAYLOAD_ID)
        val CODEC: PacketCodec<ByteBuf, ModelUpdateC2SPayload> = PacketCodecs.optional(ModelHash.CODEC).xmap(
            { ModelUpdateC2SPayload(it.getOrNull()) },
            { Optional.ofNullable(it.modelHash) },
        )
    }

    override fun getId() = ID
}