package top.fifthlight.touchcontroller.common_1_21_1_21_5.gal

import top.fifthlight.touchcontroller.common_1_21_x.gal.AbstractGameActionImpl

object GameActionImpl: AbstractGameActionImpl() {
    override fun takePanorama() {
        client.grabPanoramixScreenshot(
            client.gameDirectory,
            client.window.width,
            client.window.width,
        ).let { message ->
            this.client.execute {
                this.client.gui.chat.addMessage(message)
            }
        }
    }
}