package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.model.ChannelColor

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrainingHeader(
    channelLetter: String,
    channelNumber: Int,
    channelColor: ChannelColor,
    pilotName: String,
    onChannelClick: () -> Unit = {},
    onNameClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasOutline = channelColor.outlineColor != null
        val textSize = 24.sp
        val channelFont = FontFamily.Monospace

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = channelColor.color,
            border = if (hasOutline) BorderStroke(2.dp, channelColor.outlineColor!!) else null,
            modifier = Modifier.clickable(onClick = onChannelClick)
        ) {
            Text(
                text = "$channelLetter$channelNumber",
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = channelFont,
                color = if (channelColor == ChannelColor.WHITE) Color.Black else Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(onLongClick = onNameClick) {},
            contentAlignment = Alignment.CenterStart
        ) {
            val textModifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            if (pilotName.isBlank()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                            append("Laps")
                        }
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(".Trainer")
                        }
                    },
                    fontSize = textSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = textModifier
                )
            } else {
                Text(
                    text = pilotName,
                    fontSize = textSize,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = textModifier
                )
            }
        }
    }
}
