package com.astrax.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.astrax.app.data.MessageStatus

@Composable
fun MessageStatusIcon(status: MessageStatus, modifier: Modifier = Modifier, tint: Color = Color.White.copy(alpha = 0.86f)) {
    val icon = when (status) {
        MessageStatus.sending -> Icons.Default.Schedule
        MessageStatus.sent -> Icons.Default.Check
        MessageStatus.read -> Icons.Default.DoneAll
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(16.dp),
        tint = tint
    )
}
