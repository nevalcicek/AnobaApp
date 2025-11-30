package com.neval.anoba.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

object AppInitializer {
    fun initialize(
        context: Context,
        authInstance: FirebaseAuth,
        firestoreInstance: FirebaseFirestore,
        storageInstance: FirebaseStorage
    ) {
        startKoin {
            androidLogger(Level.INFO)
            androidContext(context)
            modules(
                createAppModuleDefinition(
                    authInstance = authInstance,
                    firestoreInstance = firestoreInstance,
                    storageInstance = storageInstance
                )
            )
        }
    }
}
