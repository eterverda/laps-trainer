package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.usb.UsbHidAction

@Composable
fun UsbHidActionIcon(
    action: UsbHidAction,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    when (action) {
        UsbHidAction.START -> Icon(painterResource(R.drawable.ic_triangle), null, modifier, tint)
        UsbHidAction.LAP -> Icon(painterResource(R.drawable.ic_circle), null, modifier, tint)
        UsbHidAction.ERROR -> Icon(painterResource(R.drawable.ic_cross), null, modifier, tint)
        UsbHidAction.FIX -> Icon(painterResource(R.drawable.ic_square), null, modifier, tint)
        UsbHidAction.UNDO -> Icon(painterResource(R.drawable.ic_backspace), null, modifier, tint)
    }
}
