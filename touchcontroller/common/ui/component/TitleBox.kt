package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.touchcontroller.assets.Textures

@Composable
fun TitleBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(
                top = 2,
                bottom = 3,
            )
            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_LIGHTGRAY_TITLE)
            .then(modifier),
        alignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalColorTheme provides ColorTheme.light
        ) {
            content()
        }
    }
}