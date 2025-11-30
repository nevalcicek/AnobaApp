package com.neval.anoba.login.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.neval.anoba.R
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.common.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun SplashScreen(navController: NavHostController) {
    val userRepository: IUserRepository = koinInject()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        delay(1500) // Splash en az 1.5 saniye görünsün.

        // 1. Ana Ekrana git ve tüm giriş ekranlarını temizle.
        val navigateToHome = {
            navController.navigate(Constants.DRAWER_GRAPH) {
                popUpTo(Constants.LOGIN_NAV_GRAPH) { inclusive = true }
            }
        }

        // 2. Giriş Ekranına git ve sadece bu ekranı (SplashScreen) kapat.
        val navigateToLogin = {
            navController.navigate(Constants.LOGIN_SCREEN) {
                popUpTo(Constants.SPLASH_SCREEN) { inclusive = true }
            }
        }

        // Tek seferlik kontrol
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Oturum yoksa, direkt giriş ekranına git.
            navigateToLogin()
        } else {
            // Oturum varsa, "Beni Hatırla" tercihini kontrol et.
            val rememberMe = withContext(Dispatchers.IO) {
                userRepository.isRememberMe()
            }

            if (rememberMe) {
                // Tercih "true" ise, ana ekrana git.
                navigateToHome()
            } else {
                // Tercih "false" ise, önce oturumu kapat, sonra giriş ekranına git.
                auth.signOut()
                navigateToLogin()
            }
        }
    }

    // --- UI (Görsel Arayüz) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App logosu",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 50.dp)
        ) {
            Text(
                text = "Anoba",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
