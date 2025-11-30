package com.neval.anoba

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.LocalKoinApplication
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 🔍 Kullanıcı giriş kontrolü → Logcat'te UID görünürse giriş yapılmış demektir
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("AuthCheck", "User: ${currentUser?.uid}")

        setContent {
            CompositionLocalProvider(LocalKoinApplication provides GlobalContext.get()) {
                MyAppNavigation()
            }
        }
    }
}
