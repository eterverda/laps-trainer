package ru.fpvladder.laps.lite.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import ru.fpvladder.laps.lite.model.ChannelColor
import ru.fpvladder.laps.lite.model.Pilot

@Composable
fun PilotSection(
    pilot: Pilot,
    editable: Boolean = false,
    onChannelClick: () -> Unit = {},
    onNameClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val channelColor = pilot.channelColor
        val hasOutline = channelColor.outlineColor != null
        val textSize = 24.sp
        val channelFont = FontFamily.Monospace

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = channelColor.color,
            border = if (hasOutline) BorderStroke(2.dp, channelColor.outlineColor!!) else null,
            modifier = if (editable) {
                Modifier.clickable(onClick = onChannelClick)
            } else Modifier
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${pilot.channelLetter}${pilot.channelNumber}",
                    fontSize = textSize,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = channelFont,
                    color = if (channelColor == ChannelColor.WHITE) Color.Black else Color.White,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = if (editable) {
                Modifier
                    .clickable(onClick = onNameClick)
                    .weight(1f)
            } else Modifier
                .weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = pilot.name,
                fontSize = textSize,
                fontWeight = FontWeight.Normal,
                fontFamily = null,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
