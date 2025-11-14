package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.BackgroundTextures
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.about.License
import top.fifthlight.touchcontroller.common.ui.component.AppBar
import top.fifthlight.touchcontroller.common.ui.component.BackButton
import top.fifthlight.touchcontroller.common.ui.component.Scaffold

class LicenseScreen(
    val license: License,
) : Screen {
    @Composable
    override fun Content() {
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_LICENSE_TITLE)
                        )
                    },
                    title = {
                        Text(license.name)
                    },
                )
            },
        ) { modifier ->
            license.content?.let { content ->
                Text(
                    text = content,
                    modifier = Modifier
                        .padding(4)
                        .verticalScroll()
                        .background(BackgroundTextures.BRICK_BACKGROUND)
                        .then(modifier)
                )
            }
        }
    }
}