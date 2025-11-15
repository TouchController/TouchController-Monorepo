package top.fifthlight.armorstand.ui.screen

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

abstract class BaseArmorStandScreen<T: BaseArmorStandScreen<T>>(
    protected val parent: Screen? = null,
    title: Text,
): Screen(title) {
    val currentClient: MinecraftClient
        get() = super.client ?: MinecraftClient.getInstance()

    override fun close() {
        currentClient.setScreen(parent)
    }
}