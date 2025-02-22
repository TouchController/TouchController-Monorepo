package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.AscendButton
import top.fifthlight.touchcontroller.control.AscendButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.AscendButton(config: AscendButton) {
    KeyMappingSwipeButton(id = config.id, keyType = KeyBindingType.JUMP) { clicked ->
        when (Pair(config.texture, clicked)) {
            Pair(AscendButtonTexture.CLASSIC, false) -> Texture(texture = Textures.CONTROL_CLASSIC_ASCEND_ASCEND)
            Pair(AscendButtonTexture.CLASSIC, true) -> Texture(
                texture = Textures.CONTROL_CLASSIC_ASCEND_ASCEND,
                tint = Color(0xFFAAAAAAu),
            )

            Pair(AscendButtonTexture.SWIMMING, false) -> Texture(texture = Textures.CONTROL_NEW_ASCEND_ASCEND_SWIMMING)
            Pair(
                AscendButtonTexture.SWIMMING,
                true
            ) -> Texture(texture = Textures.CONTROL_NEW_ASCEND_ASCEND_SWIMMING_ACTIVE)

            Pair(AscendButtonTexture.FLYING, false) -> Texture(texture = Textures.CONTROL_NEW_ASCEND_ASCEND)
            Pair(
                AscendButtonTexture.FLYING,
                true
            ) -> Texture(texture = Textures.CONTROL_NEW_ASCEND_ASCEND_ACTIVE)
        }
    }
}