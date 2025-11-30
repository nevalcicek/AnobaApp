package com.neval.anoba.ses

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.ses.sescomment.SesCommentViewModel
import com.neval.anoba.ses.sesemoji.SesEmojiReactionRow
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
fun SoundWaveAnimation(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val waveHeights = (1..5).map {
        val duration = remember { Random.nextInt(400, 800) }
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = if (isPlaying) 1f else 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp)
    ) { 
        val barCount = 50
        val barWidth = size.width / (barCount * 2)
        val maxBarHeight = size.height * 0.8f

        for (i in 0 until barCount) {
            val waveIndex = i % waveHeights.size
            val currentHeight = maxBarHeight * if (isPlaying) waveHeights[waveIndex].value else 0.1f
            val yOffset = (size.height - currentHeight) / 2

            drawRoundRect(
                color = Color.LightGray,
                topLeft = Offset(x = i * barWidth * 2 + barWidth / 2, y = yOffset),
                size = Size(width = barWidth, height = currentHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesDetailScreen(
    sesId: String,
    navController: NavController,
    viewModel: SesViewModel = koinViewModel(),
    commentViewModel: SesCommentViewModel = koinViewModel { parametersOf(sesId) },
    authViewModel: AuthViewModel = koinViewModel()
) {
    val sesState by viewModel.sesDetails.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val comments by commentViewModel.comments.collectAsState()
    val sesDeleted by viewModel.sesDeleted.collectAsState()
    
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(sesId) {
        viewModel.loadSesDetails(sesId)
        viewModel.incrementViewCount(sesId)
    }

    LaunchedEffect(sesDeleted) {
        if (sesDeleted) {
            Toast.makeText(context, "Ses başarıyla silindi.", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            viewModel.resetDeleteStatus()
        }
    }

    if (sesState == null && !isDeleting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("⏳ Ses yükleniyor...")
            }
        }
    } else {
        sesState?.let { currentSes ->
            val canDelete = currentUserId == currentSes.ownerId || userRole == "ADMIN"

            var isPlaying by remember { mutableStateOf(false) }
            var totalDuration by remember { mutableLongStateOf(0L) }
            var currentTime by remember { mutableLongStateOf(0L) }

            val exoPlayer = remember(currentSes.audioUrl) {
                ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(currentSes.audioUrl)
                    setMediaItem(mediaItem)
                    prepare()
                }
            }

            DisposableEffect(key1 = exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            totalDuration = exoPlayer.duration
                        }
                    }
                }
                exoPlayer.addListener(listener)

                onDispose {
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }

            LaunchedEffect(key1 = isPlaying) {
                while (isPlaying) {
                    currentTime = exoPlayer.currentPosition
                    delay(1000L)
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Sesi Sil") },
                    text = { Text("Bu sesi kalıcı olarak silmek istediğinizden emin misiniz?") },
                    confirmButton = {
                        TextButton(onClick = {
                            isDeleting = true
                            viewModel.deleteSes(currentSes.id)
                            showDeleteDialog = false
                        }) { Text("Evet, Sil", color = MaterialTheme.colorScheme.error) }
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
                    // 1. GERİ DÖN OKU İLE SES KARTI ARASINDAKİ BOŞLUK
                    Spacer(modifier = Modifier.height(10.dp))

                     Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSes.senderName.ifBlank { "Anonim" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                currentSes.timestamp?.let {
                                    Text(
                                        text = DateTimeUtils.formatRelativeTime(it.time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            SoundWaveAnimation(isPlaying = isPlaying)
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Durdur" else "Oynat",
                                        modifier = Modifier.fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = currentTime.toFloat(),
                                    onValueChange = { newPosition ->
                                        exoPlayer.seekTo(newPosition.toLong())
                                        currentTime = newPosition.toLong()
                                    },
                                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f),
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .graphicsLayer(scaleY = 0.5f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = formatDuration(currentTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = formatDuration(totalDuration), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    
                    // 4. SES KARTI İLE GÖZ İKONU SATIRI ARASINDAKİ BOŞLUK
                    Spacer(modifier = Modifier.height(16.dp))

                    val topReaction = currentSes.reactions.maxByOrNull { it.value }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentSes.viewCount > 0) {
                                Icon(
                                    imageVector = Icons.Default.RemoveRedEye,
                                    contentDescription = "Görüntülenme Sayısı",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentSes.viewCount.toString(),
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
                                    contentDescription = "Sesi Sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // 5. GÖZ İKONU SATIRI İLE EMOJİ SATIRI ARASINDAKİ BOŞLUK
                    Spacer(modifier = Modifier.height(16.dp))

                    SesEmojiReactionRow(
                        reactions = currentSes.reactions,
                        selectedEmoji = userReactions[sesId],
                        onReact = { emoji -> viewModel.toggleReaction(sesId, emoji) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )

                    // 6. EMOJİLERİN OLDUĞU SATIRLA YORUM SATIRI ARASINDAKİ BOŞLUK
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val route = Constants.SES_COMMENTS_ROUTE.replace("{sesId}", sesId)
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
                        if (comments.isNotEmpty()) {
                            Text(
                                text = "${comments.size} Yorum",
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
                        Text("Sesi Kapat")
                    }

                    // 7. SESİ KAPAT BUTONUYLA SAYFANIN ALTI ARASINDAKİ BOŞLUK
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}
