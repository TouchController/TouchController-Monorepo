package top.fifthlight.touchcontroller

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents
import net.minecraft.client.Minecraft
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.platform_1_21_6_1_21_8.CanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.touchcontroller.buildinfo.BuildInfo
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.event.*
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common_1_21_6_1_21_8.versionModule
import top.fifthlight.touchcontroller.common_1_21_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_21_x.gal.PlatformWindowProviderImpl

class TouchController : ClientModInitializer, KoinComponent {
    private val logger = LoggerFactory.getLogger(TouchController::class.java)
    
    companion object {
        var loaded = false
    }

    override fun onInitializeClient() {
        logger.info("Loading TouchController…")

        startKoin {
            slf4jLogger()
            modules(
                mixinModule,
                loaderModule,
                versionModule,
            )
        }

        initialize()

        loaded = true
    }

    private fun initialize() {
        val configHolder: GlobalConfigHolder = get()
        configHolder.load()

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.MISC_OVERLAYS,
            Identifier.of(BuildInfo.MOD_ID, "hud").toMinecraft()
        ) { drawContext, partialTicks ->
            val client = Minecraft.getInstance()
            if (!client.options.hideGui) {
                val canvas = CanvasImpl(drawContext)
                RenderEvents.onHudRender(canvas)
            }
        }

        val controllerHudModel: ControllerHudModel = get()
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { _, _ ->
            controllerHudModel.result.showBlockOutline
        }
        ClientTickEvents.END_CLIENT_TICK.register {
            TickEvents.clientTick()
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            ConnectionEvents.onJoinedWorld()
        }
        ClientLifecycleEvents.CLIENT_STARTED.register {
            val client = Minecraft.getInstance()
            WindowEvents.onWindowCreated(PlatformWindowProviderImpl(client.window))
            GameConfigEditorImpl.executePendingCallback()
        }
        ClientPlayerBlockBreakEvents.AFTER.register { _, _, _, _ ->
            BlockBreakEvents.afterBlockBreak()
        }
    }
}
