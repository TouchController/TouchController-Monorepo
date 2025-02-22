package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.ForwardButton
import top.fifthlight.touchcontroller.control.ForwardButtonTexture
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.ForwardButton(config: ForwardButton) {
    val (_, clicked, _) = Button(id = config.id) { clicked ->
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (Pair(config.texture, clicked)) {
                Pair(ForwardButtonTexture.CLASSIC, false) -> Texture(texture = Textures.CONTROL_CLASSIC_DPAD_UP)
                Pair(ForwardButtonTexture.CLASSIC, true) -> Texture(
                    texture = Textures.CONTROL_CLASSIC_DPAD_UP,
                    tint = Color(0xFFAAAAAAu)
                )

                Pair(ForwardButtonTexture.NEW, false) -> Texture(texture = Textures.CONTROL_NEW_DPAD_UP)
                Pair(ForwardButtonTexture.NEW, true) -> Texture(texture = Textures.CONTROL_NEW_DPAD_UP_ACTIVE)
            }
        }
    }
    if (clicked) {
        result.forward = 1f
    }
}