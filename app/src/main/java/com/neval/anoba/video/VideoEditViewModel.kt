package com.neval.anoba.video

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@Stable
sealed class TrimState {
    data object Idle : TrimState() // Boşta
    data object InProgress : TrimState() // İşlem Sürüyor
    data class Success(val uri: Uri) : TrimState() // Başarılı
    data class Error(val message: String) : TrimState() // Hata
}

@UnstableApi
class VideoEditViewModel : ViewModel() {

    // Kesme işleminin durumunu tutar
    private val _trimState = MutableStateFlow<TrimState>(TrimState.Idle)
    val trimState = _trimState.asStateFlow()

    // Video işleme ilerlemesini tutar (0-100)
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    // Videonun sessiz olup olmadığını tutar
    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private var transformer: Transformer? = null
    private val transformerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Sessize alma durumunu değiştirir
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    // Videoyu kesme ve efekt uygulama işlemini başlatır
    fun trimVideo(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        effects: List<Effect>,
        isMuted: Boolean
    ) {
        _trimState.value = TrimState.InProgress
        _progress.value = 0

        val outputVideo = File(context.cacheDir, "trimmed_video_${System.currentTimeMillis()}.mp4")

        // Transformer nesnesini oluştur ve dinleyiciyi ekle
        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                // İşlem başarıyla tamamlandığında bu metod çağrılır.
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    _trimState.value = TrimState.Success(Uri.fromFile(outputVideo))
                    transformer = null
                }

                // İşlem sırasında bir hata oluşursa bu metod çağrılır.
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    _trimState.value = TrimState.Error(exportException.message ?: "Bilinmeyen bir hata oluştu")
                    transformer = null
                }
            })
            .build()

        // 1. Kaynak MediaItem'ı oluştur
        val sourceMediaItem = MediaItem.fromUri(uri)

        // 2. Kesilecek aralığı belirten YENİ bir MediaItem oluştur
        val clippedMediaItem = sourceMediaItem.buildUpon()
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        // 3. Kesilmiş MediaItem'a efektleri ve ses durumunu ekleyerek bir EditedMediaItem oluştur
        val editedMediaItem = EditedMediaItem.Builder(clippedMediaItem)
            .setRemoveAudio(isMuted)
            .setEffects(Effects(listOf(), effects))
            .build()

        // Dönüştürme işlemini başlat
        transformer?.start(editedMediaItem, outputVideo.absolutePath)

        // İlerlemeyi takip etmek için coroutine başlat
        transformerScope.launch {
            val progressHolder = ProgressHolder()
            while (transformer != null && _trimState.value is TrimState.InProgress) {
                transformer?.getProgress(progressHolder)
                _progress.value = progressHolder.progress
                delay(250) // 250ms'de bir ilerlemeyi kontrol et
            }
        }
    }

    // İşlem durumunu sıfırlar
    fun resetTrimState() {
        _trimState.value = TrimState.Idle
        _progress.value = 0
        transformer?.cancel()
        transformer = null
    }

    // ViewModel temizlendiğinde çalışanları iptal et
    override fun onCleared() {
        super.onCleared()
        transformer?.cancel()
        transformerScope.cancel()
    }
}
