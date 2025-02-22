package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.height
import top.fifthlight.combine.modifier.pointer.draggable
import top.fifthlight.combine.paint.drawNinePatchTexture
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.ui.style.TextureSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures

data class SliderTextureSet(
    val activeTrack: NinePatchTextureSet,
    val inactiveTrack: NinePatchTextureSet,
    val handle: TextureSet,
)

val defaultSliderTexture = SliderTextureSet(
    activeTrack = NinePatchTextureSet(
        normal = Textures.WIDGET_SLIDER_SLIDER_ACTIVE,
        focus = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_HOVER,
        hover = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_HOVER,
        active = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_ACTIVE,
        disabled = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_DISABLED,
    ),
    inactiveTrack = NinePatchTextureSet(
        normal = Textures.WIDGET_SLIDER_SLIDER_INACTIVE,
        focus = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_HOVER,
        hover = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_HOVER,
        active = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_ACTIVE,
        disabled = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_DISABLED,
    ),
    handle = TextureSet(
        normal = Textures.WIDGET_HANDLE_HANDLE,
        focus = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        hover = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        active = Textures.WIDGET_HANDLE_HANDLE_ACTIVE,
        disabled = Textures.WIDGET_HANDLE_HANDLE_DISABLED,
    ),
)

val LocalSliderTexture = staticCompositionLocalOf<SliderTextureSet> { defaultSliderTexture }

@Composable
fun IntSlider(
    modifier: Modifier,
    textureSet: SliderTextureSet = LocalSliderTexture.current,
    range: IntRange,
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    fun Int.toProgress() = (this - range.first).toFloat() / (range.last - range.first)
    fun Float.toValue() = (this * (range.last - range.first)).toInt() + range.first

    Slider(
        modifier = modifier,
        textureSet = textureSet,
        range = 0f..1f,
        value = value.toProgress(),
        onValueChanged = {
            onValueChanged(it.toValue())
        },
    )
}

@Composable
fun Slider(
    modifier: Modifier,
    textureSet: SliderTextureSet = LocalSliderTexture.current,
    range: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChanged: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)

    fun Float.toValue() = this * (range.endInclusive - range.start) + range.start
    fun Float.toProgress() = (this - range.start) / (range.endInclusive - range.start)

    val progress = value.toProgress()

    val handleTexture = textureSet.handle.getByState(state)
    val handleLeftHalfWidth = handleTexture.size.width / 2

    Canvas(
        modifier = Modifier
            .height(height = 16)
            .draggable(interactionSource) { _, absolute ->
                val rawProgress = (absolute.x - handleLeftHalfWidth) / (size.width - handleTexture.size.width)
                val newProgress = rawProgress.coerceIn(0f, 1f)
                onValueChanged(newProgress.toValue())
            }
            .focusable(interactionSource)
            .then(modifier),
    ) { node ->
        with(canvas) {
            val trackRect = IntRect(
                offset = IntOffset(
                    x = handleLeftHalfWidth,
                    y = 0
                ),
                size = IntSize(
                    width = node.width - handleTexture.size.width,
                    height = node.height
                )
            )
            val progressWidth = trackRect.size.width * progress
            drawNinePatchTexture(
                texture = textureSet.activeTrack.getByState(state),
                dstRect = trackRect,
            )

            drawNinePatchTexture(
                texture = textureSet.inactiveTrack.getByState(state),
                dstRect = IntRect(
                    offset = trackRect.offset + IntOffset(
                        x = progressWidth.toInt(),
                        y = 0,
                    ),
                    size = IntSize(
                        width = trackRect.size.width - progressWidth.toInt(),
                        height = trackRect.size.height,
                    )
                ),
            )

            drawTexture(
                texture = handleTexture,
                dstRect = Rect(
                    offset = Offset(
                        x = progressWidth,
                        y = 0f,
                    ),
                    size = handleTexture.size.toSize(),
                )
            )
        }
    }
}