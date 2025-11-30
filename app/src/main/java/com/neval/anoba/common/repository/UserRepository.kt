package com.neval.anoba.common.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.neval.anoba.common.datastore.DataStoreKeys
import com.neval.anoba.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class UserRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : IUserRepository {

    companion object {
        private const val TAG = "UserRepository"
    }
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    override suspend fun findUserByUsername(username: String): User? {
        return try {
            val snapshot = db.collection("users")
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
            Log.e(TAG, "Error finding user by username: ${e.message}")
            null
        }
    }
    override suspend fun setRememberMe(rememberMe: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.REMEMBER_ME_KEY] = rememberMe
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving preferences", e)
            }
        }
    }
    override suspend fun isRememberMe(): Boolean {
        return try {
            context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e(TAG, "DataStore read error: ${exception.message}")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { preferences -> preferences[DataStoreKeys.REMEMBER_ME_KEY] == true }
                .first()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading preferences: ${e.message}")
            false
        }
    }
    override suspend fun getUserById(userId: String): User? {
        if (userId.isBlank()) {
            Log.w(TAG, "getUserById called with a blank userId.")
            return null
        }
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}")
            null
        }
    }
    override suspend fun getSonNumara(uid: String): Int? {
        return try {
            val doc = db.collection("ad_numaralari")
                .document(uid)
                .get()
                .await()

            doc.getLong("number")?.toInt()
        } catch (e: Exception) {
            Log.e("UserRepository", "son_numara okunamadı: ${e.message}")
            null
        }
    }
    override suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = db.collection("users")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all users: ${e.message}")
            emptyList()
        }
    }
    override suspend fun loginAsGuest(): Pair<Boolean, String?> {
        return try {
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid
            Log.d(TAG, "Anonymous login successful. User ID: $userId")
            Pair(true, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous login failed", e)
            Pair(false, e.message)
        }
    }
    override suspend fun canAccessChatRoom(userId: String, chatRoomId: String): Boolean {
        if (chatRoomId == "GeneralChat") {
            return userId.isNotBlank()
        }
        val user = getUserById(userId)
        return when (user?.role?.uppercase()) { // Büyük/küçük harf duyarlılığını ortadan kaldıralım
            "ADMIN", "MODERATOR", "MEMBER" -> true
            else -> false // "USER", "GUEST", null ve diğer her durum için false
        }
    }
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    override suspend fun logout() {

    }

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val storageRef = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Log.d(TAG, "Image uploaded successfully: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed", e)
            null
        }
    }

    override suspend fun updateUserProfilePhotoUrl(userId: String, photoUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("users").document(userId).update("photoUrl", photoUrl).await()
            Log.d(TAG, "User photoUrl updated successfully in Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user photoUrl in Firestore", e)
            false
        }
    }
}
