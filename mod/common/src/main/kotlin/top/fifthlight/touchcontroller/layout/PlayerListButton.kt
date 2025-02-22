package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.PlayerListButton
import top.fifthlight.touchcontroller.control.PlayerListButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.PlayerListButton(config: PlayerListButton) {
    KeyMappingButton(id = config.id, keyType = KeyBindingType.PLAYER_LIST) { clicked ->
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (config.texture) {
                PlayerListButtonTexture.CLASSIC -> Texture(Textures.CONTROL_CLASSIC_PLAYER_LIST_PLAYER_LIST)
                PlayerListButtonTexture.NEW -> Texture(Textures.CONTROL_NEW_PLAYER_LIST_PLAYER_LIST)
            }
        }
    }
}