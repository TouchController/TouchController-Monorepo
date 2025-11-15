package top.fifthlight.armorstand

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@Mod(ModInfo.MOD_ID)
@EventBusSubscriber(modid = ModInfo.MOD_ID, value = [Dist.CLIENT, Dist.DEDICATED_SERVER])
class ArmorStandNeoForgeEntrypoint(container: ModContainer) {
    init {
        ArmorStandNeoForgeEntrypoint.container = container
    }

    companion object {
        private lateinit var container: ModContainer

        @SubscribeEvent
        @JvmStatic
        fun onClientSetup(event: FMLClientSetupEvent) {
            ArmorStandNeoForgeClient.onInitializeClient(container, event)
        }

        @SubscribeEvent
        fun onPayloadHandlerRegister(event: RegisterPayloadHandlersEvent) {
            ArmorStandNeoForge.instance.registerPayloadHandlers(event)
        }

        @SubscribeEvent
        @JvmStatic
        fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
            ArmorStandNeoForgeServer.onInitializeServer(event)
        }

        @SubscribeEvent
        fun onRegisterBindings(event: RegisterKeyMappingsEvent) {
            ArmorStandNeoForgeClient.registerKeyBindings(event)
        }
    }
}
