package com.neval.anoba.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.login.state.AuthState
import com.neval.anoba.photo.photocomment.PhotoComment
import com.neval.anoba.photo.photocomment.PhotoCommentRepository
import com.neval.anoba.photo.photoemoji.PhotoEmojiReactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PhotoViewModel(
    private val authService: AuthServiceInterface,
    private val photoRepository: PhotoRepository,
    private val reactionRepository: PhotoEmojiReactionRepository,
    private val commentRepository: PhotoCommentRepository
) : ViewModel() {
    private val _photos = MutableStateFlow<List<PhotoModel>>(emptyList())
    val photos: StateFlow<List<PhotoModel>> = _photos.asStateFlow()

    private val _photoDetails = MutableStateFlow<PhotoModel?>(null)
    val photoDetails: StateFlow<PhotoModel?> = _photoDetails.asStateFlow()

    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()

    private val _comments = MutableStateFlow<List<PhotoComment>>(emptyList())
    val comments: StateFlow<List<PhotoComment>> = _comments.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    val userId: String get() = authService.getCurrentUserId()

    init {
        viewModelScope.launch {
            authService.authStateFlow.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> loadUserReactions(authState.uid)
                    else -> _userReactions.value = emptyMap()
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            photoRepository.getPhotos().collect { loadedPhotos ->
                _photos.value = loadedPhotos
            }
        }
    }

    fun loadPhotoDetails(photoId: String) {
        viewModelScope.launch {
            photoRepository.getPhoto(photoId)
                .catch { _photoDetails.value = null }
                .collect { _photoDetails.value = it }
        }
    }

    fun incrementViewCount(photoId: String) {
        viewModelScope.launch {
            photoRepository.incrementViewCount(photoId)
        }
    }

    private fun loadUserReactions(currentUserId: String) {
        viewModelScope.launch {
            if (currentUserId.isNotBlank()) {
                _userReactions.value = reactionRepository.getUserPhotoReactions(currentUserId)
            }
        }
    }

    fun uploadPhotoAndCreateRecord(photoUri: Uri, onComplete: (Boolean, String?) -> Unit) {
        val user = authService.getCurrentFirebaseUser() ?: run {
            onComplete(false, null)
            return
        }
        val username = user.displayName ?: "Anonim"
        viewModelScope.launch {
            _isUploading.value = true
            val newPhotoId = photoRepository.uploadPhoto(user.uid, username, photoUri, "")
            _isUploading.value = false
            onComplete(newPhotoId != null, newPhotoId)
        }
    }

    fun toggleReaction(photoId: String, emoji: String) {
        val currentUserId = userId
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            val oldReactions = _userReactions.value
            val newReactions = oldReactions.toMutableMap()
            if (oldReactions[photoId] == emoji) {
                newReactions.remove(photoId)
            } else {
                newReactions[photoId] = emoji
            }
            _userReactions.value = newReactions

            try {
                reactionRepository.toggleReaction(currentUserId, photoId, emoji)
            } catch (_: Exception) {
                _userReactions.value = oldReactions
            }
        }
    }

    fun loadComments(photoId: String) {
        viewModelScope.launch {
            commentRepository.getCommentsStream(photoId).collect { 
                _comments.value = it
            }
        }
    }

    fun addComment(photoId: String, text: String) {
        val user = authService.getCurrentFirebaseUser() ?: return

        viewModelScope.launch {
            val comment = PhotoComment(
                photoId = photoId,
                userId = user.uid,
                username = user.displayName ?: "Anonim",
                content = text,
                userProfileUrl = user.photoUrl?.toString()
            )
            commentRepository.addComment(photoId, comment)
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photoId)
        }
    }
}
