package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.*
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.layout.Layout
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.ParentDataModifierNode
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.placement.size
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.ui.component.CheckButton
import top.fifthlight.touchcontroller.common.ui.component.ListButton
import top.fifthlight.touchcontroller.common.ui.component.ScaledControllerWidget
import top.fifthlight.touchcontroller.common.ui.model.WidgetsTabModel
import top.fifthlight.touchcontroller.common.ui.state.WidgetsTabState
import kotlin.math.max

private enum class ControllerWidgetType {
    NORMAL,
    HERO,
}

private data class ControllerWidgetModifierNode(
    val type: ControllerWidgetType,
) : ParentDataModifierNode, Modifier.Node<ControllerWidgetModifierNode> {
    override fun modifierParentData(parentData: Any?): ControllerWidgetType = type
}

private object WidgetsLayoutScope {
    fun Modifier.widgetType(type: ControllerWidgetType) = then(ControllerWidgetModifierNode(type))
}

@Composable
private fun WidgetsLayout(
    modifier: Modifier = Modifier,
    content: @Composable WidgetsLayoutScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val heroesEnabled = constraints.maxWidth > 196
            val childConstraint = constraints.copy(minWidth = 0, minHeight = 0)
            val heroConstraint = if (heroesEnabled) {
                val heroWidth = constraints.maxWidth / 2
                constraints.copy(
                    minWidth = heroWidth,
                    maxWidth = heroWidth,
                    minHeight = 0,
                )
            } else {
                childConstraint
            }

            val childPositions = Array(measurables.size) { IntOffset.ZERO }
            var cursorPosition = IntOffset(0, 0)
            var maxWidth = 0
            var rowMaxHeight = 0
            var column = 0
            var row = 0

            val placeables = measurables.mapIndexed { index, measurable ->
                val isHero = measurable.parentData == ControllerWidgetType.HERO
                val placeable = measurable.measure(
                    if (isHero) {
                        heroConstraint
                    } else {
                        childConstraint
                    }
                )
                if (!isHero || placeable.width + cursorPosition.left > constraints.maxWidth || column >= 2) {
                    // Break line
                    cursorPosition = IntOffset(0, cursorPosition.y + rowMaxHeight)
                    rowMaxHeight = 0
                    column = 0
                    row++
                }
                column++
                childPositions[index] = cursorPosition
                cursorPosition = IntOffset(cursorPosition.x + placeable.width, cursorPosition.y)
                maxWidth = max(maxWidth, cursorPosition.x)
                rowMaxHeight = max(rowMaxHeight, placeable.height)
                placeable
            }

            val width = maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = (cursorPosition.y + rowMaxHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeAt(childPositions[index])
                }
            }
        },
        content = { WidgetsLayoutScope.content() },
    )
}

object WidgetsTab : CustomTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_WIDGET)
    }

    @Composable
    override fun Content() {
        val (screenModel, uiState, tabsButton, sideBarAtRight) = LocalCustomTabContext.current
        val tabModel: WidgetsTabModel = koinScreenModel { parametersOf(screenModel) }
        val tabState by tabModel.uiState.collectAsState()

        when (val dialogState = tabState.dialogState) {
            is WidgetsTabState.DialogState.ChangeNewWidgetParams -> AlertDialog(
                onDismissRequest = { tabModel.closeDialog() },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.closeDialog()
                            tabModel.updateNewWidgetParams(dialogState.toParams())
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_NEW_WIDGET_SAVE))
                    }
                    Button(
                        onClick = {
                            tabModel.closeDialog()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_NEW_WIDGET_CANCEL))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(.5f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    Text(
                        Text.format(
                            Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_NEW_WIDGET_OPACITY,
                            (dialogState.opacity * 100).toInt()
                        )
                    )
                    Slider(
                        modifier = Modifier.fillMaxWidth(),
                        range = 0f..1f,
                        value = dialogState.opacity,
                        onValueChanged = {
                            tabModel.updateNewWidgetParamsDialog { copy(opacity = it) }
                        }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_NEW_WIDGET_TEXTURE_SET)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Select(
                            expanded = expanded,
                            onExpandedChanged = { expanded = it },
                            dropDownContent = {
                                val textFactory = LocalTextFactory.current
                                SelectItemList(
                                    modifier = Modifier.verticalScroll(),
                                    items = TextureSet.TextureSetKey.entries,
                                    textProvider = { textFactory.of(it.nameText) },
                                    selectedIndex = TextureSet.TextureSetKey.entries.indexOf(dialogState.textureSet),
                                    onItemSelected = { index ->
                                        tabModel.updateNewWidgetParamsDialog { copy(textureSet = TextureSet.TextureSetKey.entries[index]) }
                                        expanded = false
                                    }
                                )
                            }
                        ) {
                            Text(Text.translatable(dialogState.textureSet.nameText))
                            SelectIcon(expanded = expanded)
                        }
                    }
                }
            }

            WidgetsTabState.DialogState.Empty -> Unit
        }

        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                IconButton(
                    onClick = {
                        tabModel.openNewWidgetParamsDialog()
                    }
                ) {
                    Icon(Textures.ICON_CONFIG)
                }
            }
        ) { modifier ->
            SideBarScaffold(
                modifier = modifier,
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS))
                },
                actions = {
                    CheckButton(
                        modifier = Modifier.weight(1f),
                        checked = tabState.listState is WidgetsTabState.ListState.Builtin,
                        onClick = {
                            tabModel.selectBuiltinTab()
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_BUILTIN))
                    }
                    CheckButton(
                        modifier = Modifier.weight(1f),
                        checked = tabState.listState is WidgetsTabState.ListState.Custom,
                        onClick = {
                            tabModel.selectCustomTab()
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_PRESET))
                    }
                }
            ) {
                when (val listState = tabState.listState) {
                    is WidgetsTabState.ListState.Custom.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            alignment = Alignment.Center,
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_LOADING))
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .padding(4)
                                .verticalScroll()
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4),
                        ) {
                            @Composable
                            fun WidgetButton(
                                modifier: Modifier = Modifier,
                                widget: ControllerWidget,
                                widgetIconSize: IntSize,
                            ) {
                                ListButton(
                                    modifier = modifier,
                                    padding = IntPadding(left = 4, right = 4),
                                    onClick = {
                                        val newWidget = widget.cloneBase(
                                            opacity = tabState.newWidgetParams.opacity,
                                        )
                                        val index = screenModel.newWidget(newWidget)
                                        screenModel.selectWidget(index)
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4),
                                    ) {
                                        ScaledControllerWidget(
                                            modifier = Modifier.size(widgetIconSize),
                                            widget = widget,
                                        )
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = widget.name.getString()
                                        )
                                    }
                                }
                            }

                            WidgetsLayout(Modifier.fillMaxWidth()) {
                                listState.heroes?.let { heroes ->
                                    for (widget in heroes) {
                                        WidgetButton(
                                            modifier = Modifier.widgetType(ControllerWidgetType.HERO),
                                            widget = widget,
                                            widgetIconSize = IntSize(32)
                                        )
                                    }
                                }

                                listState.widgets?.let { widgets ->
                                    for (widget in widgets) {
                                        WidgetButton(
                                            modifier = Modifier
                                                .widgetType(ControllerWidgetType.NORMAL)
                                                .fillMaxWidth(),
                                            widget = widget,
                                            widgetIconSize = IntSize(16),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}