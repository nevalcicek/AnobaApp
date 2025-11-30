package com.neval.anoba.letter

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.letter.letteremoji.LetterEmojis

@Composable
fun LetterCard(
    letter: LetterModel,
    navController: NavController,
    currentUserId: String,
    userRole: String,
    isCommentSectionClickable: Boolean = false,
    showContentPreview: Boolean = false
) {
    val topReaction = letter.reactions.maxByOrNull { it.value }
    val context = LocalContext.current
    val isSealed = letter.privacy == "SEALED"
    val dominantEmoji = letter.reactions.maxByOrNull { it.value }?.key
    val cardColor = if (isSealed) {
        LetterEmojis.emojiColors[dominantEmoji] ?: MaterialTheme.colorScheme.surfaceVariant
    } else {
        CardDefaults.cardColors().containerColor
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. BAŞLIK BÖLÜMÜ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = "Sahip",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = if(isSealed) "Gönderen: ${letter.username}" else letter.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSealed) {
                    Text(
                        text = "Alıcı: ${letter.recipientUsername ?: "..."}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            letter.timestamp?.let {
                Text(
                    text = DateTimeUtils.formatRelativeTime(it.toDate().time),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. İÇERİK KARTI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
                .clickable {
                    val canOpen = !isSealed ||
                            currentUserId == letter.ownerId ||
                            currentUserId == letter.recipientId ||
                            userRole == "ADMIN"
                    if (canOpen) {
                        val route = Constants.LETTER_DETAIL_SCREEN.replace("{letterId}", letter.id)
                        navController.navigate(route)
                    } else {
                        Toast.makeText(context, "Bu mühürlü mektubu açma yetkiniz yok.", Toast.LENGTH_SHORT).show()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            if (showContentPreview && !isSealed) {
                // Ana akışta içerik önizlemesi: ilk 3–4 satır
                Text(
                    text = letter.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            } else {
                // İkon gösterimi: ortalanmış ve daha doğal sarı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp), // sabit yükseklik → ortalama garanti
                    contentAlignment = Alignment.Center
                ) {
                    val amberYellow = Color(0xFFFFC107) // Amber 400

                    if (isSealed) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Mühürlü Mektup",
                            modifier = Modifier.size(30.dp),
                            tint = amberYellow
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Herkese Açık Mektup",
                            modifier = Modifier.size(30.dp),
                            tint = amberYellow
                        )
                    }
                }
            }
        }

        // 3. ALT BİLGİ BÖLÜMÜ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (letter.viewCount > 0) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Görüntülenme Sayısı",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = letter.viewCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    )
                }
                if (topReaction != null && topReaction.value > 0) {
                    Text(
                        text = topReaction.key,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = topReaction.value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                    )
                }
            }
            if (letter.commentsCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isCommentSectionClickable) {
                            val route = Constants.LETTER_COMMENTS_ROUTE.replace("{letterId}", letter.id)
                            navController.navigate(route)
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Yorumlar",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${letter.commentsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}