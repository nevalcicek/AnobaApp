package com.neval.anoba.login

import com.google.firebase.auth.FirebaseUser
sealed class AuthResult {
    data class Success(val firebaseUser: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}