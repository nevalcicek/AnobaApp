package com.neval.anoba.photo.photoemoji

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotoEmojiReactionRow(
    reactions: Map<String, Long>,
    selectedEmoji: String?,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Emoji listesini merkezi PhotoEmojis nesnesinden alıyoruz.
    val allEmojis = (reactions.keys + PhotoEmojis.list).distinct().filter { it.isNotBlank() }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = allEmojis,
            key = { emoji -> emoji }
        ) { emoji ->
            val count = reactions[emoji] ?: 0L
            val isSelected = selectedEmoji == emoji
            // Her emoji için ilgili arka plan rengini alıyoruz.
            val baseBackgroundColor = PhotoEmojis.emojiColors.getOrDefault(emoji, Color.Transparent)

            EmojiChip(emoji, count, isSelected, baseBackgroundColor, onReact)
        }
    }
}

@Composable
private fun EmojiChip(
    emoji: String,
    count: Long,
    isSelected: Boolean,
    baseBackgroundColor: Color,
    onReact: (String) -> Unit
) {
    val selectedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val selectedBorderColor = MaterialTheme.colorScheme.primary

    // Seçiliyse farklı, değilse haritadan gelen rengi kullanıyoruz.
    val finalBackgroundColor = if (isSelected) selectedBackgroundColor else baseBackgroundColor
    val finalBorderColor = if (isSelected) selectedBorderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(finalBackgroundColor)
            .border(
                width = 1.dp,
                color = finalBorderColor,
                shape = CircleShape
            )
            .clickable { onReact(emoji) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        if (count > 0) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
