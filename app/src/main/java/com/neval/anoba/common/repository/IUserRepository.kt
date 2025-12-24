package com.neval.anoba.common.repository

import android.net.Uri
import com.neval.anoba.models.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun getUserById(uid: String): User?
    suspend fun getAllUsers(): List<User>
    fun getAllUsersStream(): Flow<List<User>>
    suspend fun findUserByUsername(username: String): User? // Eklendi
    suspend fun getSonNumara(uid: String): Int?
    suspend fun isRememberMe(): Boolean
    suspend fun setRememberMe(rememberMe: Boolean)
    suspend fun loginAsGuest(): Pair<Boolean, String?>
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String?
    suspend fun updateUserProfilePhotoUrl(uid: String, photoUrl: String): Boolean
    fun logout()
}
