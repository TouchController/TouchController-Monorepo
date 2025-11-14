package top.fifthlight.touchcontroller.common.ui.state

import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors

data class ChatScreenState(
    val text: String = "",
    val lineSpacing: Int = 0,
    val textColor: Color = Colors.WHITE,
    val settingsDialogOpened: Boolean = false,
)
