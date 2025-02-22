package top.fifthlight.touchcontroller.control

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.combine.paint.drawNinePatchTexture
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.layout.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("custom")
data class CustomWidget(
    val normalTexture: ButtonTexture,
    val activeTexture: ButtonActiveTexture,
    val centerText: String? = null,
    val textColor: Color = Colors.BLACK,
    val swipeTrigger: Boolean = false,
    val action: ButtonTrigger = ButtonTrigger.Press(),
    override val id: Uuid = Uuid.random(),
    override val align: Align = Align.RIGHT_BOTTOM,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget(), KoinComponent {
    private val textMeasurer: TextMeasurer by inject()

    override val properties
        get() = super.properties

    private fun ButtonTexture.getSize(): Pair<IntSize, IntSize> {
        fun measureCenterText() = centerText?.takeIf { it.isNotEmpty() }?.let(textMeasurer::measure) ?: IntSize.ZERO
        return when (val texture = this) {
            is ButtonTexture.Empty -> {
                val textSize = measureCenterText()
                Pair(textSize + texture.extraPadding, textSize)
            }

            is ButtonTexture.Fill -> {
                val textSize = measureCenterText()
                Pair(textSize + texture.extraPadding + texture.borderWidth * 2, textSize)
            }

            is ButtonTexture.Fixed -> Pair(
                (texture.texture.texture.size.toSize() * texture.scale).toIntSize(),
                IntSize.ZERO
            )

            is ButtonTexture.NinePatch -> {
                val ninePatch = texture.texture.texture
                val textSize = measureCenterText()
                Pair(textSize + ninePatch.padding + texture.extraPadding, textSize)
            }
        }
    }

    private val size by lazy { normalTexture.getSize().first }

    override fun size(): IntSize = size

    private fun Context.ButtonContent(clicked: Boolean) {
        var grayTexture = false
        val active = when (action) {
            is ButtonTrigger.Press -> clicked
            is ButtonTrigger.Lock -> keyBindingHandler.getState(action.key).locked
        }
        val buttonTexture = if (active) {
            when (val activeTexture = activeTexture) {
                ButtonActiveTexture.Same -> normalTexture
                ButtonActiveTexture.Gray -> {
                    grayTexture = true
                    normalTexture
                }

                is ButtonActiveTexture.Texture -> activeTexture.texture
            }
        } else {
            normalTexture
        }
        val tint = if (grayTexture) {
            Color(0xFFAAAAAAu)
        } else {
            Color(0xFFFFFFFFu)
        }
        when (buttonTexture) {
            is ButtonTexture.Empty -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (textureSize, textSize) = buttonTexture.getSize()
                renderText?.let { text ->
                    drawQueue.enqueue { canvas ->
                        canvas.drawText(
                            offset = (textureSize - textSize) / 2,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.Fill -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (textureSize, textSize) = buttonTexture.getSize()
                drawQueue.enqueue { canvas ->
                    canvas.fillRect(
                        size = textureSize,
                        color = buttonTexture.borderColor,
                    )
                    canvas.fillRect(
                        offset = IntOffset(buttonTexture.borderWidth),
                        size = textureSize - buttonTexture.borderWidth * 2,
                        color = buttonTexture.borderColor,
                    )
                    renderText?.let { text ->
                        canvas.drawText(
                            offset = (textureSize - textSize) / 2,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.Fixed -> {
                val texture = buttonTexture.texture.texture
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val textSize = renderText?.let(textMeasurer::measure) ?: IntSize.ZERO
                Texture(texture = texture, tint = tint)
                renderText?.let { text ->
                    drawQueue.enqueue { canvas ->
                        canvas.drawText(
                            offset = (texture.size - textSize) / 2,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.NinePatch -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (textureSize, textSize) = buttonTexture.getSize()

                drawQueue.enqueue { canvas ->
                    canvas.drawNinePatchTexture(
                        texture = buttonTexture.texture.texture,
                        dstRect = IntRect(
                            offset = IntOffset.ZERO,
                            size = textureSize
                        ),
                        tint = tint,
                    )
                    renderText?.let { text ->
                        canvas.drawText(
                            offset = (textureSize - textSize) / 2,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }

    override fun layout(context: Context) {
        val (newPointer, clicked, release) = if (swipeTrigger) {
            context.SwipeButton(id) { clicked ->
                ButtonContent(clicked)
            }
        } else {
            context.Button(id) { clicked ->
                ButtonContent(clicked)
            }
        }
        when (action) {
            is ButtonTrigger.Press -> {
                if (newPointer) {
                    context.pendingAction.add(action.down)
                }
                if (clicked) {
                    action.press?.let { keyType ->
                        context.keyBindingHandler.getState(keyType).clicked = true
                    }
                }
                if (release) {
                    context.pendingAction.add(action.up)
                }
            }

            is ButtonTrigger.Lock -> {
                when (action.method) {
                    ButtonTrigger.Lock.LockMethod.SINGLE_CLICK -> {
                        TODO()
                    }

                    ButtonTrigger.Lock.LockMethod.DOUBLE_CLICK -> {
                        TODO()
                    }
                }
            }
        }
    }

    override fun cloneBase(
        id: Uuid,
        align: Align,
        offset: IntOffset,
        opacity: Float,
        lockMoving: Boolean
    ) = copy(
        align = align,
        offset = offset,
        opacity = opacity,
        lockMoving = lockMoving,
    )
}