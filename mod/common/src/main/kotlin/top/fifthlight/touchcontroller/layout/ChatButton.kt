package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.ChatButton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.ChatButton(config: ChatButton) {
    val (newClick) = Button(id = config.id) {
        if (config.classic) {
            Texture(texture = Textures.CONTROL_CLASSIC_CHAT_CHAT)
        } else {
            Texture(texture = Textures.CONTROL_NEW_CHAT_CHAT)
        }
    }

    if (newClick) {
        result.chat = true
    }
}