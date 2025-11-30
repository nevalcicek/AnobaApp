package com.neval.anoba.photo

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCreateScreen(
    navController: NavHostController,
    photoViewModel: PhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by photoViewModel.isUploading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            photoViewModel.uploadPhotoAndCreateRecord(
                photoUri = photoUri!!,
                onComplete = { uploadSuccess, newPhotoId ->
                    if (uploadSuccess && newPhotoId != null) {
                        Toast.makeText(context, "Fotoğraf yüklendi ✅", Toast.LENGTH_SHORT).show()
                        // Standart: Başarı durumunda fotoğraf ana ekranına git
                        navController.navigate(Constants.PHOTO_HOME_SCREEN) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        Toast.makeText(context, "Fotoğraf yüklenemedi. Lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
                        navController.popBackStack() // Hata durumunda geri dön
                    }
                }
            )
        } else {
            Toast.makeText(context, "Fotoğraf çekme iptal edildi.", Toast.LENGTH_SHORT).show()
            navController.popBackStack() // İptal durumunda geri dön
        }
    }

    LaunchedEffect(Unit) {
        val uri = createImageUri(context)
        if (uri == null) {
            Toast.makeText(context, "Fotoğraf dosyası hazırlanamadı.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        } else {
            photoUri = uri
            launcher.launch(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Yeni Fotoğraf Ekle") })
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Text(
                        text = "Kamera açılıyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    )
}

fun createImageUri(context: Context): Uri? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_${timestamp}_"
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir == null || (!storageDir.exists() && !storageDir.mkdirs())) {
            Log.e("createImageUri", "Depolama dizini oluşturulamadı.")
            return null
        }

        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        val authority = "${context.packageName}.provider"
        FileProvider.getUriForFile(context, authority, imageFile)
    } catch (ex: Exception) {
        Log.e("createImageUri", "Fotoğraf URI oluşturulamadı: ${ex.message}", ex)
        null
    }
}
