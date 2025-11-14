package top.fifthlight.touchcontroller.common.input

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.input.input.InputHandler
import top.fifthlight.combine.input.input.TextInputState
import top.fifthlight.data.IntRect
import top.fifthlight.touchcontroller.common.event.RenderEvents
import top.fifthlight.touchcontroller.common.gal.GameDispatcher
import top.fifthlight.touchcontroller.common.gal.WindowHandle
import top.fifthlight.touchcontroller.common.platform.PlatformProvider
import top.fifthlight.touchcontroller.proxy.client.PlatformCapability
import top.fifthlight.touchcontroller.proxy.message.FloatRect
import top.fifthlight.touchcontroller.proxy.message.InputAreaMessage
import top.fifthlight.touchcontroller.proxy.message.InputCursorMessage
import top.fifthlight.touchcontroller.proxy.message.InputStatusMessage
import top.fifthlight.touchcontroller.proxy.message.KeyboardShowMessage
import top.fifthlight.touchcontroller.proxy.message.input.TextInputState as ProxyTextInputState
import top.fifthlight.touchcontroller.proxy.message.input.TextRange as ProxyTextRange

object InputManager : KoinComponent, InputHandler {
    private val platformProvider: PlatformProvider by inject()
    private var inputState: TextInputState? = null
    private var cursorRect: IntRect? = null
    private var areaRect: IntRect? = null
    private val windowHandle: WindowHandle by inject()
    private val _events = MutableSharedFlow<TextInputState>()
    override val events = _events.asSharedFlow()
    private val gameDispatcher: GameDispatcher by inject()
    private val scope = CoroutineScope(SupervisorJob() + gameDispatcher)

    fun updateNativeState(textInputState: TextInputState) {
        inputState = textInputState
        scope.launch {
            _events.emit(textInputState)
        }
    }

    override fun updateInputState(textInputState: TextInputState?, cursorRect: IntRect?, areaRect: IntRect?) {
        val inputStateUpdated = inputState != textInputState
        val cursorRectUpdated = cursorRect != this.cursorRect
        val areaRectUpdated = areaRect != this.areaRect
        this.inputState = textInputState
        this.cursorRect = cursorRect
        this.areaRect = areaRect
        if (PlatformCapability.TEXT_STATUS in RenderEvents.platformCapabilities) {
            platformProvider.platform?.let { platform ->
                if (inputStateUpdated) {
                    platform.sendEvent(InputStatusMessage(textInputState?.let {
                        ProxyTextInputState(
                            text = textInputState.text,
                            composition = ProxyTextRange(
                                start = textInputState.composition.start,
                                length = textInputState.composition.length,
                            ),
                            selection = ProxyTextRange(
                                start = textInputState.selection.start,
                                length = textInputState.selection.length,
                            ),
                            selectionLeft = textInputState.selectionLeft,
                        )
                    }))
                }
                if (cursorRectUpdated) {
                    platform.sendEvent(
                        InputCursorMessage(
                            cursorRect?.let { rect ->
                                FloatRect(
                                    left = rect.offset.left.toFloat() / windowHandle.scaledSize.width.toFloat(),
                                    top = rect.offset.top.toFloat() / windowHandle.scaledSize.height.toFloat(),
                                    width = rect.size.width.toFloat() / windowHandle.scaledSize.width.toFloat(),
                                    height = rect.size.height.toFloat() / windowHandle.scaledSize.height.toFloat(),
                                )
                            }
                        )
                    )
                }
                if (areaRectUpdated) {
                    platform.sendEvent(
                        InputAreaMessage(
                            areaRect?.let { rect ->
                                FloatRect(
                                    left = rect.offset.left.toFloat() / windowHandle.scaledSize.width.toFloat(),
                                    top = rect.offset.top.toFloat() / windowHandle.scaledSize.height.toFloat(),
                                    width = rect.size.width.toFloat() / windowHandle.scaledSize.width.toFloat(),
                                    height = rect.size.height.toFloat() / windowHandle.scaledSize.height.toFloat(),
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    override fun tryShowKeyboard() {
        platformProvider.platform?.let { platform ->
            if (PlatformCapability.KEYBOARD_SHOW in RenderEvents.platformCapabilities) {
                platform.sendEvent(KeyboardShowMessage(true))
            }
        }
    }

    override fun tryHideKeyboard() {
        platformProvider.platform?.let { platform ->
            if (PlatformCapability.KEYBOARD_SHOW in RenderEvents.platformCapabilities) {
                platform.sendEvent(KeyboardShowMessage(false))
            }
        }
    }
}
