package top.fifthlight.touchcontroller.ui.component

import androidx.compose.runtime.Composable
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.height
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.touchcontroller.assets.Textures

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
    title: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier
            .height(20)
            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_GRAY_TITLE)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier
                .alignment(Alignment.CenterLeft)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
        }

        Row(
            modifier = Modifier
                .alignment(Alignment.Center)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            title()
        }

        Row(
            modifier = Modifier
                .alignment(Alignment.CenterRight)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing()
        }
    }
}