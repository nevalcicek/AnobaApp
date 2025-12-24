package com.neval.anoba.common.services

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.login.AuthResult
import com.neval.anoba.login.state.AuthState
import com.neval.anoba.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthService(
    private val appUserRepository: IUserRepository,
    private val coroutineScope: CoroutineScope,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthServiceInterface {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authStateFlow: StateFlow<AuthState> = _authState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        coroutineScope.launch {
            if (firebaseUser != null) {
                try {
                    createUserDocumentIfMissing(firebaseUser)
                    val regDateString = getRegistrationDateString(firebaseUser)
                    _authState.value = AuthState.Authenticated(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "Misafir",
                        email = firebaseUser.email,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        registrationDate = regDateString
                    )
                    Log.d("AuthService", "AuthStateListener: State Authenticated olarak güncellendi: ${firebaseUser.uid}")
                } catch (e: Exception) {
                    Log.e("AuthService", "AuthStateListener içinde Firestore işlemi sırasında hata", e)
                    _authState.value = AuthState.Error("Kullanıcı verisi doğrulanırken bir hata oluştu.")
                }
            } else {
                _authState.value = AuthState.Unauthenticated
                Log.d("AuthService", "AuthStateListener: State Unauthenticated olarak güncellendi.")
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        Log.d("AuthService", "AuthService başlatıldı ve AuthStateListener eklendi.")
    }

    override fun refreshAuthState() {
        coroutineScope.launch {
            Log.d("AuthService", "refreshAuthState() manuel olarak tetiklendi.")
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val regDateString = getRegistrationDateString(currentUser)
                    _authState.value = AuthState.Authenticated(
                        uid = currentUser.uid,
                        displayName = currentUser.displayName ?: "Misafir",
                        email = currentUser.email,
                        photoUrl = currentUser.photoUrl?.toString(),
                        registrationDate = regDateString
                    )
                    Log.d("AuthService", "refreshAuthState: State manuel olarak Authenticated'a güncellendi: ${currentUser.uid}")
                } else if (_authState.value !is AuthState.Unauthenticated) {
                    _authState.value = AuthState.Unauthenticated
                    Log.d("AuthService", "refreshAuthState: State manuel olarak Unauthenticated'a güncellendi.")
                }
            } catch (e: Exception) {
                Log.e("AuthService", "refreshAuthState sırasında hata", e)
                _authState.value = AuthState.Error("Kimlik doğrulama durumu kontrolünde hata oluştu.")
            }
        }
    }

    override fun logout() {
        coroutineScope.launch {
            Log.d("AuthService", "logout() called. Signing out from Firebase Auth.")
            auth.signOut()
            appUserRepository.logout()
            _authState.value = AuthState.Unauthenticated
            Log.d("AuthService", "Logout işlemi tamamlandı ve state Unauthenticated olarak ayarlandı.")
        }
    }

    override suspend fun login(email: String, password: String, rememberMe: Boolean): AuthResult {
        _authState.value = AuthState.Loading
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Kullanıcı nesnesi (Auth) null.")

            appUserRepository.setRememberMe(rememberMe)
            // AuthStateListener durumu ve kullanıcı belgesini yönetecek.
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Giriş başarısız."
            Log.e("AuthService", "Giriş (Auth) sırasında hata: $errorMessage", e)
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }

    private fun getRegistrationDateString(firebaseUser: FirebaseUser?): String? {
        return try {
            firebaseUser?.metadata?.creationTimestamp?.let { timestamp ->
                val date = Date(timestamp)
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Tarih formatlama hatası", e)
            null
        }
    }

    private suspend fun createUserDocumentIfMissing(firebaseUser: FirebaseUser) {
        val userDocRef = firestore.collection("users").document(firebaseUser.uid)
        try {
            val snapshot = userDocRef.get().await()
            if (!snapshot.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    userId = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: "Yeni Kullanıcı",
                    photoURL = firebaseUser.photoUrl?.toString(),
                    role = "MEMBER",
                    createdAt = Date()
                )
                userDocRef.set(newUser).await()
                Log.i("AuthService", "Firestore'a yeni kullanıcı eklendi: ${firebaseUser.email}")
            } else {
                if (!snapshot.contains("role")) {
                    userDocRef.update("role", "MEMBER").await()
                    Log.i("AuthService", "Mevcut kullanıcının eksik 'role' alanı 'MEMBER' olarak güncellendi.")
                }
            }
        } catch (e: Exception) {
            Log.e("AuthService", "Firestore kullanıcı ekleme/kontrol etme hatası (Ask Gemini)", e)
            _authState.value = AuthState.Error("Firestore kullanıcı verisi doğrulanırken hata oluştu.")
            throw e
        }
    }

    override fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    override fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return appUserRepository.getUserById(firebaseUser.uid)
    }

    override suspend fun getUserRole(): String {
        return determineUserRole(auth.currentUser)
    }

    private suspend fun determineUserRole(firebaseUser: FirebaseUser?): String {
        if (firebaseUser == null || firebaseUser.isAnonymous) return "GUEST"
        return getRoleFromFirestore(firebaseUser.uid) ?: "MEMBER"
    }

    private suspend fun getRoleFromFirestore(userId: String): String? {
        if (userId.isEmpty()) return null
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            snapshot.getString("role")
        } catch (e: Exception) {
            Log.e("AuthService", "getRoleFromFirestore error for $userId", e)
            null
        }
    }

    override suspend fun isRememberMe(): Boolean {
        return appUserRepository.isRememberMe()
    }

    override suspend fun setRememberMe(rememberMe: Boolean) {
        appUserRepository.setRememberMe(rememberMe)
    }

    override suspend fun getUserById(userId: String): User? {
        return appUserRepository.getUserById(userId)
    }

    override suspend fun resetPassword(email: String): Pair<Boolean, String?> {
        _authState.value = AuthState.Loading
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d("AuthService", "Password reset email sent to $email")
            Pair(true, "Şifre sıfırlama e-postası gönderildi.")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Şifre sıfırlama e-postası gönderilemedi."
            Log.e("AuthService", "Password reset failed: $errorMessage", e)
            _authState.value = AuthState.Error(errorMessage)
            Pair(false, errorMessage)
        }
    }

    override suspend fun register(email: String, password: String): AuthResult {
        return try {
            Log.d("AuthService", "register called for email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser =
                result.user ?: return AuthResult.Error("Kullanıcı nesnesi (Auth) null.")
            Log.d("AuthService", "Firebase user created successfully: ${firebaseUser.uid}")
            // AuthStateListener durumu ve kullanıcı belgesini yönetecek.
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            Log.e("AuthService", "Kayıt (Auth) sırasında hata: ${e.message}", e)
            AuthResult.Error(e.message ?: "Kayıt (Auth) başarısız.")
        }
    }

    override fun isLoggedInFirebase(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun loginAsGuest(): Pair<Boolean, String?> {
        _authState.value = AuthState.Loading
        return try {
            val (success, message) = appUserRepository.loginAsGuest()
            if (success) {
                appUserRepository.setRememberMe(false)
                // AuthStateListener durumu ve kullanıcı belgesini yönetecek.
                Pair(true, "Misafir girişi başarılı. ID: ${auth.currentUser?.uid}")
            } else {
                _authState.value = AuthState.Error(message ?: "Misafir girişi başarısız.")
                Pair(false, message)
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Misafir girişi başarısız."
            _authState.value = AuthState.Error(errorMessage)
            return Pair(false, errorMessage)
        }
    }

    override suspend fun updatePassword(newPassword: String): Pair<Boolean, String?> {
        val currentUser = auth.currentUser ?: return Pair(false, "Kullanıcı oturumu açık değil.")
        return try {
            currentUser.updatePassword(newPassword).await()
            Pair(true, "Şifre başarıyla güncellendi.")
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Şifre güncelleme başarısız."
            Pair(false, errorMessage)
        }
    }

    override suspend fun updateUserName(newName: String): Pair<Boolean, String?> {
        val firebaseUser = auth.currentUser ?: return Pair(false, "Kullanıcı oturumu bulunamadı.")

        return try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            firestore.collection("users").document(firebaseUser.uid)
                .update("displayName", newName).await()

            refreshAuthState()
            Pair(true, "Kullanıcı adı başarıyla güncellendi.")
        } catch (e: Exception) {
            val error = "Ad güncellenemedi: ${e.localizedMessage}"
            Log.e("AuthService", error, e)
            Pair(false, error)
        }
    }

    override suspend fun reauthenticateAndDelete(email: String, password: String): Pair<Boolean, String?> {
        val user = auth.currentUser ?: return Pair(false, "Kullanıcı oturumu bulunamadı.")

        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()

            user.delete().await()

            Pair(true, "Hesap başarıyla silindi.")
        } catch (e: Exception) {
            val error = "İşlem başarısız: ${e.localizedMessage}"
            Log.e("AuthService", error, e)
            Pair(false, error)
        }
    }

    override suspend fun updateUserProfilePicture(imageUri: Uri): Pair<Boolean, String?> {
        val firebaseUser = auth.currentUser ?: return Pair(false, "Kullanıcı oturumu bulunamadı.")
        return try {
            val photoUrl =
                appUserRepository.uploadProfileImage(firebaseUser.uid, imageUri) ?: return Pair(
                    false,
                    "Resim yüklenemedi."
                )

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUrl.toUri())
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val success = appUserRepository.updateUserProfilePhotoUrl(firebaseUser.uid, photoUrl)
            if (!success) {
                return Pair(false, "Veritabanı güncellenemedi.")
            }

            refreshAuthState()
            Pair(true, "Profil fotoğrafı başarıyla güncellendi.")
        } catch (e: Exception) {
            val error = "Profil fotoğrafı güncellenemedi: ${e.localizedMessage}"
            Log.e("AuthService", error, e)
            Pair(false, error)
        }
    }
}
