package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.ui.style.LocalTextStyle
import top.fifthlight.combine.ui.style.TextStyle
import top.fifthlight.combine.widget.base.BaseText

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalColorTheme.current.foreground,
    textStyle: TextStyle = LocalTextStyle.current,
) = BaseText(
    text = text,
    modifier = modifier,
    color = color,
    shadow = textStyle.shadow,
)

@Composable
fun Text(
    text: Text,
    modifier: Modifier = Modifier,
    color: Color = LocalColorTheme.current.foreground,
    textStyle: TextStyle = LocalTextStyle.current,
) = BaseText(
    text = text,
    modifier = modifier,
    color = color,
    shadow = textStyle.shadow,
)
