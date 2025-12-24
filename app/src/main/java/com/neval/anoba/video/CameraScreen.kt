package com.neval.anoba.video

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController, videoViewModel: VideoViewModel = koinViewModel()) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var hasAudioPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && hasAudioPermission) {
            if (recordedVideoUri != null) {
                VideoPreview(
                    uri = recordedVideoUri!!,
                    isUploading = isUploading,
                    onEdit = {
                        val encodedUri = URLEncoder.encode(recordedVideoUri.toString(), StandardCharsets.UTF_8.toString())
                        navController.navigate(Constants.VIDEO_EDIT_SCREEN.replace("{videoUri}", encodedUri)) {
                            popUpTo(Constants.VIDEO_CAMERA_SCREEN) { inclusive = true }
                        }
                    },
                    onSave = {
                        isUploading = true
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        videoViewModel.uploadVideo(
                            videoUri = recordedVideoUri!!,
                            title = "Video $timestamp",
                            duration = getVideoDuration(context, recordedVideoUri!!),
                            onComplete = { success, message ->
                                isUploading = false
                                val toastMessage = if (success) "Video başarıyla yüklendi!" else "Hata: ${message ?: "Bilinmeyen bir hata oluştu."}"
                                ContextCompat.getMainExecutor(context).execute {
                                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                )
            } else {
                CameraCapture(
                    context = context,
                    onVideoRecorded = { uri -> recordedVideoUri = uri }
                )
            }
        } else {
            PermissionRequestUI(permissionLauncher)
        }

        if (recordedVideoUri == null) { // Geri butonunu sadece kayıt sırasında göster
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CameraCapture(
    context: Context,
    onVideoRecorded: (Uri) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor: Executor = remember { Executors.newSingleThreadExecutor() }

    val recorder = remember { Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    fun startRecording() {
        val name = "video_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())}.mp4"
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutput)
            .withAudioEnabled()
            .start(cameraExecutor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> Log.d("CameraCapture", "Kayıt başladı")
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            onVideoRecorded(recordEvent.outputResults.outputUri)
                        } else {
                            recording?.close()
                            recording = null
                            val error = recordEvent.error
                            Log.e("CameraCapture", "Video kayıt hatası: $error")
                            // Toast mesajını göstermek için ana iş parçacığına geç
                            ContextCompat.getMainExecutor(context).execute {
                                Toast.makeText(context, "Video kaydı başarısız oldu.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    val isRecording = recording != null

    LaunchedEffect(videoCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
            } catch (exc: Exception) {
                Log.e("CameraCapture", "Kullanım senaryolarını bağlama başarısız oldu", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = { if (isRecording) stopRecording() else startRecording() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isRecording) "Kaydı Durdur" else "Kayıt Başlat",
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.6f) // İkonun boyutunu ayarlar (%40)
            )
        }
    }
}

@Composable
private fun VideoPreview(uri: Uri, isUploading: Boolean, onEdit: () -> Unit, onSave: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(videoUrl = uri.toString(), modifier = Modifier.fillMaxSize())

        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEdit, enabled = !isUploading) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Düzenle",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Düzenle")
                }

                FilledTonalButton(onClick = onSave, enabled = !isUploading) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Kaydet",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestUI(permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Kamera ve ses izni gereklidir.")
            Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) {
                Text("İzin İste")
            }
        }
    }
}

private fun getVideoDuration(context: Context, uri: Uri): Long {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        time?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        Log.e("getVideoDuration", "Error getting video duration", e)
        0L
    }
}
