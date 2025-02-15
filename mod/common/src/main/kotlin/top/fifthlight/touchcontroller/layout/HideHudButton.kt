package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.HideHudButton

fun Context.HideHudButton(config: HideHudButton) {
    val (newClick) = Button(id = "hide_hud") {
        if (config.classic) {
            Texture(texture = Textures.GUI_CONTROL_HIDE_HUD_HIDE_HUD)
        } else {
            Texture(texture = Textures.GUI_CONTROL_HIDE_HUD_HIDE_HUD_NEW)
        }
    }

    if (newClick) {
        result.hideHud = true
    }
}