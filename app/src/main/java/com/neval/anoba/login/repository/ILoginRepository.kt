package com.neval.anoba.login.repository

import com.neval.anoba.login.AuthResult

interface ILoginRepository {

    suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    suspend fun saveUserLocally(userId: String, email: String, username: String)
    suspend fun clearUser()
}