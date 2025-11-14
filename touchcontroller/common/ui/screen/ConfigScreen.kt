package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.CurrentScreen
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
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
import top.fifthlight.combine.util.LocalCloseHandler
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.ui.component.*
import top.fifthlight.touchcontroller.common.ui.model.ConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.model.LocalConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.tab.OnResetHandler
import top.fifthlight.touchcontroller.common.ui.tab.Tab
import top.fifthlight.touchcontroller.common.ui.tab.TabGroup
import top.fifthlight.touchcontroller.common.ui.tab.general.RegularTab

@Composable
private fun ConfigScreen() {
    val closeHandler = LocalCloseHandler.current
    val screenModel: ConfigScreenModel = koinInject()
    DisposableEffect(screenModel) {
        onDispose {
            screenModel.onDispose()
        }
    }

    val uiState by screenModel.uiState.collectAsState()
    AlertDialog(
        visible = uiState.developmentWarningDialog,
        onDismissRequest = {
            screenModel.closeDevelopmentDialog()
        },
        title = {
            Text(Text.translatable(Texts.WARNING_DEVELOPMENT_VERSION_TITLE))
        },
        action = {
            GuideButton(onClick = { screenModel.closeDevelopmentDialog() }) {
                Text(Text.translatable(Texts.WARNING_DEVELOPMENT_VERSION_OK))
            }
        },
    ) {
        Text(Text.translatable(Texts.WARNING_DEVELOPMENT_VERSION_MESSAGE))
    }

    val tabGroups = remember {
        val allTabs = Tab.Companion.getAllTabs(screenModel)
        (persistentListOf(null) + TabGroup.Companion.allGroups).map { group ->
            Pair(group, allTabs.filter { it.options.group == group }.sortedBy { it.options.index })
        }
    }

    CompositionLocalProvider(LocalConfigScreenModel provides screenModel) {
        TouchControllerNavigator(RegularTab) { navigator ->
            val currentTab = (navigator.lastItem as? Tab)?.takeIf { !it.options.openAsScreen }
            currentTab?.let {
                var onResetTab by remember { mutableStateOf<OnResetHandler?>(null) }
                AlertDialog(
                    value = onResetTab,
                    valueTransformer = { it },
                    modifier = Modifier
                        .fillMaxWidth(.4f)
                        .minWidth(230),
                    onDismissRequest = {
                        onResetTab = null
                    },
                    title = {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_TITLE))
                    }
                ) { currentOnResetTab ->
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
                                    screenModel.updateConfig(currentOnResetTab)
                                    onResetTab = null
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_CURRENT_TAB))
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    screenModel.updateConfig {
                                        copy(preset = PresetConfig.BuiltIn())
                                    }
                                    onResetTab = null
                                }
                            ) {
                                Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_LAYOUT_SETTINGS))
                            }
                        }
                        WarningButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                screenModel.resetConfig()
                                onResetTab = null
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_ALL_SETTINGS))
                        }
                        GuideButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onResetTab = null
                            }
                        ) {
                            Text(Text.translatable(Texts.SCREEN_CONFIG_RESET_CANCEL))
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
                                )
                            },
                            title = {
                                Text(currentTab.options.title)
                            },
                            trailing = {
                                val onReset = currentTab.options.onReset
                                if (onReset != null) {
                                    WarningButton(
                                        onClick = {
                                            onResetTab = onReset
                                        }
                                    ) {
                                        Text(Text.translatable(Texts.SCREEN_CONFIG_RESET))
                                    }
                                    Button(
                                        onClick = {
                                            screenModel.undoConfig()
                                        }
                                    ) {
                                        Text(Text.translatable(Texts.SCREEN_CONFIG_UNDO))
                                    }
                                    Button(
                                        onClick = {
                                            screenModel.undoConfig()
                                            closeHandler.close()
                                        }
                                    ) {
                                        Text(Text.translatable(Texts.SCREEN_CONFIG_CANCEL))
                                    }
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
                            },
                            tabGroups = tabGroups,
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
