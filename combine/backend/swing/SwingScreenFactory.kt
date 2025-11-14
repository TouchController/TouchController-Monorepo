package top.fifthlight.combine.backend.swing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.Dispatchers
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.input.key.KeyEvent
import top.fifthlight.combine.input.pointer.PointerButton
import top.fifthlight.combine.input.pointer.PointerEvent
import top.fifthlight.combine.input.pointer.PointerEventType
import top.fifthlight.combine.input.pointer.PointerType
import top.fifthlight.combine.input.text.LocalClipboard
import top.fifthlight.combine.node.CombineOwner
import top.fifthlight.combine.node.LocalInputHandler
import top.fifthlight.combine.screen.LocalOnDismissRequestDispatcher
import top.fifthlight.combine.screen.LocalScreenFactory
import top.fifthlight.combine.screen.OnDismissRequestDispatcher
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset
import java.awt.Graphics2D
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlin.coroutines.CoroutineContext

private class CombineFrame(title: String): JFrame(title) {
    private var initialized = false
    private val dispatcher = Dispatchers.Main
    private val dismissDispatcher = OnDismissRequestDispatcher()
    private val canvas = SwingCanvas(graphics as Graphics2D)

    private val owner = CombineOwner(dispatcher = dispatcher, textMeasurer = SwingTextMeasurer(graphics))
    override val coroutineContext: CoroutineContext
        get() = owner.coroutineContext

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                if (dismissDispatcher.hasEnabledCallbacks()) {
                    dismissDispatcher.dispatchOnDismissed()
                } else {
                    dispose()
                }
            }
        })
    }

    fun setContent(content: @Composable () -> Unit) {
        owner.setContent {
            CompositionLocalProvider(
                LocalTextFactory provides TextFactoryImpl,
                LocalClipboard provides ClipboardHandlerImpl,
                LocalScreenFactory provides SwingScreenFactory,
                LocalOnDismissRequestDispatcher provides dismissDispatcher,
                LocalInputHandler provides InputManager,
            ) {
                content()
            }
        }
    }

    override fun init() {
        super.init()
        if (!initialized) {
            initialized = true
            owner.start()
        }
    }

    private fun mapMouseButton(button: Int) = when (button) {
        0 -> PointerButton.Left
        1 -> PointerButton.Middle
        2 -> PointerButton.Right
        else -> null
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseButton = mapMouseButton(button) ?: return true
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = mouseButton,
                type = PointerEventType.Press
            )
        )
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseButton = mapMouseButton(button) ?: return true
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = mouseButton,
                type = PointerEventType.Release
            )
        )
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = null,
                type = PointerEventType.Move
            )
        )
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double,
    ): Boolean {
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = null,
                scrollDelta = Offset(
                    x = 0f,
                    y = amount.toFloat(),
                ),
                type = PointerEventType.Scroll
            )
        )
        return true
    }

    override fun charTyped(char: Char, modifiers: Int): Boolean {
        owner.onTextInput(char.toString())
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (dismissDispatcher.hasEnabledCallbacks()) {
                dismissDispatcher.dispatchOnDismissed()
            } else {
                onClose()
            }
            return true
        }
        owner.onKeyEvent(
            KeyEvent(
                key = mapKeyCode(keyCode),
                modifier = mapModifier(modifiers),
                pressed = true
            )
        )
        return true
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        owner.onKeyEvent(
            KeyEvent(
                key = mapKeyCode(keyCode),
                modifier = mapModifier(modifiers),
                pressed = false
            )
        )
        return true
    }

    override fun render(martices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        if (renderBackground) {
            this.renderBackground(martices)
        }

        val canvas = CanvasImpl(martices)
        val size = IntSize(width, height)
        RenderSystem.enableBlend()
        owner.render(size, canvas)
    }

    override fun dispose() {
        owner.close()
        super.dispose()
    }
}


object SwingScreenFactory : ScreenFactory {
    override fun openScreen(
        renderBackground: Boolean,
        title: Text,
        content: @Composable () -> Unit
    ) {
        val screen = getScreen(null, renderBackground, title, content)

    }

    override fun getScreen(
        parent: Any?,
        renderBackground: Boolean,
        title: Text,
        content: @Composable () -> Unit
    ): Any {
        val screen = CombineFrame(title.string)
        screen.setContent {
            content()
        }
        return screen
    }
}