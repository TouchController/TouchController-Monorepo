package top.fifthlight.touchcontroller.ui.screen

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.CurrentScreen
import org.koin.core.context.GlobalContext
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.minWidth
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.ui.component.*
import top.fifthlight.touchcontroller.ui.tab.Tab
import top.fifthlight.touchcontroller.ui.tab.general.RegularTab

@Composable
private fun ConfigScreen() {
    TouchControllerNavigator(RegularTab) { navigator ->
        val currentTab = (navigator.lastItem as? Tab)?.takeIf { !it.options.openAsScreen }
        currentTab?.let {
            var openResetDialog by remember { mutableStateOf(false) }
            if (openResetDialog) {
                AlertDialog(
                    modifier = Modifier
                        .fillMaxWidth(.4f)
                        .minWidth(230),
                    onDismissRequest = {
                        openResetDialog = false
                    },
                    title = {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_TITLE))
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {

                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_CURRENT_TAB))
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {

                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_LAYOUT_SETTINGS))
                            }
                        }
                        WarningButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {

                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_ALL_SETTINGS))
                        }
                        GuideButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                openResetDialog = false
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_CANCEL))
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    AppBar(
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            BackButton(
                                screenName = Text.translatable(Texts.SCREEN_CONFIG_TITLE),
                                close = true
                            )
                        },
                        title = {
                            Text(currentTab.options.title)
                        },
                        trailing = {
                            WarningButton(
                                onClick = {
                                    openResetDialog = true
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_RESET))
                            }
                            Button(
                                onClick = {

                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_UNDO))
                            }
                            Button(
                                onClick = {

                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_CANCEL))
                            }
                        }
                    )
                },
                leftSideBar = {
                    SideTabBar(
                        modifier = Modifier.fillMaxHeight(),
                        onTabSelected = { tab, options ->
                            if (options.openAsScreen) {
                                navigator.push(tab)
                            } else {
                                navigator.replace(tab)
                            }
                        }
                    )
                },
            ) { modifier ->
                Box(modifier) {
                    CurrentScreen()
                }
            }
        } ?: run {
            CurrentScreen()
        }
    }
}

fun getConfigScreenButtonText(): Any = with(GlobalContext.get()) {
    val textFactory: TextFactory = get()
    textFactory.of(Texts.SCREEN_CONFIG)
}

fun getConfigScreen(parent: Any?): Any? = with(GlobalContext.get()) {
    val textFactory: TextFactory = get()
    val screenFactory: ScreenFactory = get()
    screenFactory.getScreen(
        parent = parent,
        renderBackground = false,
        title = textFactory.of(Texts.SCREEN_CONFIG_TITLE),
        content = { ConfigScreen() },
    )
}
