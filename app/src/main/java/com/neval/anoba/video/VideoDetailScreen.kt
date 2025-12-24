package com.neval.anoba.video

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.photo.ShareState
import com.neval.anoba.video.videoemoji.VideoEmojiReactionRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    navController: NavController,
    videoId: String,
    videoViewModel: VideoViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val video by videoViewModel.videoDetails.collectAsState()
    val userReactions by videoViewModel.userReactions.collectAsState()
    val shareState by videoViewModel.shareState.collectAsState()

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult() 
    ) { }

    LaunchedEffect(shareState) {
        when (val state = shareState) {
            is ShareState.Success -> {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    type = "video/mp4"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Videoyu paylaş...")
                shareLauncher.launch(chooser)
                videoViewModel.resetShareState()
            }
            is ShareState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                videoViewModel.resetShareState()
            }
            is ShareState.Processing -> {
                Toast.makeText(context, "Paylaşım için hazırlanıyor...", Toast.LENGTH_SHORT).show()
            }
            ShareState.Idle -> { }
        }
    }

    LaunchedEffect(videoId) {
        videoViewModel.loadVideoDetails(videoId)
        videoViewModel.incrementViewCount(videoId)
    }

    LaunchedEffect(key1 = true) {
        videoViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is VideoUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    video?.let { currentVideo ->
        val canDelete = currentUserId == currentVideo.ownerId || userRole == "ADMIN"

        val exoPlayer = remember(currentVideo.videoUrl) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(currentVideo.videoUrl))
                prepare()
            }
        }

        var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
        var isLoading by remember { mutableStateOf(true) }
        var totalDuration by remember { mutableStateOf(0L) }
        var currentTime by remember { mutableStateOf(0L) }

        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                    isPlaying = isPlayingValue
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        LaunchedEffect(isPlaying) {
            while(isPlaying) {
                currentTime = exoPlayer.currentPosition.coerceAtLeast(0L)
                delay(1000)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Videoyu Sil") },
                text = { Text("Bu videoyu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            videoViewModel.deleteVideo(currentVideo.id)
                            showDeleteDialog = false
                            Toast.makeText(context, "Video silindi.", Toast.LENGTH_SHORT).show()
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
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                             if (shareState !is ShareState.Processing) { 
                                videoViewModel.shareVideo(context, currentVideo.videoUrl, currentVideo.id)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentVideo.username.ifBlank { "Anonim" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            currentVideo.timestamp?.let {
                                Text(
                                    text = DateTimeUtils.formatRelativeTime(it.time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentVideo.videoUrl.isNotBlank()) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .background(Color.Black)
                                        .clickable {
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    VideoPlayer(
                                        exoPlayer = exoPlayer,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // YÜKLEME ANİMASYONU
                                    if (isLoading) {
                                        CircularProgressIndicator()
                                    } else if (!isPlaying) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Oynat",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                VideoControls(
                                    player = exoPlayer,
                                    totalDuration = totalDuration,
                                    currentTime = currentTime,
                                    onSeek = { newPosition ->
                                        exoPlayer.seekTo(newPosition.toLong())
                                    }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Video URL bulunamadı.", color = Color.White)
                            }
                        }
                    }
                }

                val topReaction = currentVideo.reactions.maxByOrNull { it.value }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentVideo.viewCount > 0) {
                            Icon(
                                imageVector = Icons.Default.RemoveRedEye,
                                contentDescription = "Görüntülenme Sayısı",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentVideo.viewCount.toString(),
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
                                contentDescription = "Videoyu Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                VideoEmojiReactionRow(
                    reactions = currentVideo.reactions,
                    selectedEmoji = userReactions[currentVideo.id],
                    onReact = { emojiClicked ->
                        videoViewModel.toggleReaction(currentVideo.id, emojiClicked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val route =
                                Constants.VIDEO_COMMENTS_ROUTE.replace("{videoId}", currentVideo.id)
                            navController.navigate(route)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Yorum Yap",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yorum Yap",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (currentVideo.commentsCount > 0) {
                        Text(
                            text = "${currentVideo.commentsCount} Yorum",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .width(200.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Videoyu Kapat")
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

@Composable
fun VideoControls(
    player: Player,
    totalDuration: Long,
    currentTime: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = formatDuration(currentTime), color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .graphicsLayer(scaleY = 0.4f)
            ) {
                Slider(
                    value = currentTime.toFloat(),
                    onValueChange = onSeek,
                    valueRange = 0f..(totalDuration.toFloat().takeIf { it > 0 } ?: 0f)
                )
            }
            Text(text = formatDuration(totalDuration), color = MaterialTheme.colorScheme.onSurface)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { player.seekBack() }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Geri Sar")
            }
            Spacer(modifier = Modifier.size(40.dp))
            IconButton(onClick = { player.seekForward() }) {
                Icon(Icons.Default.FastForward, contentDescription = "İleri Sar")
            }
        }
    }
}