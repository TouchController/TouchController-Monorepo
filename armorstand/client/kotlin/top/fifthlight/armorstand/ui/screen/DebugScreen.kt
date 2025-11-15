package top.fifthlight.armorstand.ui.screen

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.component.BorderLayout
import top.fifthlight.armorstand.ui.component.LinearLayout

class DebugScreen(parent: Screen? = null) : BaseArmorStandScreen<DebugScreen>(
    title = Text.translatable("armorstand.debug_screen"),
    parent = parent
) {
    private val topBar by lazy {
        TextWidget(width, 32, title, currentClient.textRenderer)
    }
    private val closeButton = ButtonWidget.builder(ScreenTexts.BACK) { close() }.build()
    private val debugTip by lazy {
        TextWidget(Text.translatable("armorstand.debug_screen.tip"), currentClient.textRenderer)
    }
    private val buttons = listOf(
        ButtonWidget.builder(Text.translatable("armorstand.debug_screen.database")) {
            currentClient.setScreen(DatabaseScreen(this@DebugScreen))
        }.build()
    )

    override fun init() {
        val rootLayout = BorderLayout(
            width = width,
            height = height,
            direction = BorderLayout.Direction.VERTICAL,
        )
        rootLayout.setFirstElement(topBar) { topBar, width, height -> topBar.width = width }
        rootLayout.setCenterElement(
            LinearLayout(
                direction = LinearLayout.Direction.VERTICAL,
                width = width,
                gap = 8
            ).apply {
                add(debugTip, Positioner.create().apply { alignHorizontalCenter() })
                buttons.forEach { button ->
                    add(button, Positioner.create().apply { alignHorizontalCenter() })
                }
            }
        )
        rootLayout.setSecondElement(
            LinearLayout(
                width = width,
                height = 32,
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                gap = 8,
            ).apply {
                add(closeButton, Positioner.create().apply { alignVerticalCenter() })
            })
        rootLayout.refreshPositions()
        rootLayout.forEachChild { addDrawableChild(it) }
    }
}