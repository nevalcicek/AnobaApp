package com.neval.anoba.video

import android.Manifest
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoHomeScreen(
    navController: NavController,
    videoViewModel: VideoViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val videos by videoViewModel.videos.collectAsState()
    val isUploading by videoViewModel.isUploading.collectAsState()
    val context = LocalContext.current
    var videoUriToRecord by remember { mutableStateOf<Uri?>(null) }

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    LaunchedEffect(key1 = Unit) {
        videoViewModel.loadVideos()
    }

    val takeVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && videoUriToRecord != null) {
            val duration = getVideoDuration(context, videoUriToRecord!!)
            videoViewModel.uploadVideo(
                videoUri = videoUriToRecord!!,
                title = "Başlıksız Video",
                duration = duration,
                onComplete = { uploadSuccess, _ ->
                    if (uploadSuccess) {
                        Toast.makeText(context, "Video yüklendi ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Video yüklenemedi.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            Toast.makeText(context, "Video çekme iptal edildi.", Toast.LENGTH_SHORT).show()
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val duration = getVideoDuration(context, uri)
            videoViewModel.uploadVideo(
                videoUri = uri,
                title = "Başlıksız Video",
                duration = duration,
                onComplete = { success, _ ->
                    if (success) {
                        Toast.makeText(context, "Video yüklendi ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Video yüklenemedi.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            Toast.makeText(context, "Video seçilmedi", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            createVideoUri(context)?.let { uri ->
                videoUriToRecord = uri
                takeVideoLauncher.launch(uri)
            } ?: Toast.makeText(context, "Video dosyası oluşturulamadı.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Kamera ve Mikrofon izni gerekli.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videolar") },
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
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoCall,
                        contentDescription = "Video Çek",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { pickVideoLauncher.launch("video/*") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = "Galeriden Seç",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            if (isUploading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Henüz hiç video yüklenmemiş.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video: VideoModel ->
                        val canDelete = currentUserId == video.ownerId || userRole == "ADMIN"

                        VideoCard(
                            video = video,
                            navController = navController,
                            canDelete = canDelete,
                            onDeleteClicked = { videoViewModel.deleteVideo(video.id) },
                            onClick = {
                                val route = Constants.VIDEO_DETAIL_SCREEN.replace("{videoId}", video.id)
                                navController.navigate(route)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun createVideoUri(context: Context): Uri? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val videoFileName = "MP4_${timestamp}_"
        val storageDir = context.cacheDir

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val videoFile = File.createTempFile(
            videoFileName,
            ".mp4",
            storageDir
        )
        Log.d("createVideoUri", "Geçici video dosyası oluşturuldu: ${videoFile.absolutePath}")

        val authority = "${context.packageName}.provider"
        FileProvider.getUriForFile(context, authority, videoFile)
    } catch (ex: Exception) {
        Log.e("createVideoUri", "Video URI oluşturulamadı: ${ex.message}", ex)
        Toast.makeText(context, "Video dosyası oluşturulurken hata: ${ex.message}", Toast.LENGTH_LONG).show()
        null
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
