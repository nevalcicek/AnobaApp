package com.neval.anoba.chat.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun GroupInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initials = remember(name) {
        name.split(' ')
            .filter { it.isNotBlank() }
            .take(2).joinToString("") { it.first().uppercase() }
    }

    val backgroundColor = remember(name) {
        generateStableColor(name)
    }

    Box(
        modifier = modifier
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun generateStableColor(name: String): Color {
    val hash = name.hashCode()
    val hue = abs(hash % 360).toFloat()
    return Color.hsv(hue, 0.5f, 0.8f)
}
