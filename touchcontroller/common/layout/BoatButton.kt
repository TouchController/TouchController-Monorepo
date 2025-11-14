package top.fifthlight.touchcontroller.common.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.common.control.BoatButton
import top.fifthlight.touchcontroller.common.control.BoatButtonSide.LEFT
import top.fifthlight.touchcontroller.common.control.BoatButtonSide.RIGHT

fun Context.BoatButton(config: BoatButton) {
    val (_, clicked) = Button(config.id) { clicked ->
        if (config.classic) {
            if (clicked) {
                Texture(config.textureSet.textureSet.up, tint = Color(0xFFAAAAAAu))
            } else {
                Texture(config.textureSet.textureSet.up)
            }
        } else {
            if (clicked) {
                Texture(config.textureSet.textureSet.upActive)
            } else {
                Texture(config.textureSet.textureSet.up)
            }
        }
    }
    if (clicked) {
        when (config.side) {
            LEFT -> result.boatLeft = true
            RIGHT -> result.boatRight = true
        }
    }
}