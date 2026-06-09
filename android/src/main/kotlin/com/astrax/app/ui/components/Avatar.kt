package com.astrax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val avatarColors = listOf(
    Color(0xFF33ABF7),
    Color(0xFF31C76A),
    Color(0xFFFF9E44),
    Color(0xFFE85D75),
    Color(0xFF7C6AF2),
    Color(0xFF19B6A8)
)

@Composable
fun Avatar(name: String, size: Dp = 48.dp) {
    val color = avatarColors[(name.hashCode().toUInt().toInt() and Int.MAX_VALUE) % avatarColors.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
