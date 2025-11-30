package com.neval.anoba.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.login.viewmodel.ForgotPasswordViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ForgotPasswordScreen(
    navController: NavHostController
) {
    val viewModel: ForgotPasswordViewModel = koinViewModel()
    var email by remember { mutableStateOf("") }
    var isSendingRequest by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message -> // Eğer mesaj null değilse
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            actionMessage = null // Mesaj gösterildikten sonra temizle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Şifremi Unuttum",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta Adresiniz") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank()) {
                    scope.launch {
                        isSendingRequest = true
                        val (_, message) = viewModel.resetPassword(email)
                        actionMessage = message
                        isSendingRequest = false
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Lütfen e-posta adresinizi girin.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            enabled = !isSendingRequest && email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSendingRequest) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text(text = "Sıfırlama Bağlantısı Gönder")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Giriş Ekranına Dön")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
