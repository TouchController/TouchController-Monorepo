package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.*
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.background
import top.fifthlight.combine.modifier.focus.FocusInteraction
import top.fifthlight.combine.modifier.focus.FocusRequester
import top.fifthlight.combine.modifier.focus.focusRequester
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.height
import top.fifthlight.combine.modifier.placement.width
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.Spacer
import top.fifthlight.combine.widget.ui.AlertDialog
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.DropdownItemList
import top.fifthlight.combine.widget.ui.EditText
import top.fifthlight.combine.widget.ui.GuideButton
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.IconButton
import top.fifthlight.combine.widget.ui.IntSlider
import top.fifthlight.combine.widget.ui.Select
import top.fifthlight.combine.widget.ui.SelectIcon
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.combine.widget.ui.WarningButton
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.common.ui.component.AppBar
import top.fifthlight.touchcontroller.common.ui.component.BackButton
import top.fifthlight.touchcontroller.common.ui.component.ColorPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.HorizontalPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.IntSliderPreferenceItem
import top.fifthlight.touchcontroller.common.ui.component.Scaffold
import top.fifthlight.touchcontroller.common.ui.component.VerticalPreferenceItem
import top.fifthlight.touchcontroller.common.ui.model.ChatScreenModel
import top.fifthlight.touchcontroller.common.ui.state.ChatScreenState

@Composable
private fun ChatScreen() {
    val screenModel: ChatScreenModel = koinInject()
    DisposableEffect(screenModel) {
        onDispose {
            screenModel.onDispose()
        }
    }

    val uiState by screenModel.uiState.collectAsState()
    AlertDialog(
        visible = uiState.settingsDialogOpened,
        modifier = Modifier.fillMaxWidth(.6f),
        onDismissRequest = {
            screenModel.closeSettingsDialog()
        },
        action = {
            WarningButton(onClick = {
                screenModel.resetSettings()
            }) {
                Text(Text.translatable(Texts.SCREEN_CHAT_SETTINGS_RESET))
            }
            GuideButton(onClick = {
                screenModel.closeSettingsDialog()
            }) {
                Text(Text.translatable(Texts.SCREEN_CHAT_SETTINGS_OK))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4)
        ) {
            IntSliderPreferenceItem(
                modifier = Modifier.fillMaxWidth(),
                title = Text.translatable(Texts.SCREEN_CHAT_SETTINGS_LINE_SPACING),
                range = 0 .. 16,
                value = uiState.lineSpacing,
                onValueChanged = {
                    screenModel.updateLineSpacing(it)
                }
            )

            ColorPreferenceItem(
                modifier = Modifier.fillMaxWidth(),
                title = Text.translatable(Texts.SCREEN_CHAT_SETTINGS_TEXT_COLOR),
                value = uiState.textColor,
                onValueChanged = {
                    screenModel.updateTextColor(it)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    BackButton(
                        screenName = Text.translatable(Texts.SCREEN_CHAT_EXIT),
                    )
                },
                title = {
                    Text(Text.translatable(Texts.SCREEN_CHAT_TITLE))
                },
            )
        },
    ) { modifier ->
        Column(
            modifier = modifier,
        ) {
            val messageProvider: ChatMessageProvider = koinInject()
            var messages by remember { mutableStateOf(messageProvider.getMessages()) }
            LaunchedEffect(Unit) {
                while (true) {
                    withFrameMillis { delta ->
                        messages = messageProvider.getMessages()
                    }
                }
            }
            Column(
                modifier = Modifier
                    .verticalScroll(true)
                    .background(Colors.TRANSPARENT_BLACK)
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(uiState.lineSpacing, Alignment.Bottom),
            ) {
                for (message in messages) {
                    Text(message.message, color = uiState.textColor)
                }
            }
            val bottomBarHeight = 32
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomBarHeight),
            ) {
                val focusRequester = remember { FocusRequester() }
                val interactionSource = remember { MutableInteractionSource() }
                var focused by remember { mutableStateOf(false) }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        when (it) {
                            FocusInteraction.Blur -> {
                                focused = false
                            }

                            FocusInteraction.Focus -> {
                                focused = true
                            }
                        }
                    }
                }
                IconButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(bottomBarHeight),
                    focusable = false,
                    onClick = {
                        if (focused) {
                            focusRequester.requestBlur()
                        } else {
                            focusRequester.requestFocus()
                        }
                    },
                ) {
                    Icon(Textures.ICON_CHAT_KEYBOARD)
                }
                IconButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(bottomBarHeight),
                    onClick = {}
                ) {
                    Icon(Textures.ICON_CHAT_COMMAND)
                }
                IconButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(bottomBarHeight),
                    onClick = {
                        screenModel.openSettingsDialog()
                    },
                ) {
                    Icon(Textures.ICON_CHAT_SETTING)
                }
                EditText(
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .weight(1f)
                        .fillMaxHeight(),
                    value = uiState.text,
                    onValueChanged = screenModel::updateText,
                    onEnter = screenModel::sendText,
                )
                IconButton(
                    modifier = Modifier
                        .width(64)
                        .fillMaxHeight(),
                    onClick = screenModel::sendText,
                ) {
                    Icon(Textures.ICON_CHAT_SEND)
                }
            }
        }
    }
}

fun openChatScreen(parent: Any? = null): Any? = with(GlobalContext.get()) {
    val textFactory: TextFactory = get()
    val screenFactory: ScreenFactory = get()
    screenFactory.openScreen(
        renderBackground = false,
        title = textFactory.of(Texts.SCREEN_CHAT_TITLE),
    ) {
        ChatScreen()
    }
}
