package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalContentColor.current,
    strokeColor: Color = if (fillColor.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color.White.copy(alpha = 0.35f)
    },
    strokeWidth: Float = 8f,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null
) {
    OutlinedText(
        text = AnnotatedString(text),
        modifier = modifier,
        fillColor = fillColor,
        strokeColor = strokeColor,
        strokeWidth = strokeWidth,
        style = style,
        textAlign = textAlign
    )
}

@Composable
fun OutlinedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalContentColor.current,
    strokeColor: Color = if (fillColor.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color.White.copy(alpha = 0.35f)
    },
    strokeWidth: Float = 8f,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = style.copy(
                color = strokeColor,
                drawStyle = Stroke(
                    width = strokeWidth,
                    join = StrokeJoin.Round
                )
            ),
            textAlign = textAlign
        )
        Text(
            text = text,
            style = style.copy(color = fillColor),
            textAlign = textAlign
        )
    }
}
