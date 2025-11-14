package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.util.LocalCloseHandler
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.combine.widget.ui.TextButton
import top.fifthlight.touchcontroller.assets.Texts

val LocalCloseHandler = staticCompositionLocalOf<CloseHandler> { CloseHandler.Empty }

interface CloseHandler {
    fun close()

    object Empty: CloseHandler {
        override fun close() {}
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    screenName: Text,
    onClick: (() -> Unit)? = null,
) {
    val closeHandler = LocalCloseHandler.current
    val navigator = LocalNavigator.current
    TextButton(
        modifier = modifier,
        onClick = {
            if (onClick != null) {
                onClick()
            } else {
                if (navigator?.pop() != true) {
                    closeHandler.close()
                }
            }
        }
    ) {
        Text(Text.format(Texts.BACK, screenName.string))
    }
}