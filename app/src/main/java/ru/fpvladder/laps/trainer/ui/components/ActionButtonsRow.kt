package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.fpvladder.laps.trainer.R

@Composable
fun ActionButtonsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = { },
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_triangle),
                contentDescription = "Triangle",
                modifier = Modifier.size(32.dp)
            )
        }
        FilledIconButton(
            onClick = { },
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cross),
                contentDescription = "Cross",
                modifier = Modifier.size(32.dp)
            )
        }
        FilledIconButton(
            onClick = { },
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_square),
                contentDescription = "Square",
                modifier = Modifier.size(32.dp)
            )
        }
        FilledIconButton(
            onClick = { },
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_circle),
                contentDescription = "Circle",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
