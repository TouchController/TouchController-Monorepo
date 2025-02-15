package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.ForwardButton
import top.fifthlight.touchcontroller.control.ForwardButtonTexture

fun Context.ForwardButton(config: ForwardButton) {
    val (_, clicked, _) = Button(id = "forward") { clicked ->
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (Pair(config.texture, clicked)) {
                Pair(ForwardButtonTexture.CLASSIC, false) -> Texture(texture = Textures.GUI_CONTROL_DPAD_UP_CLASSIC)
                Pair(ForwardButtonTexture.CLASSIC, true) -> Texture(
                    texture = Textures.GUI_CONTROL_DPAD_UP_CLASSIC,
                    color = 0xFFAAAAAAu
                )

                Pair(ForwardButtonTexture.NEW, false) -> Texture(texture = Textures.GUI_CONTROL_DPAD_UP)
                Pair(ForwardButtonTexture.NEW, true) -> Texture(texture = Textures.GUI_CONTROL_DPAD_UP_ACTIVE)
            }
        }
    }
    if (clicked) {
        result.forward = 1f
    }
}