package com.neval.anoba.login.repository

import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.login.AuthResult

class LoginRepository(
    private val authService: AuthServiceInterface
) : ILoginRepository {

    override suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult {
        return authService.login(email, password, rememberMe)
    }

    override suspend fun register(email: String, password: String): AuthResult {
        return authService.register(email, password)
    }
}
