package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.common.ui.state.ChatScreenState

class ChatScreenModel : TouchControllerScreenModel() {
    private val configHolder: GlobalConfigHolder by inject()
    private val chatMessageProvider: ChatMessageProvider by inject()
    private val _uiState = MutableStateFlow(ChatScreenState())
    val uiState = _uiState.asStateFlow()

    init {
        coroutineScope.launch {
            configHolder.config.collect { config ->
                _uiState.getAndUpdate {
                    it.copy(
                        lineSpacing = config.chat.lineSpacing,
                        textColor = config.chat.textColor,
                    )
                }
            }
        }
    }

    fun updateText(newText: String) {
        _uiState.getAndUpdate { it.copy(text = newText) }
    }

    fun sendText() {
        chatMessageProvider.sendMessage(uiState.value.text)
        updateText("")
    }

    fun openSettingsDialog() {
        _uiState.getAndUpdate { it.copy(settingsDialogOpened = true) }
    }

    fun closeSettingsDialog() {
        _uiState.getAndUpdate { it.copy(settingsDialogOpened = false) }
    }

    fun resetSettings() {
        _uiState.getAndUpdate {
            it.copy(
                lineSpacing = 0,
                textColor = Colors.WHITE,
            )
        }
    }

    fun updateLineSpacing(lineSpacing: Int) {
        configHolder.updateConfig {
            copy(chat = chat.copy(lineSpacing = lineSpacing))
        }
    }

    fun updateTextColor(textColor: Color) {
        configHolder.updateConfig {
            copy(chat = chat.copy(textColor = textColor))
        }
    }
}