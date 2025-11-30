package com.neval.anoba.common.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.login.state.AuthState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthServiceInterface,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sonNumara = MutableStateFlow<Int?>(null)
    val sonNumara: StateFlow<Int?> = _sonNumara.asStateFlow()

    private val _userRole = MutableStateFlow("GUEST")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val isAdmin: StateFlow<Boolean> = userRole
        .mapLatest { it.equals("ADMIN", ignoreCase = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserId: StateFlow<String?> = authService.authStateFlow
        .mapLatest { state -> (state as? AuthState.Authenticated)?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    sealed class ViewModelState {
        object Loading : ViewModelState()
        data class Authenticated(
            val displayName: String?,
            val email: String?,
            val photoUrl: String?,
            val registrationDate: String?,
            val userNumber: Int?
        ) : ViewModelState()

        data class LoggedOut(val message: String? = null) : ViewModelState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userName: StateFlow<String> = authService.authStateFlow
        .mapLatest { serviceState ->
            when (serviceState) {
                is AuthState.Authenticated -> serviceState.displayName ?: Constants.DEFAULT_USERNAME_GUEST
                else -> Constants.DEFAULT_USERNAME_GUEST
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.DEFAULT_USERNAME_GUEST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userEmail: StateFlow<String> = authService.authStateFlow
        .mapLatest { serviceState ->
            when (serviceState) {
                is AuthState.Authenticated -> serviceState.email ?: Constants.DEFAULT_EMAIL_NOT_FOUND
                else -> Constants.DEFAULT_EMAIL_NOT_FOUND
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.DEFAULT_EMAIL_NOT_FOUND)

    @OptIn(ExperimentalCoroutinesApi::class)
    val profileImageUrl: StateFlow<String> = authService.authStateFlow
        .mapLatest { serviceState ->
            when (serviceState) {
                is AuthState.Authenticated -> serviceState.photoUrl ?: "https://via.placeholder.com/150"
                else -> "https://via.placeholder.com/150"
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://via.placeholder.com/150")

    @OptIn(ExperimentalCoroutinesApi::class)
    val registrationDate: StateFlow<String?> = authService.authStateFlow
        .mapLatest { serviceState ->
            when (serviceState) {
                is AuthState.Authenticated -> serviceState.registrationDate
                else -> null
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewModelState: StateFlow<ViewModelState> = authService.authStateFlow
        .combine(sonNumara) { serviceState, number ->
            when (serviceState) {
                is AuthState.Authenticated -> {
                    _isLoading.value = false
                    ViewModelState.Authenticated(
                        displayName = serviceState.displayName,
                        email = serviceState.email,
                        photoUrl = serviceState.photoUrl,
                        registrationDate = serviceState.registrationDate,
                        userNumber = number
                    )
                }

                is AuthState.Unauthenticated -> {
                    _isLoading.value = false
                    ViewModelState.LoggedOut()
                }

                is AuthState.Error -> {
                    _isLoading.value = false
                    ViewModelState.LoggedOut(message = serviceState.message)
                }

                is AuthState.Loading -> {
                    _isLoading.value = true
                    ViewModelState.Loading
                }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewModelState.Loading)

    private fun fetchUserNumber(uid: String) {
        viewModelScope.launch {
            try {
                _sonNumara.value = userRepository.getSonNumara(uid)
                Log.d("AuthViewModel", "Kullanıcı numarası başarıyla alındı: ${_sonNumara.value}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Kullanıcı numarası alınırken hata oluştu", e)
                _sonNumara.value = null
            }
        }
    }

    private fun fetchUserRole() {
        viewModelScope.launch {
            try {
                _userRole.value = authService.getUserRole()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Kullanıcı rolü alınırken hata oluştu", e)
                _userRole.value = "GUEST"
            }
        }
    }

    init {
        Log.d("AuthViewModel", "AuthViewModel initialized. Listening to AuthService state.")
        viewModelScope.launch {
            authService.authStateFlow.collect { serviceState ->
                if (serviceState is AuthState.Authenticated) {
                    fetchUserNumber(serviceState.uid)
                    fetchUserRole()
                } else {
                    _sonNumara.value = null
                    _userRole.value = "GUEST"
                }
            }
        }
    }

    suspend fun login(email: String, password: String, rememberMe: Boolean) {
        _isLoading.value = true
        authService.login(email, password, rememberMe)
        _isLoading.value = false
    }

    fun refreshAuthState() {
        Log.d("AuthViewModel", "AuthService'e durumu yenileme komutu gönderiliyor.")
        authService.refreshAuthState()
        val currentState = authService.authStateFlow.value
        if (currentState is AuthState.Authenticated) {
            fetchUserNumber(currentState.uid)
            fetchUserRole()
        }
    }

    fun logout(onLogoutFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authService.logout()
            } finally {
                _isLoading.value = false
                onLogoutFinished?.invoke()
            }
        }
    }
    fun triggerAuthCheck() {
        Log.d("AuthViewModel", "AuthService'e kimlik kontrolü tetikleniyor.")
        authService.refreshAuthState()
    }

    fun updateUserName(
        newName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val (success, message) = authService.updateUserName(newName)
            onResult(success, message)
            _isLoading.value = false
        }
    }

    fun reauthenticateAndDelete(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val (success, message) = authService.reauthenticateAndDelete(email, password)
            onResult(success, message)
            _isLoading.value = false
        }
    }

    fun updateUserProfilePicture(
        imageUri: Uri,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val (success, message) = authService.updateUserProfilePicture(imageUri)
            onResult(success, message)
            _isLoading.value = false
        }
    }
}
