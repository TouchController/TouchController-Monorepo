package top.fifthlight.armorstand

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher

object ArmorStandNeoForgeServer : ArmorStandNeoForge() {
    override lateinit var mainDispatcher: CoroutineDispatcher
        private set
    override lateinit var scope: CoroutineScope
        private set

    fun onInitializeServer(event: FMLDedicatedServerSetupEvent) {
        super.onInitialize()

        NeoForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onServerStart(event: ServerAboutToStartEvent) {
                val server = event.server
                mainDispatcher = ThreadExecutorDispatcher(server)
                scope = CoroutineScope(SupervisorJob() + mainDispatcher)
            }
        })
    }
}
