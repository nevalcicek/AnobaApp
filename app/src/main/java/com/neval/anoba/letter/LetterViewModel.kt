package com.neval.anoba.letter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.repository.IUserRepository
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.letter.letteremoji.LetterEmojiReactionRepository
import com.neval.anoba.login.state.AuthState
import com.neval.anoba.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LetterViewModel(
    private val authService: AuthServiceInterface, 
    private val letterRepository: LetterRepository,
    private val reactionRepository: LetterEmojiReactionRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _letters = MutableStateFlow<List<LetterModel>>(emptyList())
    val letters: StateFlow<List<LetterModel>> = _letters.asStateFlow()
    private val _selectedLetter = MutableStateFlow<LetterModel?>(null)
    val selectedLetter: StateFlow<LetterModel?> = _selectedLetter.asStateFlow()
    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()
    private val userId: String? get() = (authService.authStateFlow.value as? AuthState.Authenticated)?.uid

    init {
        viewModelScope.launch {
            authService.authStateFlow.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> loadUserReactions(authState.uid)
                    else -> _userReactions.value = emptyMap() // Çıkış yapıldığında reaksiyonları temizle
                }
            }
        }
        loadAllUsers() // ViewModel başlatıldığında kullanıcıları dinlemeye başla
    }
    
    private fun loadAllUsers() {
        viewModelScope.launch {
            userRepository.getAllUsersStream()
                .catch { e ->
                    Log.e("LetterViewModel", "Error loading all users from stream", e)
                }
                .collect { users ->
                    _allUsers.value = users
                }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            letterRepository.getLetters().collect { letterList ->
                _letters.value = letterList
            }
        }
    }

    fun getLetterDetails(letterId: String) {
        viewModelScope.launch {
            letterRepository.getLetterById(letterId)
                .catch { _selectedLetter.value = null }
                .collect { _selectedLetter.value = it }
        }
    }

    fun sendLetter(
        content: String,
        privacy: LetterPrivacy,
        recipientId: String? = null,
        recipientUsername: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        val user = authService.getCurrentFirebaseUser() ?: run {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            val letter = LetterModel(
                ownerId = user.uid,
                username = user.displayName ?: "Anonim",
                content = content,
                privacy = privacy.name,
                recipientId = recipientId,
                recipientUsername = recipientUsername
            )
            val success = letterRepository.sendLetter(letter)
            _isSending.value = false
            onResult(success)
        }
    }

    fun deleteLetter(letterId: String) {
        viewModelScope.launch {
            letterRepository.deleteLetter(letterId)
        }
    }

    fun incrementLetterViewCount(letterId: String) {
        viewModelScope.launch {
            letterRepository.incrementLetterViewCount(letterId)
        }
    }

    private fun loadUserReactions(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                reactionRepository.getUserLetterReactions(userId).collect { reactions ->
                    _userReactions.value = reactions
                }
            } catch (e: Exception) {
                Log.e("LetterViewModel", "Error loading user reactions for $userId", e)
                _userReactions.value = emptyMap() // Hata durumunda temizle
            }
        }
    }

    fun toggleReaction(letterId: String, emoji: String) {
        val currentUserId = this.userId ?: return
        viewModelScope.launch {
            val currentReactions = _userReactions.value.toMutableMap()
            val existingReaction = currentReactions[letterId]

            if (existingReaction == emoji) {
                currentReactions.remove(letterId)
            } else {
                currentReactions[letterId] = emoji
            }
            _userReactions.value = currentReactions

            try {
                reactionRepository.toggleReaction(currentUserId, letterId, emoji)
            } catch (e: Exception) {
                Log.e("LetterViewModel", "Failed to toggle reaction for letter $letterId", e)
                // Hata durumunda, güvenli bir şekilde reaksiyonları yeniden yükle
                loadUserReactions(currentUserId)
            }
        }
    }
}
