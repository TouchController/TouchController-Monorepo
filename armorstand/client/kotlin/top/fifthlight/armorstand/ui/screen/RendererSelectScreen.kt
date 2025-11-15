package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.flow.map
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import top.fifthlight.armorstand.config.GlobalConfig
import top.fifthlight.armorstand.ui.component.*
import top.fifthlight.armorstand.ui.model.RendererSelectViewModel
import top.fifthlight.armorstand.ui.util.checkbox

class RendererSelectScreen(parent: Screen? = null) : ArmorStandScreen<RendererSelectScreen, RendererSelectViewModel>(
    title = Text.translatable("armorstand.renderer"),
    viewModelFactory = ::RendererSelectViewModel,
    parent = parent,
) {
    private val rendererData = GlobalConfig.RendererKey.entries.map { Pair(it, RendererData(it)) }

    private data class RendererData(
        val speed: Speed,
        val isShaderCompatible: Boolean,
        val isAvailable: Boolean,
    ) {
        constructor(key: GlobalConfig.RendererKey) : this(
            speed = when (key) {
                GlobalConfig.RendererKey.VERTEX_SHADER_TRANSFORM -> Speed.FAST
                GlobalConfig.RendererKey.COMPUTE_SHADER_TRANSFORM -> Speed.MEDIUM
                GlobalConfig.RendererKey.CPU_TRANSFORM -> Speed.SLOW
            },
            isShaderCompatible = when (key) {
                GlobalConfig.RendererKey.VERTEX_SHADER_TRANSFORM -> false
                GlobalConfig.RendererKey.COMPUTE_SHADER_TRANSFORM -> true
                GlobalConfig.RendererKey.CPU_TRANSFORM -> true
            },
            isAvailable = key.type.isAvailable,
        )

        enum class Speed(val nameKey: String) {
            SLOW("armorstand.renderer.speed.slow"),
            MEDIUM("armorstand.renderer.speed.medium"),
            FAST("armorstand.renderer.speed.fast"),
        }
    }

    private val topBar by lazy {
        TextWidget(width, 32, title, currentClient.textRenderer)
    }
    private val dataTable by lazy {
        GridLayout(
            surface = Surface.listBackgroundWithSeparator(),
            gridPadding = Insets(8),
        ).apply {
            val textRenderer = currentClient.textRenderer
            var row = 0
            listOf(
                "armorstand.renderer.name",
                "armorstand.renderer.speed",
                "armorstand.renderer.shader_compatible",
                "armorstand.renderer.is_available",
                "armorstand.renderer.is_current",
            ).forEachIndexed { column, value ->
                add(
                    column = column,
                    row = row,
                    widget = TextWidget(Text.translatable(value), textRenderer),
                )
            }
            row++
            for ((key, value) in rendererData) {
                add(
                    column = 0,
                    row = row,
                    widget = TextWidget(Text.translatable(key.nameKey), textRenderer),
                )
                add(
                    column = 1,
                    row = row,
                    widget = TextWidget(Text.translatable(value.speed.nameKey), textRenderer),
                )
                add(
                    column = 2,
                    row = row,
                    widget = TextWidget(
                        if (value.isShaderCompatible) ScreenTexts.YES else ScreenTexts.NO,
                        textRenderer,
                    ),
                )
                add(
                    column = 3,
                    row = row,
                    widget = TextWidget(
                        if (value.isAvailable) ScreenTexts.YES else ScreenTexts.NO,
                        textRenderer,
                    ),
                )
                add(
                    column = 4,
                    row = row,
                    widget = checkbox(
                        value = viewModel.uiState.map { it.currentRenderer == key },
                        enabled = value.isAvailable,
                    ) {
                        viewModel.setCurrentRenderer(key)
                    },
                )
                row++
            }
            pack()
        }
    }
    private val closeButton = ButtonWidget.builder(ScreenTexts.BACK) { close() }.build()

    override fun init() {
        val rootLayout = BorderLayout(
            width = width,
            height = height,
            direction = BorderLayout.Direction.VERTICAL,
        )
        rootLayout.setFirstElement(topBar) { topBar, width, _ -> topBar.width = width }
        rootLayout.setCenterElement(
            LinearLayout(
                width = width,
                direction = LinearLayout.Direction.VERTICAL,
                align = LinearLayout.Align.CENTER,
            ).apply {
                add(dataTable, Positioner.create().apply { alignHorizontalCenter() })
            },
        )
        addDrawable(dataTable)
        rootLayout.setSecondElement(
            LinearLayout(
                width = width,
                height = 32,
                direction = LinearLayout.Direction.HORIZONTAL,
                align = LinearLayout.Align.CENTER,
                gap = 8,
            ).apply {
                add(closeButton, Positioner.create().apply { alignVerticalCenter() })
            }
        )
        rootLayout.refreshPositions()
        rootLayout.forEachChild { addDrawableChild(it) }
    }

    override fun applyBlur(context: DrawContext) {
        if (client?.world == null) {
            super.applyBlur(context)
        }
    }
}