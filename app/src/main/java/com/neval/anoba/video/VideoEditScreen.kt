package com.neval.anoba.video

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditScreen(
    navController: NavController,
    videoUri: String,
    viewModel: VideoEditViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uri = remember { videoUri.toUri() }

    // ViewModel'den gelen durumları izle
    val trimState by viewModel.trimState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    // Video önizlemesi için PlayerView oluştur
    val playerView = remember {
        PlayerView(context).apply {
            controllerAutoShow = false
            setShowNextButton(false)
            setShowPreviousButton(false)
        }
    }

    // Videoyu oynatmak için ExoPlayer'ı hazırla
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE // Videonun sürekli dönmesini sağla
        }
    }

    var videoDuration by remember { mutableLongStateOf(0L) }
    val isReady = videoDuration > 0

    var sliderPositions by remember(videoDuration) {
        mutableStateOf(0f..videoDuration.toFloat())
    }

    // Mute durumuna göre ExoPlayer'ın sesini ayarla
    LaunchedEffect(isMuted) {
        exoPlayer.isDeviceMuted = isMuted
    }

    // ExoPlayer'ın yaşam döngüsünü yönet
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Video hazır olduğunda süresini al ve oynatmaya başla
                if (playbackState == Player.STATE_READY) {
                    videoDuration = exoPlayer.duration
                    exoPlayer.playWhenReady = true
                }
            }
        }
        exoPlayer.addListener(listener)
        playerView.player = exoPlayer

        onDispose {
            exoPlayer.removeListener(listener)
            playerView.player = null
        }
    }

    // Video kesme işleminin sonucunu dinle
    LaunchedEffect(trimState) {
        when (val state = trimState) {
            is TrimState.Success -> {
                Toast.makeText(context, "Video başarıyla işlendi!", Toast.LENGTH_SHORT).show()
                // Bir önceki ekrana kesilmiş videonun URI'sini gönder
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "trimmed_video_uri",
                    state.uri.toString()
                )
                navController.popBackStack() // Geri dön
            }
            is TrimState.Error -> {
                Toast.makeText(context, "Hata: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetTrimState()
            }
            else -> {}
        }
    }

    // Ekran kapandığında ExoPlayer'ı serbest bırak
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Videoyu Düzenle") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Video işlenirken bir yükleme göstergesi göster
            if (trimState is TrimState.InProgress) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Video işleniyor: $progress%")
                }
            } else {
                // Video oynatıcı
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { playerView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isReady) {
                    Text("Video yükleniyor...")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video kesme aralığı seçicisi
                RangeSlider(
                    value = sliderPositions,
                    onValueChange = { newPositions -> sliderPositions = newPositions },
                    onValueChangeFinished = {
                        exoPlayer.seekTo(sliderPositions.start.toLong())
                    },
                    valueRange = 0f..videoDuration.toFloat(),
                    enabled = isReady,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatDuration(sliderPositions.start.toLong()))
                    Text(text = formatDuration(sliderPositions.endInclusive.toLong()))
                }

                // Kontrol Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sesi Aç/Kapat Butonu
                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Sesi Aç" else "Sesi Kapat"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Kaydet butonu
                Button(
                    onClick = {
                        val startMs = sliderPositions.start.toLong()
                        val endMs = sliderPositions.endInclusive.toLong()
                        viewModel.trimVideo(context, uri, startMs, endMs, emptyList(), isMuted)
                    },
                    enabled = isReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uygula ve Kaydet")
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
