package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.SneakButton
import top.fifthlight.touchcontroller.control.SneakButtonTexture
import top.fifthlight.touchcontroller.control.SneakButtonTrigger
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Context.RawSneakButton(
    id: Uuid,
    size: IntSize = this.size,
    trigger: SneakButtonTrigger = SneakButtonTrigger.DOUBLE_CLICK_LOCK,
    texture: SneakButtonTexture = SneakButtonTexture.CLASSIC,
) {
    val sneakButtonState = keyBindingHandler.getState(KeyBindingType.SNEAK)
    val (newPointer, clicked) = Button(id = id) { clicked ->
        val isLockTrigger =
            trigger == SneakButtonTrigger.SINGLE_CLICK_LOCK || trigger == SneakButtonTrigger.DOUBLE_CLICK_LOCK
        val showActive = (!isLockTrigger && clicked) || (isLockTrigger && sneakButtonState.locked)
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (texture) {
                SneakButtonTexture.CLASSIC -> if (isLockTrigger) {
                    if (sneakButtonState.locked) {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK_ACTIVE, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK_ACTIVE)
                        }
                    } else {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK)
                        }
                    }
                } else {
                    if (clicked) {
                        Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK, tint = Color(0xFFAAAAAAu))
                    } else {
                        Texture(texture = Textures.CONTROL_CLASSIC_SNEAK_SNEAK)
                    }
                }

                SneakButtonTexture.NEW -> if (showActive) {
                    Texture(texture = Textures.CONTROL_NEW_SNEAK_SNEAK_ACTIVE)
                } else {
                    Texture(texture = Textures.CONTROL_NEW_SNEAK_SNEAK)
                }

                SneakButtonTexture.NEW_DPAD -> if (showActive) {
                    Texture(texture = Textures.CONTROL_NEW_SNEAK_SNEAK_HORSE_ACTIVE)
                } else {
                    Texture(texture = Textures.CONTROL_NEW_SNEAK_SNEAK_HORSE)
                }

                SneakButtonTexture.DISMOUNT -> if (showActive) {
                    Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_HORSE_ACTIVE)
                } else {
                    Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP)
                }

                SneakButtonTexture.DISMOUNT_DPAD -> if (showActive) {
                    Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_HORSE_ACTIVE)
                } else {
                    Texture(texture = Textures.CONTROL_NEW_JUMP_JUMP_HORSE)
                }
            }
        }
    }

    when (trigger) {
        SneakButtonTrigger.DOUBLE_CLICK_LOCK -> if (newPointer) {
            if (status.sneakLocking.click(timer.tick)) {
                sneakButtonState.locked = !sneakButtonState.locked
            }
        }

        SneakButtonTrigger.SINGLE_CLICK_LOCK -> if (newPointer) {
            sneakButtonState.locked = !sneakButtonState.locked
        }

        SneakButtonTrigger.HOLD -> {
            if (clicked) {
                sneakButtonState.clicked = true
            }
        }

        SneakButtonTrigger.SINGLE_CLICK_TRIGGER -> if (newPointer) {
            sneakButtonState.clicked = true
        }

        SneakButtonTrigger.DOUBLE_CLICK_TRIGGER -> if (newPointer) {
            if (status.sneakTrigger.click(timer.tick)) {
                sneakButtonState.clicked = true
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Context.SneakButton(config: SneakButton) {
    RawSneakButton(
        id = config.id,
        trigger = config.trigger,
        texture = config.texture
    )
}