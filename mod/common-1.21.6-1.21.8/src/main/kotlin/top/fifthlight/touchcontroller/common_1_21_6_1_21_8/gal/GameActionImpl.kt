package top.fifthlight.touchcontroller.common_1_21_6_1_21_8.gal

import top.fifthlight.touchcontroller.common_1_21_x.gal.AbstractGameActionImpl

object GameActionImpl: AbstractGameActionImpl() {
    override fun takePanorama() {
        client.grabPanoramixScreenshot(
            client.gameDirectory,
        ).let { message ->
            this.client.execute {
                this.client.gui.chat.addMessage(message)
            }
        }
    }
}