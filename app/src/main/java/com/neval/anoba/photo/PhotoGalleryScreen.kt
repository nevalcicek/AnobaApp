package com.neval.anoba.photo

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.neval.anoba.common.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    navController: NavHostController,
    photoViewModel: PhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            isUploading = true

            photoViewModel.uploadPhotoAndCreateRecord(
                photoUri = uri,
                onComplete = { success, newPhotoIdOrError ->
                    isUploading = false
                    if (success && newPhotoIdOrError != null) {
                        Toast.makeText(context, "Yükleme başarılı ✅", Toast.LENGTH_SHORT).show()
                        selectedPhotoUri = null

                        navController.navigate("${Constants.PHOTO_DETAIL_SCREEN}/$newPhotoIdOrError") {
                            popUpTo(Constants.PHOTO_GALLERY_SCREEN) { 
                                inclusive = true 
                            }
                        }

                        Log.d("PhotoGalleryScreen", "Fotoğraf başarıyla yüklendi. ID: $newPhotoIdOrError")
                    } else {
                        Toast.makeText(context, "Fotoğraf yüklenemedi: ${newPhotoIdOrError ?: "Lütfen tekrar deneyin."}", Toast.LENGTH_LONG).show()
                        Log.e("PhotoGalleryScreen", "Fotoğraf yükleme başarısız. Detay: $newPhotoIdOrError")
                    }
                }
            )
        } else {
            Toast.makeText(context, "Fotoğraf seçilmedi", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galeriden Fotoğraf Seç") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri Dön"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (!isUploading) {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Yükleniyor..." else "Galeriden Fotoğraf Seç")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedPhotoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedPhotoUri),
                    contentDescription = "Seçilen Fotoğraf",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Seçilen URI: $selectedPhotoUri",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
