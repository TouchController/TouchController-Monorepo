package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.PanoramaButton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.PanoramaButton(config: PanoramaButton) {
    val (newClick) = Button(id = config.id) {
        if (config.classic) {
            Texture(texture = Textures.CONTROL_CLASSIC_PANORAMA_PANORAMA)
        } else {
            Texture(texture = Textures.CONTROL_NEW_PANORAMA_PANORAMA)
        }
    }

    if (newClick) {
        result.takePanorama = true
    }
}