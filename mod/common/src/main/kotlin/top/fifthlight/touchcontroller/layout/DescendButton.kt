package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.DescendButton
import top.fifthlight.touchcontroller.control.DescendButtonTexture
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.DescendButton(config: DescendButton) {
    KeyMappingSwipeButton(id = config.id, keyType = KeyBindingType.SNEAK) { clicked ->
        when (Pair(config.texture, clicked)) {
            Pair(DescendButtonTexture.CLASSIC, false) -> Texture(texture = Textures.CONTROL_CLASSIC_DESCEND_DESCEND)
            Pair(DescendButtonTexture.CLASSIC, true) -> Texture(
                texture = Textures.CONTROL_CLASSIC_DESCEND_DESCEND,
                tint = Color(0xFFAAAAAAu)
            )

            Pair(
                DescendButtonTexture.SWIMMING,
                false
            ) -> Texture(texture = Textures.CONTROL_NEW_DESCEND_DESCEND_SWIMMING)

            Pair(
                DescendButtonTexture.SWIMMING,
                true
            ) -> Texture(texture = Textures.CONTROL_NEW_DESCEND_DESCEND_SWIMMING_ACTIVE)

            Pair(DescendButtonTexture.FLYING, false) -> Texture(texture = Textures.CONTROL_NEW_DESCEND_DESCEND)
            Pair(
                DescendButtonTexture.FLYING,
                true
            ) -> Texture(texture = Textures.CONTROL_NEW_DESCEND_DESCEND_ACTIVE)
        }
    }
}