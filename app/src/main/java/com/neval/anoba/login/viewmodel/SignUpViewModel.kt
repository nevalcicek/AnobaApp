package com.neval.anoba.login.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.neval.anoba.models.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _confirmPasswordVisible = MutableStateFlow(false)
    val confirmPasswordVisible: StateFlow<Boolean> = _confirmPasswordVisible.asStateFlow()

    // Hata Mesajları için State'ler
    private val _usernameError = MutableStateFlow("")
    val usernameError: StateFlow<String> = _usernameError.asStateFlow()

    private val _emailError = MutableStateFlow("")
    val emailError: StateFlow<String> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow("")
    val passwordError: StateFlow<String> = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow("")
    val confirmPasswordError: StateFlow<String> = _confirmPasswordError.asStateFlow()

    private val _formValid = MutableStateFlow(false)
    val formValid: StateFlow<Boolean> = _formValid.asStateFlow()

    private val _signUpState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    // --- Güncelleme Fonksiyonları ---
    fun updateUsername(newUsername: String) {
        _username.update { newUsername.trim() }
        validateUsername()
        validateForm()
    }
    fun updateEmail(newEmail: String) {
        _email.update { newEmail.trim() }
        validateEmail()
        validateForm()
    }
    fun updatePassword(newPassword: String) {
        _password.update { newPassword.trim() }
        validatePassword()
        validateConfirmPassword()
        validateForm()
    }
    fun updateConfirmPassword(newConfirmPassword: String) {
        _confirmPassword.update { newConfirmPassword.trim() }
        validateConfirmPassword()
        validateForm()
    }
    fun togglePasswordVisibility() {
        _passwordVisible.update { !it }
    }
    fun toggleConfirmPasswordVisibility() {
        _confirmPasswordVisible.update { !it }
    }
    // --- Doğrulama Fonksiyonları ---
    private fun validateUsername() {
        _usernameError.value = if (_username.value.length < 3) {
            "Kullanıcı adı en az 3 karakter olmalıdır."
        } else {
            ""
        }
    }
    private fun validateEmail() {
        _emailError.value = if (!Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()) {
            "Geçerli bir e-posta adresi giriniz."
        } else {
            ""
        }
    }
    private fun validatePassword() {
        _passwordError.value = if (_password.value.length < 6) {
            "Şifre en az 6 karakter olmalıdır."
        } else {
            ""
        }
    }
    private fun validateConfirmPassword() {
        _confirmPasswordError.value = if (_password.value != _confirmPassword.value) {
            "Şifreler eşleşmiyor."
        } else {
            ""
        }
    }
    fun validateForm() {
        val isUsernameValid = _username.value.length >= 3
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()
        val isPasswordValid = _password.value.length >= 6
        val isConfirmPasswordValid = _password.value == _confirmPassword.value

        _formValid.value =
            isUsernameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid &&
                    _usernameError.value.isEmpty() &&
                    _emailError.value.isEmpty() &&
                    _passwordError.value.isEmpty() &&
                    _confirmPasswordError.value.isEmpty()
    }
    //kullanıcı adı üretme fonksiyonu.
    private suspend fun generateUniqueUsername(baseUsername: String): String {
        val usernamesCollection = firestore.collection("kullanici_adlari")
        val countersCollection = firestore.collection("ad_numaralari")
        return firestore.runTransaction { transaction ->
            val counterRef = countersCollection.document(baseUsername.lowercase())
            val counterDoc = transaction.get(counterRef)
            var nextNumber = (counterDoc.getLong("son_numara") ?: 0L) + 1
            var potentialUsername: String

            // Kullanılmamış bir kullanıcı adı bulana kadar döngüye gir.
            while (true) {
                potentialUsername = if (nextNumber == 1L) baseUsername else "$baseUsername$nextNumber"
                val usernameRef = usernamesCollection.document(potentialUsername.lowercase())
                val usernameDoc = transaction.get(usernameRef)

                if (!usernameDoc.exists()) {
                    // Bu kullanıcı adı boşta, alabiliriz.
                    break
                } else {
                    // Bu kullanıcı adı dolu, bir sonrakini dene.
                    nextNumber++
                }
            }

            // Kullanıcı adını rezerve et.
            transaction.set(usernamesCollection.document(potentialUsername.lowercase()), mapOf("reserved" to true))

            // Sayacı güncelle.
            transaction.set(counterRef, mapOf("son_numara" to nextNumber))

            potentialUsername // Başarılı, üretilen kullanıcı adını döndür.
        }.await()
    }
    fun performRegistration() {
        validateForm()
        if (!_formValid.value) {
            _signUpState.value = SignUpUiState.Error("Lütfen formdaki hataları düzeltin.")
            return
        }

        _signUpState.value = SignUpUiState.Loading
        viewModelScope.launch { 
            try {
                // 1. Kayıt işlemi
                auth.createUserWithEmailAndPassword(_email.value, _password.value).await()

                // 2. Hemen giriş yap
                val loginResult = auth.signInWithEmailAndPassword(_email.value, _password.value).await()
                val firebaseUser = loginResult.user ?: throw Exception("Kullanıcı oturumu alınamadı.")
                val baseUsername = _username.value.trim().ifBlank { "kullanici" }
                val finalUsername = generateUniqueUsername(baseUsername)

                // 4. Profil güncelle
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(finalUsername)
                    .build()
                firebaseUser.updateProfile(profileUpdate).await()

                // 5. Token yenile (kurallar için kritik)
                firebaseUser.getIdToken(true).await()

                // 6. Firestore veri yazımı
                val publicUserData = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "userId" to firebaseUser.uid,
                    "username" to finalUsername,
                    "displayName" to finalUsername,
                    "photoURL" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "role" to UserRole.MEMBER.name,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(firebaseUser.uid)
                    .set(publicUserData, SetOptions.merge()).await()
                val privateUserDataMap = hashMapOf("email" to firebaseUser.email)
                firestore.collection("privateUserData").document(firebaseUser.uid)
                    .set(privateUserDataMap).await()
                val signupData = hashMapOf(
                    "username" to finalUsername,
                    "email" to firebaseUser.email,
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("signups").document(firebaseUser.uid)
                    .set(signupData, SetOptions.merge()).await()

                // 7. Oturumu kapat
                auth.signOut()

                // 8. Başarı bildirimi
                _signUpState.value = SignUpUiState.Success("Kayıt başarılı: $finalUsername")
            } catch (e: Exception) {
                _signUpState.value =
                    SignUpUiState.Error("Kayıt sırasında bir hata oluştu: ${e.localizedMessage}")
                Log.e("SignUpViewModel", "Registration failed", e)
            }
        }
    }
    sealed interface SignUpUiState {
        object Idle : SignUpUiState
        object Loading : SignUpUiState
        data class Success(val message: String) : SignUpUiState
        data class Error(val message: String) : SignUpUiState
    }
}
