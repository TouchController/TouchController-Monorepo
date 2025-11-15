package top.fifthlight.armorstand.ui.state

import top.fifthlight.armorstand.config.GlobalConfig

data class RendererSelectScreenState(
    val currentRenderer: GlobalConfig.RendererKey,
)
