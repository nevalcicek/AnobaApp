package com.neval.anoba.ses

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.common.utils.Constants
import org.koin.androidx.compose.koinViewModel

@Composable
fun SesHomeScreen(
    navController: NavController,
    viewModel: SesViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val sesler by viewModel.sesler.collectAsState()
    val sending by viewModel.sending.collectAsState()
    
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var hasFinishedRecording by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSesler()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startRecording(context)
                isRecording = true
                hasFinishedRecording = false
            } else {
                Toast.makeText(context, "🎙️ Mikrofon izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri Dön",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = {
                    navController.navigate(Constants.HOME_SCREEN) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Ana Sayfa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(bottom = 48.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // NOT: Butonu yukarıdan ayırmak ve biraz aşağıya almak için bu Spacer'ı kullanıyoruz. Değeri değiştirerek boşluğu ayarlayabilirsiniz.
                Spacer(modifier = Modifier.height(20.dp))

                if (!isRecording && !hasFinishedRecording) {
                    RecordButton(isRecording = false, onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
                } else if (isRecording) {
                    RecordButton(isRecording = true, onClick = {
                        viewModel.stopRecording()
                        isRecording = false
                        hasFinishedRecording = true
                    })
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                viewModel.uploadAudioToFirebase(viewModel.audioFile!!)
                                hasFinishedRecording = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Gönder")
                        }
                        Button(
                            onClick = {
                                viewModel.cancelRecording()
                                hasFinishedRecording = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("İptal")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Sesler", fontSize = 18.sp, fontWeight = FontWeight.Normal)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (sesler.isEmpty() && !sending) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz ses yok", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (sending) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Gönderiliyor...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    items(items = sesler, key = { it.id }) { ses: SesModel ->
                        val canDelete = currentUserId == ses.ownerId || userRole == "ADMIN"

                        SesCard(
                            ses = ses,
                            navController = navController,
                            canDelete = canDelete,
                            onDeleteClicked = { viewModel.deleteSes(ses.id) },
                            isCommentSectionClickable = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .border(2.dp, color, RoundedCornerShape(50.dp)),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 36.dp else 24.dp)
                .background(color, shape = RoundedCornerShape(if (isRecording) 4.dp else 50.dp))
        )
    }
}
