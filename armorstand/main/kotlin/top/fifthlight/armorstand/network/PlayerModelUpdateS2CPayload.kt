package top.fifthlight.armorstand.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.Uuids
import top.fifthlight.armorstand.util.ModelHash
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class PlayerModelUpdateS2CPayload(
    val uuid: UUID,
    val modelHash: ModelHash?,
) : CustomPayload {
    companion object {
        private val PAYLOAD_ID = Identifier.of("armorstand", "player_model_update")
        val ID = CustomPayload.Id<PlayerModelUpdateS2CPayload>(PAYLOAD_ID)
        val CODEC: PacketCodec<PacketByteBuf, PlayerModelUpdateS2CPayload> = PacketCodec.tuple(
            Uuids.PACKET_CODEC,
            PlayerModelUpdateS2CPayload::uuid,
            PacketCodecs.optional(ModelHash.CODEC),
            { Optional.ofNullable(it.modelHash) },
            { uuid, modelId -> PlayerModelUpdateS2CPayload(uuid, modelId.getOrNull()) },
        )
    }

    override fun getId() = ID

}