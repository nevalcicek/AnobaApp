package com.neval.anoba.common.services

import android.net.Uri
import com.neval.anoba.login.state.AuthState
import com.google.firebase.auth.FirebaseUser
import com.neval.anoba.login.AuthResult
import com.neval.anoba.models.User
import kotlinx.coroutines.flow.StateFlow

interface AuthServiceInterface {
    fun isLoggedInFirebase(): Boolean
    suspend fun getCurrentUser(): User?
    fun getCurrentUserId(): String
    suspend fun getUserRole(): String
    fun logout()
    suspend fun isRememberMe(): Boolean
    suspend fun setRememberMe(rememberMe: Boolean)
    fun refreshAuthState()
    suspend fun register(email: String, password: String): AuthResult
    suspend fun getUserById(userId: String): User?
    suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult
    suspend fun loginAsGuest(): Pair<Boolean, String?>
    suspend fun resetPassword(email: String): Pair<Boolean, String?>
    fun getCurrentFirebaseUser(): FirebaseUser?
    suspend fun updatePassword(newPassword: String): Pair<Boolean, String?>
    val authStateFlow: StateFlow<AuthState>

    suspend fun updateUserName(newName: String): Pair<Boolean, String?>
    suspend fun reauthenticateAndDelete(email: String, password: String): Pair<Boolean, String?>
    suspend fun updateUserProfilePicture(imageUri: Uri): Pair<Boolean, String?>
}
