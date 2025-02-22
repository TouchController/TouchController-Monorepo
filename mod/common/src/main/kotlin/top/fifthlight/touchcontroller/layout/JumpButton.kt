package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.JumpButton
import top.fifthlight.touchcontroller.control.JumpButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private fun Context.JumpButtonTexture(size: IntSize, clicked: Boolean, texture: JumpButtonTexture) {
    withAlign(align = Align.CENTER_CENTER, size = size) {
        when (Pair(texture, clicked)) {
            Pair(JumpButtonTexture.CLASSIC, false) -> Texture(texture = Textures.CONTROL_CLASSIC_JUMP_JUMP)
            Pair(JumpButtonTexture.CLASSIC, true) -> Texture(
                texture = Textures.CONTROL_CLASSIC_JUMP_JUMP,
                tint = Color(0xFFAAAAAAu)
            )

            Pair(
                JumpButtonTexture.CLASSIC_FLYING,
                false
            ) -> Texture(texture = Textures.CONTROL_CLASSIC_JUMP_JUMP_FLYING)
            Pair(JumpButtonTexture.CLASSIC_FLYING, true) -> Texture(
                texture = Textures.CONTROL_CLASSIC_JUMP_JUMP_FLYING,
                tint = Color(0xFFAAAAAAu)
            )

            Pair(JumpButtonTexture.NEW, false) -> Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP)
            Pair(JumpButtonTexture.NEW, true) -> Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_ACTIVE)

            Pair(JumpButtonTexture.NEW_HORSE, false) -> Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_HORSE)
            Pair(JumpButtonTexture.NEW_HORSE, true) -> Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_HORSE_ACTIVE)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Context.DPadJumpButton(
    id: Uuid,
    size: IntSize = this.size,
    texture: JumpButtonTexture = JumpButtonTexture.CLASSIC,
): ButtonResult = SwipeButton(id = id) { clicked ->
    JumpButtonTexture(size, clicked, texture)
}

@OptIn(ExperimentalUuidApi::class)
fun Context.JumpButton(config: JumpButton) {
    KeyMappingButton(id = config.id, keyType = KeyBindingType.JUMP) { clicked ->
        JumpButtonTexture(config.size(), clicked, config.texture)
    }
}
