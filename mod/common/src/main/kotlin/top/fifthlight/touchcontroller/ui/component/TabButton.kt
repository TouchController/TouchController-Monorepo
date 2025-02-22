package top.fifthlight.touchcontroller.ui.component

import androidx.compose.runtime.Composable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.touchcontroller.assets.Textures

val tabButtonTextures = NinePatchTextureSet(
    normal = Textures.WIDGET_TAB_TAB,
    focus = Textures.WIDGET_TAB_TAB_HOVER,
    hover = Textures.WIDGET_TAB_TAB_HOVER,
    active = Textures.WIDGET_TAB_TAB_ACTIVE,
    disabled = Textures.WIDGET_TAB_TAB_DISABLED,
)

val selectedTabButtonTextures = NinePatchTextureSet(
    normal = Textures.WIDGET_TAB_TAB_PRESSLOCK,
    focus = Textures.WIDGET_TAB_TAB_PRESSLOCK_HOVER,
    hover = Textures.WIDGET_TAB_TAB_PRESSLOCK_HOVER,
    active = Textures.WIDGET_TAB_TAB_ACTIVE,
    disabled = Textures.WIDGET_TAB_TAB_DISABLED,
)

@Composable
fun TabButton(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Button(
        modifier = modifier,
        textureSet = if (selected) {
            selectedTabButtonTextures
        } else {
            tabButtonTextures
        },
        colorTheme = if (selected) {
            ColorTheme.light
        } else {
            ColorTheme.dark
        },
        onClick = onClick,
        clickSound = clickSound,
        content = content,
    )
}