package top.fifthlight.armorstand.ui.component

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text
import kotlin.math.min

class ModelWidget(
    x: Int = 0,
    y: Int = 0,
    width: Int = 1,
    height: Int = 1,
    private val surface: Surface = Surface.empty,
) : ClickableWidget(x, y, width, height, Text.empty()) {
    override fun isNarratable() = false

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        surface.draw(context, x, y, width, height)
        val client = MinecraftClient.getInstance()
        client.player?.let { player ->
            InventoryScreen.drawEntity(
                context,
                x,
                y,
                x + width,
                y + height,
                min(width, height) / 2,
                .0625f,
                mouseX.toFloat(),
                mouseY.toFloat(),
                player,
            )
        }
    }

    override fun playDownSound(soundManager: SoundManager) {}

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {}
}