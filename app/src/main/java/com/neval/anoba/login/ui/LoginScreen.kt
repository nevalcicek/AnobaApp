package com.neval.anoba.login.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.login.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val rememberMeValue by viewModel.rememberMe.collectAsState()
    val primaryColor = Color(0xFF232066)

    val loginResultMessage by viewModel.loginResultMessage.collectAsState()
    val guestLoginResultMessage by viewModel.guestLoginResultMessage.collectAsState()

    LaunchedEffect(guestLoginResultMessage) {
        guestLoginResultMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = "Misafir girişi başarılı! ID: $message",
                duration = SnackbarDuration.Short
            )
            navController.navigate(Constants.DRAWER_GRAPH) {
                popUpTo(Constants.LOGIN_NAV_GRAPH) { inclusive = true }
            }
            viewModel.onGuestLoginResultMessageShown()
        }
    }
    LaunchedEffect(loginResultMessage) {
        loginResultMessage?.let { message ->
            if (!message.startsWith("Giriş başarılı")) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            } else {
                navController.navigate(Constants.DRAWER_GRAPH) {
                    popUpTo(Constants.LOGIN_NAV_GRAPH) { inclusive = true }
                }
            }
            viewModel.onLoginResultMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hoş Geldiniz!",
                style = MaterialTheme.typography.displayMedium,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email") },
                isError = emailError.isNotEmpty(),
                supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Şifre") },
                isError = passwordError.isNotEmpty(),
                supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = MaterialTheme.colorScheme.error) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMeValue,
                        onCheckedChange = { newValue ->
                            viewModel.onRememberMeChanged(newValue)
                        }
                    )
                    Text("Beni Hatırla")
                }
                TextButton(onClick = { navController.navigate(Constants.FORGOT_PASSWORD_SCREEN) }) {
                    Text(
                        text = "Şifremi Unuttum",
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.login()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isValid && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(30.dp), color = Color.White)
                else Text("Giriş Yap", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.loginAsGuest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, primaryColor),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0E68C))
            ) {
                Text("Misafir Girişi", style = MaterialTheme.typography.bodyLarge, color = primaryColor)
            }

            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = { navController.navigate(Constants.SIGN_UP_SCREEN) }) {
                Text(
                    text = "Hesabın yok mu? Kayıt Ol",
                    color = primaryColor,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}
