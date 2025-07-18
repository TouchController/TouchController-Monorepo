package top.fifthlight.touchcontroller

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RenderHighlightEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.event.BlockBreakEvents
import top.fifthlight.touchcontroller.common.event.ConnectionEvents
import top.fifthlight.touchcontroller.common.event.TickEvents
import top.fifthlight.touchcontroller.common.event.WindowEvents
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.ui.screen.getConfigScreen
import top.fifthlight.touchcontroller.common_1_21_6_1_21_8.versionModule
import top.fifthlight.touchcontroller.common_1_21_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_21_x.gal.PlatformWindowProviderImpl
import java.util.function.Predicate

@Mod(BuildInfo.MOD_ID)
class TouchController(
    context: FMLJavaModLoadingContext,
) : KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)
    
    companion object {
        var loaded = false
    }

    init {
        val modBusGroup = context.modBusGroup
        FMLClientSetupEvent.getBus(modBusGroup).addListener(::onClientSetup)
        InterModProcessEvent.getBus(modBusGroup).addListener(::onInterModProcess)
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

        loaded = true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInterModProcess(event: InterModProcessEvent) {
        GameConfigEditorImpl.executePendingCallback()
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        MinecraftForge.registerConfigScreen { client, parent ->
            getConfigScreen(parent) as Screen
        }

        val controllerHudModel: ControllerHudModel = get()
        BlockEvent.BUS.addListener { event ->
            BlockBreakEvents.afterBlockBreak()
        }
        RenderHighlightEvent.BUS.addListener(Predicate { event ->
            !controllerHudModel.result.showBlockOutline
        })
        TickEvent.ClientTickEvent.Post.BUS.addListener {
            TickEvents.clientTick()
        }
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener {
            ConnectionEvents.onJoinedWorld()
        }
    }
}
