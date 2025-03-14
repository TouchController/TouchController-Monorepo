package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.*
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.persistentListOf
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.layout.Layout
import top.fifthlight.combine.layout.offset
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.ParentDataModifierNode
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
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

@Composable
private fun TwoItemRow(
    modifier: Modifier = Modifier,
    rightWidth: Int,
    space: Int,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            require(measurables.size == 2) { "TwoItemRow must contain two items" }

            val (left, right) = measurables
            val leftPlaceable = left.measure(
                constraints
                    .offset(-rightWidth - space, 0)
                    .copy(minHeight = rightWidth, maxHeight = rightWidth)
            )
            val rightPlaceable = right.measure(
                constraints.copy(
                    minWidth = rightWidth,
                    maxWidth = rightWidth,
                    minHeight = leftPlaceable.height,
                    maxHeight = leftPlaceable.height,
                )
            )

            layout(leftPlaceable.width + rightPlaceable.width, leftPlaceable.height) {
                leftPlaceable.placeAt(0, 0)
                rightPlaceable.placeAt(leftPlaceable.width + space, 0)
            }
        },
        content = content,
    )
}

@Composable
private fun WidgetButton(
    modifier: Modifier = Modifier,
    widget: ControllerWidget,
    widgetIconSize: IntSize,
    onClicked: () -> Unit,
) {
    ListButton(
        modifier = modifier,
        padding = IntPadding(left = 4, right = 4),
        onClick = onClicked,
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
                text = widget.name.getText()
            )
        }
    }
}

@Composable
private fun BuiltInWidgetList(
    modifier: Modifier = Modifier,
    listContent: WidgetsTabState.ListContent.BuiltIn,
    onWidgetSelected: (ControllerWidget) -> Unit,
) {
    WidgetsLayout(modifier) {
        for (widget in listContent.heroes) {
            WidgetButton(
                modifier = Modifier.widgetType(ControllerWidgetType.HERO),
                widget = widget,
                widgetIconSize = IntSize(32),
                onClicked = { onWidgetSelected(widget) },
            )
        }
        for (widget in listContent.widgets) {
            WidgetButton(
                modifier = Modifier
                    .widgetType(ControllerWidgetType.NORMAL)
                    .fillMaxWidth(),
                widget = widget,
                widgetIconSize = IntSize(16),
                onClicked = { onWidgetSelected(widget) },
            )
        }
    }
}

@Composable
private fun CustomWidgetList(
    modifier: Modifier = Modifier,
    listContent: WidgetsTabState.ListContent.Custom,
    onWidgetSelected: (ControllerWidget) -> Unit,
    onWidgetRenamed: (Int, ControllerWidget) -> Unit,
    onWidgetDeleted: (Int) -> Unit,
) {
    WidgetsLayout(modifier) {
        for ((index, widget) in listContent.widgets.withIndex()) {
            TwoItemRow(
                modifier = Modifier
                    .widgetType(ControllerWidgetType.NORMAL)
                    .fillMaxWidth(),
                rightWidth = 24,
                space = 4,
            ) {
                WidgetButton(
                    widget = widget,
                    widgetIconSize = IntSize(16),
                    onClicked = { onWidgetSelected(widget) },
                )

                var popupOpened by remember { mutableStateOf(false) }
                var anchor by remember { mutableStateOf(IntRect.ZERO) }
                ListButton(
                    modifier = Modifier.anchor { anchor = it },
                    onClick = {
                        popupOpened = true
                    },
                ) {
                    Icon(Textures.ICON_MENU)
                }

                if (popupOpened) {
                    DropDownMenu(
                        anchor = anchor,
                        onDismissRequest = {
                            popupOpened = false
                        }
                    ) {
                        DropdownItemList(
                            modifier = Modifier.verticalScroll(),
                            items = persistentListOf(
                                Pair(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_WIDGET_PRESET_RENAME)) {
                                    onWidgetRenamed(index, widget)
                                },
                                Pair(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_WIDGET_PRESET_DELETE)) {
                                    onWidgetDeleted(index)
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
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

        when (val dialogState = tabState.tabState.dialogState) {
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
                                DropdownItemList(
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

            is WidgetsTabState.DialogState.RenameWidgetPresetItem -> AlertDialog(
                onDismissRequest = { tabModel.closeDialog() },
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_EDIT_WIDGET_PRESET_TITLE))
                },
                action = {
                    GuideButton(
                        onClick = {
                            tabModel.renameWidgetPresetItem(dialogState.index, dialogState.name)
                            tabModel.closeDialog()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_EDIT_WIDGET_PRESET_SAVE))
                    }
                    Button(
                        onClick = {
                            tabModel.closeDialog()
                        },
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_EDIT_WIDGET_PRESET_CANCEL))
                    }
                }
            ) {
                EditText(
                    modifier = Modifier.fillMaxWidth(.5f),
                    value = dialogState.name.asString(),
                    onValueChanged = tabModel::updateRenameWidgetPresetItemDialog
                )
            }

            WidgetsTabState.DialogState.Empty -> Unit
        }

        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                uiState.selectedWidget?.let { selectedWidget ->
                    IconButton(
                        onClick = {
                            screenModel.addWidgetPreset(selectedWidget)
                        },
                    ) {
                        Icon(Textures.ICON_SAVE)
                    }
                }
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
                        checked = tabState.tabState.listState == WidgetsTabState.ListState.BUILTIN,
                        onClick = {
                            tabModel.selectBuiltinTab()
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_BUILTIN))
                    }
                    CheckButton(
                        modifier = Modifier.weight(1f),
                        checked = tabState.tabState.listState == WidgetsTabState.ListState.CUSTOM,
                        onClick = {
                            tabModel.selectCustomTab()
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_WIDGETS_PRESET))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(4)
                        .verticalScroll()
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    fun addWidget(widget: ControllerWidget) {
                        val newWidget = widget.cloneBase(
                            opacity = tabState.tabState.newWidgetParams.opacity,
                        )
                        val index = screenModel.newWidget(newWidget)
                        screenModel.selectWidget(index)
                    }

                    when (val listContent = tabState.listContent) {
                        is WidgetsTabState.ListContent.BuiltIn -> BuiltInWidgetList(
                            modifier = Modifier.fillMaxWidth(),
                            listContent = listContent,
                            onWidgetSelected = ::addWidget,
                        )

                        is WidgetsTabState.ListContent.Custom -> CustomWidgetList(
                            modifier = Modifier.fillMaxWidth(),
                            listContent = listContent,
                            onWidgetSelected = ::addWidget,
                            onWidgetRenamed = tabModel::openRenameWidgetPresetItemDialog,
                            onWidgetDeleted = tabModel::deleteWidgetPresetItem,
                        )
                    }
                }
            }
        }
    }
}