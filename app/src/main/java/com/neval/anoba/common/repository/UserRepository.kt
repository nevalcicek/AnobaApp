package com.neval.anoba.common.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.neval.anoba.common.datastore.DataStoreKeys
import com.neval.anoba.common.datastore.dataStore
import com.neval.anoba.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.callbackFlow

class UserRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : IUserRepository {

    companion object {
        private const val TAG = "UserRepository"
    }

    override suspend fun getUserById(uid: String): User? {
        if (uid.isEmpty()) return null
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID: $uid", e)
            null
        }
    }

    override suspend fun findUserByUsername(username: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("displayName", username)
                .limit(1)
                .get()
                .await()
            if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents[0].toObject(User::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by username: $username", e)
            null
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all users", e)
            emptyList()
        }
    }

    override fun getAllUsersStream(): Flow<List<User>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for user changes", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(users).isSuccess
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getSonNumara(uid: String): Int? {
        if (uid.isEmpty()) return null
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.getLong("sonNumara")?.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sonNumara for user: $uid", e)
            null
        }
    }

    override suspend fun isRememberMe(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMEMBER_ME_KEY] == "true"
        }.first()
    }

    override suspend fun setRememberMe(rememberMe: Boolean) {
        context.dataStore.edit {
            it[DataStoreKeys.REMEMBER_ME_KEY] = rememberMe.toString()
        }
    }

    override suspend fun loginAsGuest(): Pair<Boolean, String?> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                Pair(true, user.uid)
            } else {
                Pair(false, "Misafir girişi başarısız.")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Bilinmeyen bir hata oluştu.")
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: Uri): String? {
        return try {
            val storageRef = storage.reference.child("profile_images").child("$uid.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile image", e)
            null
        }
    }

    override suspend fun updateUserProfilePhotoUrl(uid: String, photoUrl: String): Boolean {
        return try {
            firestore.collection("users").document(uid).update("photoURL", photoUrl).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile photo URL", e)
            false
        }
    }

    override fun logout() {
        // Bu metodun içeriği AuthViewModel'e taşındığı için boş bırakılabilir.
    }
}
