package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.model.ChannelColor

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
    Column(modifier = modifier.fillMaxWidth()) {
        AppHeader()

        Row(
            modifier = Modifier
                .fillMaxWidth()
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

            Text(
                text = pilotName,
                fontSize = textSize,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
                    .clickable(onClick = onNameClick)
            )
        }
    }
}
