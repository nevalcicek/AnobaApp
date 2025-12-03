package com.neval.anoba.livestream

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSheet(
    scope: CoroutineScope,
    bottomSheetState: SheetState,
    liveStreamViewModel: LiveStreamViewModel,
    isCompact: Boolean
) {
    val currentStream by liveStreamViewModel.currentLiveStream.collectAsState()
    var showUnderConstructionDialog by rememberSaveable { mutableStateOf(false) }

    if (showUnderConstructionDialog) {
        AlertDialog(
            onDismissRequest = { showUnderConstructionDialog = false },
            title = { Text(text = "Yapım Aşamasında") },
            text = { Text("Bu özellik şu anda geliştirilmektedir. Lütfen daha sonra tekrar deneyin.") },
            confirmButton = {
                TextButton(onClick = { showUnderConstructionDialog = false }) {
                    Text("Geri dönmek için tıkla")
                }
            }
        )
    }

    val minHeight = if (isCompact) 250.dp else 320.dp
    val contentPadding = if (isCompact) PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    else PaddingValues(horizontal = 20.dp, vertical = 12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(Color(0xFFF3E5F5))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Uygulama Kürsüsü", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = {
                    scope.launch {
                        bottomSheetState.hide() // collapse() yerine hide()
                    }
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Kapat")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentStream != null) {
                val stream = currentStream!!
                Text(
                    "Şu An Sahnede:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                LiveStreamCard(stream = stream, onClick = { /* TODO: Canlı yayın detayına git */ })
                Spacer(Modifier.height(8.dp))
                if (!stream.description.isNullOrBlank()) {
                    Text(
                        stream.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        "Yayın için özel bir açıklama bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 4.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Filled.Podcasts,
                    contentDescription = "Aktif yayın yok",
                    modifier = Modifier
                        .size(60.dp)
                        .padding(vertical = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    "Şu anda sahnede aktif bir yayın bulunmuyor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bir sonraki yayın için takipte kalın!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { showUnderConstructionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text("Sahneye Çıkmak İçin Talepte Bulun")
            }
        }
    }
}

@Composable
fun LiveStreamCard(stream: LiveStream, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stream.title.takeIf { it.isNotBlank() } ?: "Başlıksız Yayın",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Yayıncı",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stream.streamerName.takeIf { it.isNotBlank() } ?: "Bilinmeyen Yayıncı",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Podcasts,
                    contentDescription = "Canlı Yayın Aktif",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Red
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "CANLI",
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.Red),
                    )
                    stream.startTime?.let { date ->
                        Text(
                            text = "Başlangıç: ${
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    .format(date)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            if (!stream.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stream.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}