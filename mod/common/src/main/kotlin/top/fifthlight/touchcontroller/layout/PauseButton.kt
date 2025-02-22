package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.PauseButton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.PauseButton(config: PauseButton) {
    val (newClick) = Button(id = config.id) {
        if (config.classic) {
            Texture(texture = Textures.CONTROL_CLASSIC_PAUSE_PAUSE)
        } else {
            Texture(texture = Textures.CONTROL_NEW_PAUSE_PAUSE)
        }
    }

    if (newClick) {
        result.pause = true
    }
}