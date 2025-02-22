package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.SprintButton
import top.fifthlight.touchcontroller.control.SprintButtonTexture.CLASSIC
import top.fifthlight.touchcontroller.control.SprintButtonTexture.NEW
import top.fifthlight.touchcontroller.control.SprintButtonTrigger.HOLD
import top.fifthlight.touchcontroller.control.SprintButtonTrigger.SINGLE_CLICK_LOCK
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.SprintButton(config: SprintButton) {
    val sprintButtonState = keyBindingHandler.getState(KeyBindingType.SPRINT)
    val (newPointer, clicked) = Button(id = config.id) { clicked ->
        val isLockTrigger = config.trigger == SINGLE_CLICK_LOCK
        val showActive = (!isLockTrigger && clicked) || (isLockTrigger && sprintButtonState.locked)
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (config.texture) {
                CLASSIC -> if (isLockTrigger) {
                    if (sprintButtonState.locked) {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT_ACTIVE, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT_ACTIVE)
                        }
                    } else {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT)
                        }
                    }
                } else {
                    if (clicked) {
                        Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT, tint = Color(0xFFAAAAAAu))
                    } else {
                        Texture(texture = Textures.CONTROL_CLASSIC_SPRINT_SPRINT)
                    }

                }

                NEW -> if (showActive) {
                    Texture(texture = Textures.CONTROL_NEW_SPRINT_SPRINT_ACTIVE)
                } else {
                    Texture(texture = Textures.CONTROL_NEW_SPRINT_SPRINT)
                }
            }
        }
    }
    when (config.trigger) {
        SINGLE_CLICK_LOCK -> if (newPointer) {
            sprintButtonState.locked = !sprintButtonState.locked
        }

        HOLD -> {
            if (clicked) {
                sprintButtonState.clicked = true
            }
        }
    }
}
