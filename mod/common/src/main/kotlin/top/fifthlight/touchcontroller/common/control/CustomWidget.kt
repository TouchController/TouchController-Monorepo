package top.fifthlight.touchcontroller.common.control

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.combine.paint.drawNinePatchTexture
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.layout.*
import kotlin.uuid.Uuid

@Serializable
@SerialName("custom")
data class CustomWidget(
    val normalTexture: ButtonTexture = ButtonTexture.NinePatch(extraPadding = IntPadding(8)),
    val activeTexture: ButtonActiveTexture = ButtonActiveTexture.Same,
    val centerText: String? = "",
    val textColor: Color = Colors.BLACK,
    val swipeTrigger: Boolean = false,
    val grabTrigger: Boolean = false,
    val action: ButtonTrigger = ButtonTrigger(),
    override val id: Uuid = fastRandomUuid(),
    override val name: Name = Name.Translatable(Texts.WIDGET_CUSTOM_BUTTON_NAME),
    override val align: Align = Align.RIGHT_BOTTOM,
    override val offset: IntOffset = IntOffset.ZERO,
    override val opacity: Float = 1f,
    override val lockMoving: Boolean = false,
) : ControllerWidget(), KoinComponent {
    private val textMeasurer: TextMeasurer by inject()

    companion object : KoinComponent {
        private val textFactory: TextFactory by inject()

        @Suppress("UNCHECKED_CAST")
        private val _properties = properties + persistentListOf<Property<CustomWidget, *>>(
            BooleanProperty(
                getValue = { it.swipeTrigger },
                setValue = { config, value ->
                    config.copy(swipeTrigger = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_SWIPE_TRIGGER),
            ),
            BooleanProperty(
                getValue = { it.grabTrigger },
                setValue = { config, value ->
                    config.copy(grabTrigger = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_GRAB_TRIGGER),
            ),
            StringProperty(
                getValue = { it.centerText ?: "" },
                setValue = { config, value ->
                    config.copy(centerText = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_CENTER_TEXT),
            ),
            ColorProperty(
                getValue = { it.textColor },
                setValue = { config, value ->
                    config.copy(textColor = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_TEXT_COLOR),
            ),
            ButtonTextureProperty(
                getValue = { it.normalTexture },
                setValue = { config, value ->
                    config.copy(normalTexture = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_NORMAL_TEXTURE),
            ),
            ButtonActiveTextureProperty(
                getValue = { it.activeTexture },
                setValue = { config, value ->
                    config.copy(activeTexture = value)
                },
                name = textFactory.of(Texts.WIDGET_CUSTOM_BUTTON_ACTIVE_TEXTURE),
            ),
            ButtonTriggerProperty(
                getValue = { it.action },
                setValue = { config, value ->
                    config.copy(action = value)
                },
            ),
        ) as PersistentList<Property<ControllerWidget, *>>
    }

    override val properties
        get() = _properties

    private fun ButtonTexture.getSize(): Pair<IntSize, IntOffset> {
        fun measureCenterText() = centerText?.takeIf { it.isNotEmpty() }?.let(textMeasurer::measure) ?: IntSize.ZERO
        return when (val texture = this) {
            is ButtonTexture.Empty -> {
                val textSize = measureCenterText()
                Pair(
                    textSize + texture.extraPadding,
                    texture.extraPadding.leftTopOffset,
                )
            }

            is ButtonTexture.Fill -> {
                val textSize = measureCenterText()
                Pair(
                    textSize + texture.extraPadding + texture.borderWidth * 2,
                    texture.extraPadding.leftTopOffset + texture.borderWidth,
                )
            }

            is ButtonTexture.Fixed -> {
                val textSize = measureCenterText()
                val buttonSize = (texture.texture.texture.size.toSize() * texture.scale).toIntSize()
                Pair(
                    buttonSize,
                    buttonSize - textSize,
                )
            }

            is ButtonTexture.NinePatch -> {
                val ninePatch = texture.texture.texture
                val textSize = measureCenterText()
                Pair(
                    textSize + ninePatch.padding + texture.extraPadding,
                    ninePatch.padding.leftTopOffset + texture.extraPadding.leftTopOffset,
                )
            }
        }
    }

    private val size by lazy { normalTexture.getSize().first }

    override fun size(): IntSize = size

    private fun Context.ButtonContent(clicked: Boolean) {
        var grayTexture = false
        val active = clicked || action.hasLock(id)
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
                val (_, textOffset) = buttonTexture.getSize()
                renderText?.let { text ->
                    drawQueue.enqueue { canvas ->
                        canvas.drawText(
                            offset = textOffset,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.Fill -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (textureSize, textOffset) = buttonTexture.getSize()
                drawQueue.enqueue { canvas ->
                    canvas.fillRect(
                        size = textureSize,
                        color = buttonTexture.borderColor,
                    )
                    canvas.fillRect(
                        offset = IntOffset(buttonTexture.borderWidth),
                        size = textureSize - buttonTexture.borderWidth * 2,
                        color = buttonTexture.backgroundColor,
                    )
                    renderText?.let { text ->
                        canvas.drawText(
                            offset = textOffset,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.Fixed -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (_, textOffset) = buttonTexture.getSize()
                Texture(texture = buttonTexture.texture.texture, tint = tint)
                renderText?.let { text ->
                    drawQueue.enqueue { canvas ->
                        canvas.drawText(
                            offset = textOffset / 2,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }

            is ButtonTexture.NinePatch -> {
                val renderText = centerText?.takeIf { it.isNotEmpty() }
                val (textureSize, textOffset) = buttonTexture.getSize()
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
                            offset = textOffset,
                            text = text,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }

    override fun layout(context: Context) {
        context.status.doubleClickCounter.update(context.timer.renderTick, id)
        val buttonResult = if (swipeTrigger) {
            context.SwipeButton(id) { clicked ->
                ButtonContent(clicked)
            }
        } else {
            context.Button(
                id = id,
                grabTrigger = grabTrigger,
            ) { clicked ->
                ButtonContent(clicked)
            }
        }
        action.refresh(context, id)
        action.trigger(context, buttonResult, id)
    }

    override fun cloneBase(
        id: Uuid,
        name: Name,
        align: Align,
        offset: IntOffset,
        opacity: Float,
        lockMoving: Boolean
    ) = copy(
        id = id,
        name = name,
        align = align,
        offset = offset,
        opacity = opacity,
        lockMoving = lockMoving,
    )
}