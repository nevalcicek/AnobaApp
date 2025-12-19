package com.neval.anoba

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.neval.anoba.di.AppInitializer
import java.util.concurrent.atomic.AtomicBoolean

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize App Check first
        initAppCheckIfNeeded()

        val firebaseAuthInstance: FirebaseAuth = Firebase.auth
        val firebaseFirestoreInstance: FirebaseFirestore = Firebase.firestore
        val firebaseStorageInstance: FirebaseStorage = FirebaseStorage.getInstance()

        AppInitializer.initialize(
            context = this,
            authInstance = firebaseAuthInstance,
            firestoreInstance = firebaseFirestoreInstance,
            storageInstance = firebaseStorageInstance
        )
    }

    companion object {
        // App Check'i sadece bir kez başlatmak için güvenli bayrak.
        private val appCheckInitialized = AtomicBoolean(false)

        fun initAppCheckIfNeeded() {
            if (appCheckInitialized.compareAndSet(false, true)) {
                try {
                    Log.i("AppCheck", "RELEASE modunda çalışıyor. PlayIntegrityAppCheckProviderFactory kullanılıyor.")
                    val providerFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
                    Firebase.appCheck.installAppCheckProviderFactory(providerFactory)
                    Log.i("AppCheck", "App Check başarıyla başlatıldı.")
                } catch (t: Throwable) {
                    appCheckInitialized.set(false)
                    Log.e("AppCheck", "App Check başlatılırken hata: ${t.message}", t)
                } catch (e: IllegalStateException) {
                    appCheckInitialized.set(true)
                    Log.w("AppCheck", "App Check başlatma denemesi (zaten başlatılmış olabilir): ${e.message}")
                }
            } else {
                Log.d("AppCheck", "App Check zaten aktif.")
            }
        }
    }
}
