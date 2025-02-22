package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.ScreenshotButton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.ScreenshotButton(config: ScreenshotButton) {
    val (newClick) = Button(id = config.id) {
        if (config.classic) {
            Texture(texture = Textures.CONTROL_CLASSIC_SCREENSHOT_SCREENSHOT)
        } else {
            Texture(texture = Textures.CONTROL_NEW_SCREENSHOT_SCREENSHOT)
        }
    }

    if (newClick) {
        result.takeScreenshot = true
    }
}