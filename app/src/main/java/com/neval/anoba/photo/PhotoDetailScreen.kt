package com.neval.anoba.photo

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.photo.photoemoji.PhotoEmojiReactionRow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    navController: NavHostController,
    photoId: String,
    photoViewModel: PhotoViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val photo by photoViewModel.photoDetails.collectAsState()
    val userReactions by photoViewModel.userReactions.collectAsState()
    val shareState by photoViewModel.shareState.collectAsState()

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(shareState) {
        when (val state = shareState) {
            is ShareState.Success -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    type = "image/jpeg"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Fotoğrafı paylaş...")
                shareLauncher.launch(chooser)
                photoViewModel.resetShareState()
            }
            is ShareState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                photoViewModel.resetShareState()
            }
            is ShareState.Processing -> {
                Toast.makeText(context, "Paylaşım için hazırlanıyor...", Toast.LENGTH_SHORT).show()
            }
            ShareState.Idle -> { }
        }
    }

    LaunchedEffect(photoId) {
        photoViewModel.loadPhotoDetails(photoId)
        photoViewModel.incrementViewCount(photoId)
        photoViewModel.loadComments(photoId)
    }

    photo?.let { currentPhoto ->
        val canDelete = currentUserId == currentPhoto.ownerId || userRole == "ADMIN"

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Fotoğrafı Sil") },
                text = { Text("Bu fotoğrafı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            photoViewModel.deletePhoto(currentPhoto.id)
                            showDeleteDialog = false
                            Toast.makeText(context, "Fotoğraf silindi.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    ) { Text("Evet, Sil", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Hayır") }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentPhoto.username.ifBlank { "Fotoğraf" }) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (shareState !is ShareState.Processing) {
                                photoViewModel.sharePhoto(context, currentPhoto.photoUrl, currentPhoto.id)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş")
                        }
                        IconButton(onClick = {
                            navController.navigate(Constants.HOME_SCREEN) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }) {
                            Icon(Icons.Default.Home, contentDescription = "Ana Sayfa")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentPhoto.username.ifBlank { "Anonim" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentPhoto.timestamp?.let { DateTimeUtils.formatRelativeTime(it.time) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AsyncImage(
                            model = currentPhoto.photoUrl,
                            contentDescription = "Fotoğraf ${currentPhoto.id}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.4f),
                            contentScale = ContentScale.Crop
                        )

                        val topReaction = currentPhoto.reactions.maxByOrNull { it.value }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentPhoto.viewCount > 0) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveRedEye,
                                        contentDescription = "Görüntülenme Sayısı",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentPhoto.viewCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                if (topReaction != null && topReaction.value > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = topReaction.key,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = topReaction.value.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (canDelete) {
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Fotoğrafı Sil",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                PhotoEmojiReactionRow(
                    reactions = currentPhoto.reactions.filterValues { it > 0 },
                    selectedEmoji = userReactions[currentPhoto.id],
                    onReact = { emojiClicked ->
                        photoViewModel.toggleReaction(currentPhoto.id, emojiClicked)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val route = Constants.PHOTO_COMMENTS_ROUTE.replace("{photoId}", currentPhoto.id)
                            navController.navigate(route)
                        }
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Yorum Yap",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yorum Yap",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (currentPhoto.commentsCount > 0) {
                        Text(
                            text = "${currentPhoto.commentsCount} Yorum",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.width(200.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Fotoğrafı Kapat")
                }

                Spacer(modifier = Modifier.height(26.dp))
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("⏳ Fotoğraf yükleniyor...")
            }
        }
    }
}
