package com.astrax.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeText(timestamp: Long, modifier: Modifier = Modifier) {
    Text(
        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
