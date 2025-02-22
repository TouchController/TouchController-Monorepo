package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.AttackButton
import top.fifthlight.touchcontroller.control.AttackButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.AttackButton(config: AttackButton) {
    KeyMappingButton(id = config.id, keyType = KeyBindingType.ATTACK) { clicked ->
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (config.texture) {
                AttackButtonTexture.CLASSIC -> {
                    if (clicked) {
                        Texture(texture = Textures.CONTROL_CLASSIC_ATTACK_ATTACK, tint = Color(0xFFAAAAAAu))
                    } else {
                        Texture(texture = Textures.CONTROL_CLASSIC_ATTACK_ATTACK)
                    }
                }

                AttackButtonTexture.NEW -> {
                    if (clicked) {
                        Texture(texture = Textures.CONTROL_NEW_ATTACK_ATTACK_ACTIVE)
                    } else {
                        Texture(texture = Textures.CONTROL_NEW_ATTACK_ATTACK)
                    }
                }
            }
        }
    }
}