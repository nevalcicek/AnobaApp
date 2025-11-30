package com.neval.anoba.login.viewmodel

import androidx.lifecycle.ViewModel
import com.neval.anoba.common.services.AuthServiceInterface

class NewPasswordViewModel(
    private val authService: AuthServiceInterface
) : ViewModel() {

    suspend fun updatePassword(newPassword: String): Pair<Boolean, String?> {
        return authService.updatePassword(newPassword)
    }
}
