package com.neval.anoba.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.login.viewmodel.SignUpViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignUpScreen(
    navController: NavHostController,
    viewModel: SignUpViewModel = koinViewModel()
) {
    val username by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val confirmPasswordVisible by viewModel.confirmPasswordVisible.collectAsState()
    val formValid by viewModel.formValid.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()

    val usernameError by viewModel.usernameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val confirmPasswordError by viewModel.confirmPasswordError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(username, email, password, confirmPassword) {
        viewModel.validateForm()
    }

    LaunchedEffect(signUpState) {
        when (val state = signUpState) {
            is SignUpViewModel.SignUpUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Kayıt başarılı! Giriş ekranına yönlendiriliyorsunuz...",
                    duration = SnackbarDuration.Short
                )
                // Snackbar gösterildikten sonra yönlendir.
                navController.navigate(Constants.LOGIN_SCREEN) {
                    popUpTo(Constants.LOGIN_NAV_GRAPH) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            is SignUpViewModel.SignUpUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> Unit // Idle veya Loading durumlarında bir şey yapma
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Kayıt Ol", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Kullanıcı Adı") },
                isError = usernameError.isNotEmpty(),
                supportingText = { if (usernameError.isNotEmpty()) Text(usernameError, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = viewModel::updateEmail,
                label = { Text("E-posta") },
                isError = emailError.isNotEmpty(),
                supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Şifre") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(icon, contentDescription = "Şifre Görünürlüğü")
                    }
                },
                isError = passwordError.isNotEmpty(),
                supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = { Text("Şifreyi Onayla") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(icon, contentDescription = "Onay Şifresini Görünürlüğü")
                    }
                },
                isError = confirmPasswordError.isNotEmpty(),
                supportingText = { if (confirmPasswordError.isNotEmpty()) Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.performRegistration()
                },
                enabled = formValid && signUpState !is SignUpViewModel.SignUpUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (signUpState is SignUpViewModel.SignUpUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Kayıt Ol", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    if (signUpState !is SignUpViewModel.SignUpUiState.Loading) {
                        // "Giriş Yap"a basıldığında da LoginScreen'e git, geri yığını temizle
                        navController.navigate(Constants.LOGIN_SCREEN) {
                            popUpTo(Constants.LOGIN_NAV_GRAPH) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zaten bir hesabın var mı? Giriş Yap")
            }
        }
    }
}
