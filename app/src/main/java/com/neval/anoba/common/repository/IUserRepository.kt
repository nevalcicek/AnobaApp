package com.neval.anoba.common.repository

import android.net.Uri
import com.neval.anoba.models.User

interface IUserRepository {
    suspend fun findUserByUsername(username: String): User?
    suspend fun setRememberMe(rememberMe: Boolean)
    suspend fun isRememberMe(): Boolean
    suspend fun getUserById(userId: String): User?
    suspend fun getSonNumara(uid: String): Int?
    suspend fun getAllUsers(): List<User>
    suspend fun loginAsGuest(): Pair<Boolean, String?>
    suspend fun canAccessChatRoom(userId: String, chatRoomId: String): Boolean
    fun getCurrentUserId(): String?
    suspend fun logout()
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String?
    suspend fun updateUserProfilePhotoUrl(userId: String, photoUrl: String): Boolean
}
