package com.neval.anoba.video

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.login.state.AuthState
import com.neval.anoba.video.videoemoji.VideoEmojiReactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class VideoUiEvent {
    data class ShowToast(val message: String) : VideoUiEvent()
}
class VideoViewModel(
    private val videoRepository: VideoRepository,
    private val reactionRepository: VideoEmojiReactionRepository,
    private val authService: AuthServiceInterface
) : ViewModel() {
    private val _videos = MutableStateFlow<List<VideoModel>>(emptyList())
    val videos: StateFlow<List<VideoModel>> = _videos.asStateFlow()

    private val _videoDetails = MutableStateFlow<VideoModel?>(null)
    val videoDetails: StateFlow<VideoModel?> = _videoDetails.asStateFlow()

    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _eventFlow = MutableSharedFlow<VideoUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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
    fun loadVideos() {
        viewModelScope.launch {
            videoRepository.getVideos().collect { loadedVideos ->
                _videos.value = loadedVideos
            }
        }
    }
    fun loadVideoDetails(videoId: String) {
        viewModelScope.launch {
            videoRepository.getVideo(videoId)
                .catch { _videoDetails.value = null }
                .collect { _videoDetails.value = it }
        }
    }
    private fun loadUserReactions(currentUserId: String) {
        viewModelScope.launch {
            if (currentUserId.isNotBlank()) {
                _userReactions.value = reactionRepository.getUserVideoReactions(currentUserId)
            }
        }
    }
    fun uploadVideo(videoUri: Uri, title: String, duration: Long, onComplete: (Boolean, String?) -> Unit) {
        val user = authService.getCurrentFirebaseUser() ?: run {
            onComplete(false, null)
            return
        }
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val newVideoId = videoRepository.uploadVideo(
                    userId = user.uid,
                    username = user.displayName ?: "Anonim",
                    videoUri = videoUri,
                    title = title,
                    duration = duration
                )
                onComplete(newVideoId != null, newVideoId)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            videoRepository.deleteVideo(videoId)
        }
    }

    fun toggleReaction(videoId: String, emoji: String) {
        val currentUserId = userId
        if (currentUserId.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(VideoUiEvent.ShowToast("Tepki vermek için lütfen giriş yapın.")) }
            return
        }

        viewModelScope.launch {
            val oldReactions = _userReactions.value
            val userPreviousReaction = oldReactions[videoId]

            val newReactions = oldReactions.toMutableMap()
            if (userPreviousReaction == emoji) {
                newReactions.remove(videoId)
            } else {
                newReactions[videoId] = emoji
            }
            _userReactions.value = newReactions

            try {
                reactionRepository.toggleReaction(currentUserId, videoId, emoji)
                if (userPreviousReaction == emoji) {
                    _eventFlow.emit(VideoUiEvent.ShowToast("Tepki geri alındı"))
                }
            } catch (e: Exception) {
                _userReactions.value = oldReactions
                _eventFlow.emit(VideoUiEvent.ShowToast("Hata: ${e.localizedMessage}"))
            }
        }
    }

    fun incrementViewCount(videoId: String) {
        viewModelScope.launch {
            videoRepository.incrementViewCount(videoId)
        }
    }
}
