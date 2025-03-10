package top.fifthlight.touchcontroller

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.BlockEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import top.fifthlight.combine.platform_1_21_1_21_1.CanvasImpl
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.event.*
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.ui.screen.getConfigScreen
import top.fifthlight.touchcontroller.common_1_21_1.versionModule
import top.fifthlight.touchcontroller.common_1_21_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_21_x.gal.PlatformWindowProviderImpl

@Mod(BuildInfo.MOD_ID)
class TouchController(modEventBus: IEventBus, private val container: ModContainer) : KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)

    init {
        modEventBus.addListener(::onClientSetup)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClientSetup(event: FMLClientSetupEvent) {
        logger.info("Loading TouchController…")

        startKoin {
            slf4jLogger()
            modules(
                loaderModule,
                versionModule,
            )
        }

        initialize()

        val client = Minecraft.getInstance()
        // MUST RUN ON RENDER THREAD
        // Because Forge load mods in parallel, mods don't load on main render thread,
        // which is ok for most cases, but RegisterTouchWindow() and other Win32 API
        // requires caller on the thread created window. We post an event to render
        // thread here, to solve this problem.
        client.execute {
            WindowEvents.onWindowCreated(PlatformWindowProviderImpl(client.window))
        }
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        GameConfigEditorImpl.executePendingCallback()

        container.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { _, parent ->
            getConfigScreen(parent) as Screen
        })

        val controllerHudModel: ControllerHudModel = get()
        NeoForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun hudRender(event: RenderGuiEvent.Post) {
                val canvas = CanvasImpl(event.guiGraphics)
                RenderEvents.onHudRender(canvas)
                RenderSystem.disableBlend()
            }

            @SubscribeEvent
            fun blockOutlineEvent(event: RenderHighlightEvent.Block) {
                if (!controllerHudModel.result.showBlockOutline) {
                    event.isCanceled = true
                }
            }

            @SubscribeEvent
            fun blockBroken(event: BlockEvent.BreakEvent) {
                BlockBreakEvents.afterBlockBreak()
            }

            @SubscribeEvent
            fun renderTick(event: RenderFrameEvent.Pre) {
                RenderEvents.onRenderStart()
            }

            @SubscribeEvent
            fun clientTick(event: ClientTickEvent.Post) {
                TickEvents.clientTick()
            }

            @SubscribeEvent
            fun joinWorld(event: ClientPlayerNetworkEvent.LoggingIn) {
                ConnectionEvents.onJoinedWorld()
            }
        })
    }
}
