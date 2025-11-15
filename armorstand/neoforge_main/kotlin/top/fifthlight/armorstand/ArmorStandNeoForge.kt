package top.fifthlight.armorstand

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import top.fifthlight.armorstand.network.ModelUpdateC2SPayload
import top.fifthlight.armorstand.network.PlayerModelUpdateS2CPayload
import top.fifthlight.armorstand.server.ServerModelPathManager

abstract class ArmorStandNeoForge : ArmorStand {
    private var server: MinecraftServer? = null

    companion object {
        lateinit var instance: ArmorStandNeoForge
    }

    fun onInitialize() {
        ArmorStand.instance = this
        instance = this

        ServerModelPathManager.onUpdateListener = { uuid, hash ->
            server?.let { server ->
                val payload = PlayerModelUpdateS2CPayload(uuid, hash)
                server.playerManager.playerList.forEach { player ->
                    if (player.uuid == uuid) {
                        return@let
                    }
                    PacketDistributor.sendToPlayer(player, payload)
                }
            }
        }

        NeoForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onServerStarting(event: ServerStartingEvent) {
                this@ArmorStandNeoForge.server = event.server
            }

            @SubscribeEvent
            fun onServerStopped(event: ServerStoppedEvent) {
                ServerModelPathManager.clear()
            }

            @SubscribeEvent
            fun onPlayerJoined(event: PlayerEvent.PlayerLoggedInEvent) {
                val player = event.entity
                if (player !is ServerPlayerEntity) {
                    return
                }
                for ((uuid, hash) in ServerModelPathManager.getModels()) {
                    if (player.uuid == uuid) {
                        continue
                    }
                    PacketDistributor.sendToPlayer(player, PlayerModelUpdateS2CPayload(uuid, hash))
                }
            }

            @SubscribeEvent
            fun onPlayerDisconnected(event: PlayerEvent.PlayerLoggedOutEvent) {
                val player = event.entity
                if (player !is ServerPlayerEntity) {
                    return
                }
                ServerModelPathManager.update(player.uuid, null)
            }
        })
    }

    open fun registerPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        event.registrar("armorstand")
            .versioned(ModInfo.MOD_VERSION)
            .optional()
            .playToClient(PlayerModelUpdateS2CPayload.ID, PlayerModelUpdateS2CPayload.CODEC)
            .playToServer(ModelUpdateC2SPayload.ID, ModelUpdateC2SPayload.CODEC) { payload, context ->
                ServerModelPathManager.update(context.player().uuid, payload.modelHash)
            }
    }
}