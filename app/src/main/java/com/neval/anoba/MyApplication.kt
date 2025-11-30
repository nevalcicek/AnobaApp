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

        val firebaseAuthInstance: FirebaseAuth = Firebase.auth
        val firebaseFirestoreInstance: FirebaseFirestore = Firebase.firestore
        val firebaseStorageInstance: FirebaseStorage = FirebaseStorage.getInstance()

        Log.i("AppMode", "RELEASE modunda çalışıyor. Emulator'ler kullanılmıyor.")

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

        /**
         * App Check'i ihtiyaç anında (ilk Firestore/Storage çağrısından hemen önce)
         * güvenli şekilde başlatır. Birden fazla kez çağrılsa bile yalnızca bir kez çalışır.
         *
         * Örnek kullanım:
         * - Firestore/Storage erişiminden önce:
         *   MyApplication.initAppCheckIfNeeded()
         */
        fun initAppCheckIfNeeded() {
            if (appCheckInitialized.compareAndSet(false, true)) {
                try {
                    Firebase.appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance(),
                    )
                    Log.i("AppCheck", "App Check başlatıldı (lazy).")
                } catch (t: Throwable) {
                    // Başlatma başarısız olsa bile açılışı engellemeyelim; loglayıp devam.
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