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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.login.viewmodel.NewPasswordViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NewPasswordScreen(navController: NavHostController) {
    val viewModel: NewPasswordViewModel = koinViewModel()

    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isUpdatingPassword by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message -> // Eğer mesaj null değilse
            snackbarHostState.showSnackbar(message)
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
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Yeni Şifre") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmNewPassword,
            onValueChange = { confirmNewPassword = it },
            label = { Text("Yeni Şifre (Tekrar)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (newPassword.isBlank() || confirmNewPassword.isBlank()) {
                    actionMessage = "Lütfen tüm alanları doldurun."
                } else if (newPassword != confirmNewPassword) {
                    actionMessage = "Şifreler eşleşmiyor."
                } else {
                    scope.launch {
                        isUpdatingPassword = true
                        val (success, message) = viewModel.updatePassword(newPassword)
                        isUpdatingPassword = false
                        actionMessage = message

                        if (success) {
                            navController.navigate(Constants.LOGIN_NAV_GRAPH) {
                                popUpTo(Constants.LOGIN_NAV_GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            },
            enabled = !isUpdatingPassword
        ) {
            if (isUpdatingPassword) {
                CircularProgressIndicator()
            } else {
                Text("Şifreyi Güncelle")
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
