package top.fifthlight.combine.platform_1_21_6_1_21_7

import androidx.compose.runtime.Composable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import top.fifthlight.combine.platform_1_21_x.AbstractCombineScreen
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.combine.data.Text as CombineText

class CombineScreen(
    title: Component,
    renderBackground: Boolean,
    parent: Screen?,
) : AbstractCombineScreen(
    title = title,
    renderBackground = renderBackground,
    parent = parent,
) {
    override val soundManager = SoundManagerImpl(client.soundManager)
    override val screenFactory: ScreenFactory
        get() = ScreenFactoryImpl
}

object ScreenFactoryImpl : ScreenFactory {
    override fun openScreen(
        renderBackground: Boolean,
        title: CombineText,
        content: @Composable () -> Unit,
    ) {
        val client = Minecraft.getInstance()
        val screen = getScreen(client.screen, renderBackground, title, content)
        client.setScreen(screen as Screen)
    }

    override fun getScreen(
        parent: Any?,
        renderBackground: Boolean,
        title: CombineText,
        content: @Composable () -> Unit,
    ): Any {
        val screen = CombineScreen(
            title.toMinecraft(),
            renderBackground = renderBackground,
            parent?.let { it as Screen }
        )
        screen.setContent {
            content()
        }
        return screen
    }
}
