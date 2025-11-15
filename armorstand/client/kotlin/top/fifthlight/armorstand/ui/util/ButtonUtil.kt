package top.fifthlight.armorstand.ui.util

import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.screen.BaseArmorStandScreen

fun <T : BaseArmorStandScreen<T>> BaseArmorStandScreen<T>.autoWidthButton(
    text: Text,
    padding: Int = 8,
    onPress: ButtonWidget.PressAction,
): ButtonWidget = ButtonWidget.builder(text, onPress)
    .width(currentClient.textRenderer.getWidth(text) + padding * 2)
    .build()