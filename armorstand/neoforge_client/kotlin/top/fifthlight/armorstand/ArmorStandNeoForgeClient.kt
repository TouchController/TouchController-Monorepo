package top.fifthlight.armorstand

import com.mojang.logging.LogUtils
import kotlinx.coroutines.*
import net.minecraft.client.MinecraftClient
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppedEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import top.fifthlight.armorstand.ArmorStandClient.Companion.animationKeyBinding
import top.fifthlight.armorstand.ArmorStandClient.Companion.configKeyBinding
import top.fifthlight.armorstand.ArmorStandClient.Companion.modelSwitchKeyBinding
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.debug.ModelManagerDebugFrame
import top.fifthlight.armorstand.event.ScreenEvents
import top.fifthlight.armorstand.manage.ModelManagerHolder
import top.fifthlight.armorstand.network.PlayerModelUpdateS2CPayload
import top.fifthlight.armorstand.state.ClientModelPathManager
import top.fifthlight.armorstand.state.ModelHashManager
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.state.NetworkModelSyncer
import top.fifthlight.armorstand.ui.screen.AnimationScreen
import top.fifthlight.armorstand.ui.screen.ConfigScreen
import top.fifthlight.armorstand.ui.screen.ModelSwitchScreen
import top.fifthlight.armorstand.util.RendererManager
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher
import top.fifthlight.blazerod.api.event.RenderEvents
import top.fifthlight.blazerod.model.formats.ModelFileLoaders
import javax.swing.SwingUtilities

object ArmorStandNeoForgeClient : ArmorStandNeoForge(), ArmorStandClient {
    private val LOGGER = LogUtils.getLogger()

    override lateinit var mainDispatcher: CoroutineDispatcher
        private set
    override lateinit var scope: CoroutineScope
        private set

    override var debug: Boolean = false
        private set
    override var debugBone: Boolean = false
        private set

    fun onInitializeClient(container: ModContainer, event: FMLClientSetupEvent) {
        super.onInitialize()
        ArmorStandClient.instance = this

        container.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { _, parent ->
            ConfigScreen(parent)
        })

        if (System.getProperty("armorstand.debug") == "true") {
            debug = true
            if (System.getProperty("armorstand.debug.bone") == "true") {
                debugBone = true
            }
            if (System.getProperty("armorstand.debug.gui") == "true") {
                System.setProperty("java.awt.headless", "false")
                SwingUtilities.invokeLater {
                    try {
                        ModelManagerDebugFrame().isVisible = true
                    } catch (ex: Exception) {
                        LOGGER.info("Failed to show debug windows", ex)
                    }
                }
            }
        }

        NeoForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onBeforeEntityRender(event: RenderLevelStageEvent.AfterOpaqueBlocks) {
                PlayerRenderer.startRenderWorld()
            }

            @SubscribeEvent
            fun onAfterEntityRender(event: RenderLevelStageEvent.AfterEntities) {
                PlayerRenderer.executeDraw()
                ModelInstanceManager.cleanup(System.nanoTime())
            }

            @SubscribeEvent
            fun onEndFrame(event: RenderLevelStageEvent.AfterLevel) {
                PlayerRenderer.endFrame()
            }

            @SubscribeEvent
            fun onClientStopping(event: ClientStoppedEvent) {
                runBlocking {
                    ModelManagerHolder.close()
                }
                scope.cancel()
            }

            @SubscribeEvent
            fun onStartClientTick(event: ClientTickEvent.Pre) {
                val client = MinecraftClient.getInstance()
                if (client.player == null) {
                    return
                }
                if (client.currentScreen != null) {
                    return
                }
                if (configKeyBinding.isPressed) {
                    client.setScreen(ConfigScreen(null))
                }
                if (animationKeyBinding.isPressed) {
                    client.setScreen(AnimationScreen(null))
                }
                if (modelSwitchKeyBinding.isPressed) {
                    client.setScreen(ModelSwitchScreen(null))
                }
            }

            @SubscribeEvent
            fun onClientDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
                ModelHashManager.clearHash()
            }
        })

        ScreenEvents.UNLOCK_CURSOR.register { screen ->
            when (screen) {
                is ModelSwitchScreen -> false
                else -> true
            }
        }
        ScreenEvents.MOVE_VIEW.register { screen ->
            when (screen) {
                is ModelSwitchScreen -> false
                else -> true
            }
        }

        event.enqueueWork {
            val client = MinecraftClient.getInstance()
            mainDispatcher = ThreadExecutorDispatcher(client)
            scope = CoroutineScope(SupervisorJob() + mainDispatcher)

            RenderEvents.FLIP_FRAME.register {
                RendererManager.rotate()
            }

            ModelFileLoaders.initialize()
            ConfigHolder.read()

            runBlocking {
                ModelManagerHolder.initialize()
                NetworkModelSyncer.initialize()
                ClientModelPathManager.initialize()
            }
        }
    }

    fun registerKeyBindings(event: RegisterKeyMappingsEvent) {
        event.register(configKeyBinding)
        event.register(animationKeyBinding)
        event.register(modelSwitchKeyBinding)
    }

    override fun registerPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        super.registerPayloadHandlers(event)
        event.registrar("armorstand")
            .versioned(ModInfo.MOD_VERSION)
            .optional()
            .playToClient(PlayerModelUpdateS2CPayload.ID, PlayerModelUpdateS2CPayload.CODEC) { payload, context ->
                scope.launch {
                    ModelHashManager.putModelHash(payload.uuid, payload.modelHash)
                }
            }
    }
}
