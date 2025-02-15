package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.PlayerListButton
import top.fifthlight.touchcontroller.control.PlayerListButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType

fun Context.PlayerListButton(config: PlayerListButton) {
    KeyMappingButton(id = "player_list", keyType = KeyBindingType.PLAYER_LIST) { clicked ->
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (config.texture) {
                PlayerListButtonTexture.CLASSIC -> Texture(Textures.GUI_CONTROL_PLAYER_LIST_PLAYER_LIST)
                PlayerListButtonTexture.NEW -> Texture(Textures.GUI_CONTROL_PLAYER_LIST_PLAYER_LIST_NEW)
            }
        }
    }
}