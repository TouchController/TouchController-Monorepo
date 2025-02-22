package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.PerspectiveSwitchButton
import top.fifthlight.touchcontroller.control.PerspectiveSwitchButtonStyle
import top.fifthlight.touchcontroller.gal.CameraPerspective
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.PerspectiveSwitchButton(config: PerspectiveSwitchButton) {
    val (newPointer) = Button(id = config.id) {
        val texture = when (config.style) {
            PerspectiveSwitchButtonStyle.CLASSIC -> when (input.perspective) {
                CameraPerspective.FIRST_PERSON -> Textures.CONTROL_CLASSIC_PERSPECTIVE_PERSPECTIVE_FIRST_PERSON
                CameraPerspective.THIRD_PERSON_BACK -> Textures.CONTROL_CLASSIC_PERSPECTIVE_PERSPECTIVE_THIRD_PERSON_BACK
                CameraPerspective.THIRD_PERSON_FRONT -> Textures.CONTROL_CLASSIC_PERSPECTIVE_PERSPECTIVE_THIRD_PERSON_FRONT
            }

            PerspectiveSwitchButtonStyle.CLASSIC_SIMPLE -> Textures.CONTROL_NEW_PERSPECTIVE_PERSPECTIVE_SIMPLE

            PerspectiveSwitchButtonStyle.NEW -> when (input.perspective) {
                CameraPerspective.FIRST_PERSON -> Textures.CONTROL_NEW_PERSPECTIVE_PERSPECTIVE_FIRST_PERSON
                CameraPerspective.THIRD_PERSON_BACK -> Textures.CONTROL_NEW_PERSPECTIVE_PERSPECTIVE_THIRD_PERSON_BACK
                CameraPerspective.THIRD_PERSON_FRONT -> Textures.CONTROL_NEW_PERSPECTIVE_PERSPECTIVE_THIRD_PERSON_FRONT
            }

            PerspectiveSwitchButtonStyle.NEW_SIMPLE -> Textures.CONTROL_NEW_PERSPECTIVE_PERSPECTIVE_SIMPLE
        }
        Texture(texture)
    }
    if (newPointer) {
        result.nextPerspective = true
    }
}