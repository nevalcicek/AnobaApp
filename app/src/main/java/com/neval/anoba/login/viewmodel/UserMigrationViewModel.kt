package com.neval.anoba.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Suppress("unused")
class UserMigrationViewModel(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    @Suppress("unused")
    fun migrateUsersCollection() {
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { doc ->
                val data = mutableMapOf<String, Any>()

                val userId = doc.getString("userId") ?: doc.id
                data["userId"] = userId

                val displayName = doc.getString("displayName") ?: doc.getString("username") ?: "Bilinmeyen"
                data["displayName"] = displayName

                val email = doc.getString("email") ?: ""
                data["email"] = email

                val photoURL = doc.getString("photoURL") ?: ""
                data["photoURL"] = photoURL

                firestore.collection("users").document(doc.id).set(data, SetOptions.merge())
            }
        }.addOnFailureListener {
            Log.e("Migration", "Kullanıcı belgeleri alınamadı: ${it.message}", it)
        }
    }
}
