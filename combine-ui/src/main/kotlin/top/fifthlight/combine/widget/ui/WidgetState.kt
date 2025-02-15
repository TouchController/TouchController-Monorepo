package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.InteractionSource
import top.fifthlight.combine.modifier.focus.FocusInteraction
import top.fifthlight.combine.modifier.pointer.ClickInteraction
import top.fifthlight.combine.ui.style.NinePatchTextureSet
import top.fifthlight.combine.ui.style.TextureSet

internal enum class WidgetState {
    NORMAL,
    FOCUS,
    HOVER,
    ACTIVE,
}

internal fun TextureSet.getByState(state: WidgetState, disabled: Boolean = false) = if (disabled) {
    this.disabled
} else {
    when (state) {
        WidgetState.NORMAL -> normal
        WidgetState.HOVER -> hover
        WidgetState.ACTIVE -> active
        WidgetState.FOCUS -> focus
    }
}

internal fun NinePatchTextureSet.getByState(state: WidgetState, disabled: Boolean = false) = if (disabled) {
    this.disabled
} else {
    when (state) {
        WidgetState.NORMAL -> normal
        WidgetState.HOVER -> hover
        WidgetState.ACTIVE -> active
        WidgetState.FOCUS -> focus
    }
}

@Composable
internal fun widgetState(interactionSource: InteractionSource): State<WidgetState> {
    var state = remember { mutableStateOf(WidgetState.NORMAL) }
    var lastClickInteraction by remember { mutableStateOf<ClickInteraction>(ClickInteraction.Empty) }
    var lastFocusInteraction by remember { mutableStateOf<FocusInteraction>(FocusInteraction.Blur) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is ClickInteraction) {
                lastClickInteraction = it
            }
            if (it is FocusInteraction) {
                lastFocusInteraction = it
            }
            state.value = when (lastClickInteraction) {
                ClickInteraction.Active -> WidgetState.ACTIVE
                ClickInteraction.Hover -> WidgetState.HOVER
                ClickInteraction.Empty -> when (lastFocusInteraction) {
                    FocusInteraction.Blur -> WidgetState.NORMAL
                    FocusInteraction.Focus -> WidgetState.FOCUS
                }
            }
        }
    }
    return state
}