package top.fifthlight.touchcontroller.common_1_21_x.gal

import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.touchcontroller.common.gal.GameAction
import top.fifthlight.touchcontroller.helper.ChatScreenOpenable

abstract class AbstractGameActionImpl : GameAction {
    protected val client: Minecraft = Minecraft.getInstance()

    override fun openChatScreen() {
        (client as ChatScreenOpenable).`touchcontroller$openChatScreen`("")
    }

    override fun openGameMenu() {
        client.pauseGame(false)
    }

    override fun sendMessage(text: Text) {
        client.gui.chat.addMessage(text.toMinecraft())
    }

    override fun nextPerspective() {
        val perspective = client.options.cameraType
        client.options.cameraType = client.options.cameraType.cycle()
        if (perspective.isFirstPerson != client.options.cameraType.isFirstPerson) {
            val newCameraEntity = client.getCameraEntity().takeIf { client.options.cameraType.isFirstPerson }
            client.gameRenderer.checkEntityPostEffect(newCameraEntity)
        }
    }

    override fun takeScreenshot() {
        Screenshot.grab(
            client.gameDirectory,
            client.mainRenderTarget,
        ) { message ->
            this.client.execute {
                this.client.gui.chat.addMessage(message)
            }
        }
    }

    override var hudHidden: Boolean
        get() = client.options.hideGui
        set(value) {
            client.options.hideGui = value
        }
}