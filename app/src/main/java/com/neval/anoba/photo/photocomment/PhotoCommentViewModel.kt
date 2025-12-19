package com.neval.anoba.photo.photocomment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.login.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class PhotoCommentViewModel(
    private val photoId: String,
    private val repository: PhotoCommentRepository,
    private val authService: AuthServiceInterface
) : ViewModel() {

    private val _comments = MutableStateFlow<List<PhotoComment>>(emptyList())
    val comments: StateFlow<List<PhotoComment>> = _comments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _replyingToComment = MutableStateFlow<PhotoComment?>(null)
    val replyingToComment: StateFlow<PhotoComment?> = _replyingToComment.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        listenForComments()
    }
    private fun listenForComments() {
        viewModelScope.launch {
            repository.getCommentsStream(photoId)
                .onStart { _isLoading.value = true } // Yükleme başlangıcı
                .catch { e ->
                    _error.value = "Yorumlar yüklenirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false // Hata durumunda yüklemeyi bitir
                }
                .collect { commentList ->
                    _comments.value = commentList
                    _error.value = null
                    _isLoading.value = false // Yükleme tamamlandı
                }
        }
    }

    fun onCommentTextChanged(text: String) {
        _commentText.value = text
    }

    fun addComment() {
        val content = _commentText.value.trim()
        if (content.isEmpty()) return

        val authState = authService.authStateFlow.value
        if (authState is AuthState.Authenticated) {
            val comment = PhotoComment(
                photoId = photoId,
                userId = authState.uid,
                username = authState.displayName ?: "Anonim",
                content = content,
                replyToId = _replyingToComment.value?.id,
                userProfileUrl = authState.photoUrl
            )

            viewModelScope.launch {
                try {
                    repository.addComment(photoId, comment)
                    _commentText.value = ""
                    _replyingToComment.value = null
                } catch (e: Exception) {
                    _error.value = "Yorum gönderilirken bir hata oluştu: ${e.message}"
                }
            }
        } else {
            _error.value = "Yorum yapmak için giriş yapmalısınız."
        }
    }

    fun onReplyClicked(comment: PhotoComment) {
        _replyingToComment.value = comment
    }

    fun cancelReply() {
        _replyingToComment.value = null
    }

    fun likeComment(commentId: String) {
        val authState = authService.authStateFlow.value
        if (authState is AuthState.Authenticated) {
            viewModelScope.launch {
                try {
                    repository.toggleCommentLike(photoId, commentId, authState.uid)
                } catch (e: Exception) {
                    _error.value = "Beğeni işlemi sırasında bir hata oluştu: ${e.message}"
                }
            }
        } else {
            _error.value = "Beğenmek için giriş yapmalısınız."
        }
    }

    fun deleteComment(comment: PhotoComment, userRole: String?) {
        val authState = authService.authStateFlow.value
        if (authState is AuthState.Authenticated) {
            if (authState.uid == comment.userId || userRole == "ADMIN") {
                viewModelScope.launch {
                    try {
                        repository.deleteComment(photoId, comment.id)
                    } catch (e: Exception) {
                        _error.value = "Yorum silinirken bir hata oluştu: ${e.message}"
                    }
                }
            } else {
                _error.value = "Bu yorumu silme yetkiniz yok."
            }
        } else {
            _error.value = "Bu işlemi yapmak için giriş yapmalısınız."
        }
    }
}