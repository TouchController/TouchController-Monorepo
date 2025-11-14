package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.IconButton
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures

object PropertiesTab : CustomTab() {
    @Composable
    override fun Icon() {
        Icon(Textures.ICON_PROPERTIES)
    }

    @Composable
    override fun Content() {
        val (screenModel, uiState, tabsButton, sideBarAtRight) = LocalCustomTabContext.current
        SideBarContainer(
            sideBarAtRight = sideBarAtRight,
            tabsButton = tabsButton,
            actions = {
                val moveLocked = uiState.pageState.moveLocked
                IconButton(
                    onClick = {
                        screenModel.setMoveLocked(!moveLocked)
                    },
                    selected = moveLocked,
                    enabled = uiState.selectedPreset != null,
                ) {
                    Icon(Textures.ICON_LOCK)
                }

                val highlight = uiState.pageState.highlight
                IconButton(
                    onClick = {
                        screenModel.setHighlight(!highlight)
                    },
                    selected = highlight,
                    enabled = uiState.selectedPreset != null,
                ) {
                    Icon(Textures.ICON_LIGHT)
                }

                IconButton(
                    enabled = uiState.selectedWidget != null,
                    onClick = {
                        uiState.selectedWidget?.let(screenModel::addWidgetPreset)
                    },
                ) {
                    Icon(Textures.ICON_SAVE)
                }

                IconButton(
                    enabled = uiState.selectedWidget != null,
                    onClick = {},
                ) {
                    Icon(Textures.ICON_FORMAT_PAINTER)
                }
            }
        ) { modifier ->
            SideBarScaffold(
                modifier = modifier,
                title = {
                    Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_PROPERTIES))
                },
                actions = if (uiState.selectedWidget != null) {
                    {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                screenModel.copyWidget(uiState.selectedWidget)
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_COPY))
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                screenModel.copyWidget(uiState.selectedWidget)
                                screenModel.deleteWidget(uiState.pageState.selectedWidgetIndex)
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_CUT))
                        }
                    }
                } else {
                    null
                }
            ) {
                if (uiState.selectedWidget != null) {
                    Column(
                        modifier = Modifier
                            .padding(4)
                            .verticalScroll()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4),
                    ) {
                        for (property in uiState.selectedWidget.properties) {
                            property.controller(
                                modifier = Modifier.fillMaxWidth(),
                                config = uiState.selectedWidget,
                                currentPreset = uiState.selectedPreset,
                                onConfigChanged = { screenModel.editWidget(uiState.pageState.selectedWidgetIndex, it) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.Center,
                    ) {
                        Text(Text.translatable(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_NO_WIDGET_SELECTED))
                    }
                }
            }
        }
    }
}