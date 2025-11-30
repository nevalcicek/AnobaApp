package com.neval.anoba.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.common.utils.EmailValidator
import com.neval.anoba.login.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    val authService: AuthServiceInterface,
    private val emailValidator: EmailValidator,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    // "Beni Hatırla" kutusu işaretsiz başlar.
    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _emailError = MutableStateFlow("")
    val emailError: StateFlow<String> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow("")
    val passwordError: StateFlow<String> = _passwordError.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginResultMessage = MutableStateFlow<String?>(null)
    val loginResultMessage: StateFlow<String?> = _loginResultMessage.asStateFlow()

    private val _guestLoginResultMessage = MutableStateFlow<String?>(null)
    val guestLoginResultMessage: StateFlow<String?> = _guestLoginResultMessage.asStateFlow()

    fun onLoginResultMessageShown() {
        _loginResultMessage.value = null
    }

    fun onGuestLoginResultMessageShown() {
        _guestLoginResultMessage.value = null
    }

    fun updateEmail(newEmail: String) {
        _email.value = newEmail.trim()
        validateEmail()
        validateForm()
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword.trim()
        validatePassword()
        validateForm()
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    fun onRememberMeChanged(isChecked: Boolean) {
        _rememberMe.value = isChecked
    }

    fun validateEmail(): Boolean {
        val currentEmail = _email.value
        val isValid = emailValidator.isValid(currentEmail)
        _emailError.value = if (!isValid) "Geçersiz e-posta adresi" else ""
        return isValid
    }

    private fun validatePassword() {
        _passwordError.value = when {
            _password.value.isEmpty() -> "Şifre alanı boş bırakılamaz"
            _password.value.length < 6 -> "Şifre en az 6 karakter olmalıdır"
            else -> ""
        }
    }

    fun validateForm() {
        val emailValid = validateEmail()
        validatePassword()
        _isValid.value =
            emailValid &&
                    _passwordError.value.isEmpty() &&
                    _email.value.isNotEmpty() &&
                    _password.value.isNotEmpty()

        Log.d("LoginViewModel", "Form validation: isValid = ${_isValid.value}")
    }

    fun login() {
        validateForm()

        if (!_isValid.value) {
            _loginResultMessage.value = "Lütfen formdaki hataları düzeltin."
            return
        }

        _isLoading.value = true

        viewModelScope.launch {

            val result = authService.login(_email.value, _password.value, _rememberMe.value)

            when (result) {
                is AuthResult.Success -> {
                    // ✅ “Beni Hatırla” değerini DataStore’a kaydet
                    userRepository.setRememberMe(_rememberMe.value)
                    _loginResultMessage.value = "Giriş başarılı"
                }
                is AuthResult.Error -> {
                    _loginResultMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            _isLoading.value = true
            _guestLoginResultMessage.value = null

            val (success, message) = authService.loginAsGuest()

            _guestLoginResultMessage.value = if (success) {
                message ?: "Misafir girişi başarılı!"
            } else {
                message ?: "Misafir girişi başarısız."
            }
            _isLoading.value = false
        }
    }
}