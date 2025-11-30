package com.neval.anoba.ses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Composable
fun SesCard(
    ses: SesModel,
    navController: NavController,
    canDelete: Boolean,
    onDeleteClicked: () -> Unit,
    isCommentSectionClickable: Boolean = false,
    showPlayIcon: Boolean = true
) {
    val topReaction = ses.reactions.maxByOrNull { it.value }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // BAŞLIK BÖLÜMÜ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Sahip", modifier = Modifier.size(32.dp))
            Text(
                text = ses.username.ifBlank { "Anonim" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            ses.timestamp?.let {
                Text(
                    text = DateTimeUtils.formatRelativeTime(it.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canDelete) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Daha fazla")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            onClick = {
                                onDeleteClicked()
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // ANA KART
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Constants.SES_DETAIL_SCREEN.replace("{sesId}", ses.id)) },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.7f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val random = remember(ses.id) { Random(ses.id.hashCode()) }
                    repeat(60) {
                        Spacer(
                            modifier = Modifier
                                .height(random.nextInt(15, 70).dp)
                                .width(3.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                if (showPlayIcon) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = "Ses Kaydını Oynat",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = formatDuration(ses.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // ALT BİLGİ BÖLÜMÜ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ses.viewCount > 0) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Görüntülenme Sayısı",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ses.viewCount.toString(),
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

            if (ses.commentCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isCommentSectionClickable) {
                            navController.navigate(Constants.SES_COMMENTS_ROUTE.replace("{sesId}", ses.id))
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Yorumlar",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ses.commentCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}