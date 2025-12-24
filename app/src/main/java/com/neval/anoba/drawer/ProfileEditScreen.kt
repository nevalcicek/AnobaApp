package com.neval.anoba.drawer

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.neval.anoba.R
import com.neval.anoba.common.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val currentEmail by authViewModel.userEmail.collectAsState()
    var newDisplayName by remember { mutableStateOf("") }
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    var initialUserNameLoaded by remember { mutableStateOf(false) }

    var isUpdating by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Galeri açma başlatıcısı
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedImageUri = uri
        }
    )

    LaunchedEffect(authViewModel.userName) {
        authViewModel.userName.collectLatest { vmUserName ->
            if (!initialUserNameLoaded && vmUserName.isNotBlank() && vmUserName != "Misafir") {
                newDisplayName = vmUserName
                initialUserNameLoaded = true
            } else if (!initialUserNameLoaded && newDisplayName.isEmpty()) {
                newDisplayName = vmUserName
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profili Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = selectedImageUri ?: profileImageUrl,
                    contentDescription = "Profil Resmi",
                    placeholder = painterResource(id = R.drawable.ic_default_profile),
                    error = painterResource(id = R.drawable.ic_default_profile),
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { galleryLauncher.launch("image/*") }
                )
                if (isUpdating) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newDisplayName,
                onValueChange = { newDisplayName = it },
                label = { Text("Adınız") },
                singleLine = true,
            )

            OutlinedTextField(
                value = currentEmail,
                onValueChange = {},
                label = { Text("E-posta (değiştirilemez)") },
                singleLine = true,
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isUpdating = true
                    // Seçilen bir resim varsa önce onu yükle
                    selectedImageUri?.let { uri ->
                        authViewModel.updateUserProfilePicture(uri) { success, message ->
                            if (success) {
                                // Resim yüklendikten sonra adı güncelle
                                updateDisplayName(authViewModel, newDisplayName, context, navController) { isUpdating = false }
                            } else {
                                Toast.makeText(context, "Resim yüklenemedi: ${message ?: "Bilinmeyen hata"}", Toast.LENGTH_LONG).show()
                                isUpdating = false
                            }
                        }
                    } ?: run {
                        // Sadece adı güncelle
                        updateDisplayName(authViewModel, newDisplayName, context, navController) { isUpdating = false }
                    }
                },
                enabled = !isUpdating
            ) {
                Text("Değişiklikleri Kaydet")
            }
        }
    }
}

// Yardımcı fonksiyon
private fun updateDisplayName(
    authViewModel: AuthViewModel,
    newName: String,
    context: android.content.Context,
    navController: NavHostController,
    onFinished: () -> Unit
) {
    if (newName.isBlank()) {
        Toast.makeText(context, "Ad boş bırakılamaz.", Toast.LENGTH_SHORT).show()
        onFinished()
        return
    }
    authViewModel.updateUserName(newName = newName) { success, message ->
        if (success) {
            Toast.makeText(context, message ?: "Profil güncellendi ✅", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        } else {
            Toast.makeText(context, "Hata: ${message ?: "Bilinmeyen hata"}", Toast.LENGTH_LONG).show()
        }
        onFinished()
    }
}
