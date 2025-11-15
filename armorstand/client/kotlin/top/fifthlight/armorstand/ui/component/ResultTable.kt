package top.fifthlight.armorstand.ui.component

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import top.fifthlight.armorstand.ui.state.DatabaseScreenState
import kotlin.math.max

class ResultTable(
    private val textRenderer: TextRenderer,
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
) : ClickableWidget(x, y, width, height, Text.empty()) {
    private var layout: Layout? = null

    private sealed class Layout {
        abstract fun render(
            table: ResultTable,
            context: DrawContext,
        )

        data object Empty : Layout() {
            override fun render(
                table: ResultTable,
                context: DrawContext,
            ) {
                context.drawText(
                    table.textRenderer,
                    Text.translatable("armorstand.debug_database.empty_tip"),
                    table.x,
                    table.y,
                    Colors.WHITE,
                    false,
                )
            }
        }

        data object Loading : Layout() {
            override fun render(
                table: ResultTable,
                context: DrawContext,
            ) {
                context.drawText(
                    table.textRenderer,
                    Text.translatable("armorstand.debug_database.querying"),
                    table.x,
                    table.y,
                    Colors.WHITE,
                    false,
                )
            }
        }

        data class Failed(val message: Text?) : Layout() {
            override fun render(
                table: ResultTable,
                context: DrawContext,
            ) {
                context.drawWrappedText(
                    table.textRenderer,
                    message ?: Text.translatable("armorstand.debug_database.query_failed"),
                    table.x,
                    table.y,
                    table.width,
                    Colors.WHITE,
                    false,
                )
            }
        }

        data class Updated(val message: Text) : Layout() {
            override fun render(
                table: ResultTable,
                context: DrawContext,
            ) {
                context.drawText(
                    table.textRenderer,
                    message,
                    table.x,
                    table.y,
                    Colors.WHITE,
                    false,
                )
            }
        }

        data class Result(
            val duration: Text,
            val headers: List<Text>,
            val rows: List<List<Text>>,
            val columnWidths: List<Int>,
        ) : Layout() {
            override fun render(
                table: ResultTable,
                context: DrawContext,
            ) {
                val textRenderer = table.textRenderer
                val x = table.x
                val y = table.y
                var offsetX = 0
                var offsetY = 0
                // render duration
                context.drawText(
                    textRenderer,
                    duration,
                    x,
                    y,
                    Colors.WHITE,
                    false,
                )
                offsetY += textRenderer.fontHeight + 2
                // render headers
                for ((index, header) in headers.withIndex()) {
                    context.drawText(
                        textRenderer,
                        header,
                        x + offsetX,
                        y + offsetY,
                        Colors.WHITE,
                        false,
                    )
                    offsetX += columnWidths[index] + 2
                }
                context.drawHorizontalLine(x, x + offsetX, y + offsetY + textRenderer.fontHeight, Colors.WHITE)
                // render rows
                offsetX = 0
                offsetY += textRenderer.fontHeight + 2
                for (row in rows) {
                    for ((index, column) in row.withIndex()) {
                        context.drawText(
                            textRenderer,
                            column,
                            x + offsetX,
                            y + offsetY,
                            Colors.WHITE,
                            false,
                        )
                        offsetX += columnWidths[index] + 2
                    }
                    offsetX = 0
                    offsetY += textRenderer.fontHeight + 2
                }
            }
        }
    }

    fun setContent(state: DatabaseScreenState.QueryState) {
        layout = when (state) {
            DatabaseScreenState.QueryState.Empty -> Layout.Empty

            DatabaseScreenState.QueryState.Loading -> Layout.Loading

            is DatabaseScreenState.QueryState.Failed -> Layout.Failed(
                message = state.error?.let {
                    Text.translatable("armorstand.debug_database.query_failed_with_message", it)
                }
            )

            is DatabaseScreenState.QueryState.Updated -> Layout.Updated(
                message = Text.translatable(
                    "armorstand.debug_database.executed",
                    state.updateCount,
                    state.duration.toString()
                )
            )

            is DatabaseScreenState.QueryState.Result -> {
                val headerTexts = state.headers.map { Text.literal(it) }
                val rowTexts = state.rows.map { row -> row.map { Text.literal(it) } }
                val columnWidths = mutableListOf<Int>().apply {
                    headerTexts.forEach { add(textRenderer.getWidth(it)) }
                }
                for (row in rowTexts) {
                    for ((index, column) in row.withIndex()) {
                        columnWidths[index] = max(columnWidths[index], textRenderer.getWidth(column))
                    }
                }
                Layout.Result(
                    duration = Text.translatable("armorstand.debug_database.queried_time", state.duration.toString()),
                    headers = headerTexts,
                    rows = rowTexts,
                    columnWidths = columnWidths,
                )
            }
        }
    }

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        layout?.render(this, context)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {}
}