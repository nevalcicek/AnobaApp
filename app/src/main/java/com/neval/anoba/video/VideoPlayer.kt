package com.neval.anoba.video

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Suppress("unused")
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    showControls: Boolean = true,
    onPlayerError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = autoPlay
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(videoUrl) { // videoUrl değiştiğinde medyayı yeniden yükle
        if (videoUrl.isNotBlank()) {
            try {
                val mediaItem = MediaItem.fromUri(videoUrl.toUri())
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            } catch (e: Exception) {
                val errorMessage = "MediaItem oluşturulurken veya hazırlanırken hata: ${e.localizedMessage}"
                Log.e("VideoPlayer", errorMessage, e)
                onPlayerError?.invoke(errorMessage)
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                val errorMessage = "Oynatıcı Hatası: ${error.localizedMessage ?: "Bilinmeyen oynatıcı hatası"} (Kod: ${error.errorCodeName})"
                Log.e("VideoPlayer", errorMessage, error)
                onPlayerError?.invoke(errorMessage)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release() // Kaynakları serbest bırak
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = showControls // Kontrolleri göster/gizle
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.background(Color.Black) // Video yüklenene kadar veya hata durumunda siyah arka plan
    )
}
