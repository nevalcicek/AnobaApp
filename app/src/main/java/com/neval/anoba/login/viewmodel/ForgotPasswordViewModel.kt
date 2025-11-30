package com.neval.anoba.login.viewmodel

import androidx.lifecycle.ViewModel
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.common.utils.EmailValidator

class ForgotPasswordViewModel(
    private val authService: AuthServiceInterface,
    private val emailValidator: EmailValidator
) : ViewModel() {

    suspend fun resetPassword(email: String): Pair<Boolean, String?> {
        if (!emailValidator.isValid(email)) {
            return Pair(false, "Geçersiz e-posta adresi.")
        }
        return authService.resetPassword(email)
    }
}
