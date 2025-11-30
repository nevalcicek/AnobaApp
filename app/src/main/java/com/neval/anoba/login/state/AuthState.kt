package com.neval.anoba.login.state

sealed class AuthState {
    data class Authenticated(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
        val registrationDate: String?
    ) : AuthState()

    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
