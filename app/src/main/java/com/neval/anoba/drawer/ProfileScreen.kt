package com.neval.anoba.drawer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.neval.anoba.R
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val userEmail by authViewModel.userEmail.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()
    val registrationDate by authViewModel.registrationDate.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val sonNumara by authViewModel.sonNumara.collectAsState()

    val context = LocalContext.current
    val currentUserEmailForDeletion = remember { FirebaseAuth.getInstance().currentUser?.email }
    val isLoggedInWithValidEmail = userEmail.isNotEmpty() &&
            userEmail != Constants.DEFAULT_USERNAME_GUEST &&
            userEmail != Constants.DEFAULT_EMAIL_NOT_FOUND

    LaunchedEffect(Unit) {
        authViewModel.refreshAuthState()
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            authViewModel.updateUserProfilePicture(uri) { success, message ->
                if (success) {
                    Toast.makeText(context, "Profil resmi güncellendi.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Hata: ${message ?: "Bilinmeyen hata"}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Resim seçilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var passwordInputForDelete by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog && isLoggedInWithValidEmail) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                passwordInputForDelete = ""
                deleteError = null
            },
            title = { Text("Hesabı Sil") },
            text = {
                Column {
                    Text("Hesabınızı silmek kalıcı bir işlemdir ve geri alınamaz. Devam etmek için lütfen şifrenizi girin:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInputForDelete,
                        onValueChange = { passwordInputForDelete = it },
                        label = { Text("Şifre") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    deleteError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hata: $it",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordInputForDelete.isBlank()) {
                            deleteError = "Lütfen şifrenizi girin."
                            return@TextButton
                        }
                        if (currentUserEmailForDeletion == null || !currentUserEmailForDeletion.contains("@")) {
                            deleteError = "Hesap silme işlemi için geçerli bir e-posta bulunamadı. Lütfen tekrar giriş yapın."
                            return@TextButton
                        }

                        authViewModel.reauthenticateAndDelete(
                            email = currentUserEmailForDeletion,
                            password = passwordInputForDelete
                        ) { success, error ->
                            if (success) {
                                showDeleteDialog = false
                                Toast.makeText(context, "Hesabınız başarıyla silindi.", Toast.LENGTH_LONG).show()
                                navController.navigate(Constants.LOGIN_NAV_GRAPH) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            } else {
                                deleteError = error ?: "Hesap silinirken bilinmeyen bir hata oluştu."
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading && showDeleteDialog) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Evet, Sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    passwordInputForDelete = ""
                    deleteError = null
                }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Constants.HOME_SCREEN) {
                            popUpTo(Constants.HOME_SCREEN) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Filled.Home, contentDescription = "Ana Sayfa")
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
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (isLoading && !showDeleteDialog) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(all = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isLoggedInWithValidEmail) Arrangement.SpaceBetween else Arrangement.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clickable(enabled = isLoggedInWithValidEmail) {
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = profileImageUrl.ifBlank { R.drawable.ic_default_profile },
                                contentDescription = "Profil Fotoğrafı",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_default_profile)
                            )
                            // Edit icon overlay
                            if (isLoggedInWithValidEmail) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Profili Düzenle",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (userName.isNotBlank() && userName != Constants.DEFAULT_USERNAME_GUEST) {
                                UserInfoRow(
                                    label = "Kullanıcı Adı",
                                    value = userName,
                                    onClick = {
                                        if (isLoggedInWithValidEmail) {
                                            navController.navigate(Constants.PROFILE_EDIT_SCREEN)
                                        } else {
                                            Toast.makeText(context, "Bu özelliği kullanmak için giriş yapmalısınız.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (isLoggedInWithValidEmail) {
                                UserInfoRow(
                                    label = "E-posta",
                                    value = userEmail,
                                    onClick = { Toast.makeText(context, "E-posta adresi değiştirilemez.", Toast.LENGTH_SHORT).show() }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            registrationDate?.let { date ->
                                if (isLoggedInWithValidEmail) {
                                    UserInfoRow(
                                        label = "Kayıt Tarihi",
                                        value = date,
                                        onClick = { Toast.makeText(context, "Kayıt tarihi değiştirilemez.", Toast.LENGTH_SHORT).show() }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }

                            if (sonNumara != null && isLoggedInWithValidEmail) {
                                UserInfoRow(
                                    label = "Kullanıcı Numarası",
                                    value = sonNumara.toString()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (!isLoggedInWithValidEmail) {
                                Text(
                                    text = "Misafir olarak göz atıyorsunuz. Tam erişim için lütfen kayıt olun veya giriş yapın.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                            }
                        }

                        if (isLoggedInWithValidEmail) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { navController.navigate(Constants.PROFILE_EDIT_SCREEN) },
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    enabled = !isLoading
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Düzenle",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(text = "Profili Düzenle")
                                }

                                OutlinedButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    enabled = !isLoading
                                ) {
                                    Text(text = "Hesabımı Sil")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UserInfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
