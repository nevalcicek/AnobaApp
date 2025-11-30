package com.neval.anoba.letter.lettercomment

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

class LetterCommentViewModel(
    private val letterId: String,
    private val repository: LetterCommentRepository,
    private val authService: AuthServiceInterface
) : ViewModel() {

    private val _comments = MutableStateFlow<List<LetterComment>>(emptyList())
    val comments: StateFlow<List<LetterComment>> = _comments.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _replyingToComment = MutableStateFlow<LetterComment?>(null)
    val replyingToComment: StateFlow<LetterComment?> = _replyingToComment.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        listenForComments()
    }

    private fun listenForComments() {
        viewModelScope.launch {
            repository.getCommentsStream(letterId)
                .onStart { _isLoading.value = true } // Standart: Yükleme başlangıcı
                .catch { e ->
                    _error.value = "Yorumlar yüklenirken bir hata oluştu: ${e.message}"
                    _isLoading.value = false // Standart: Hata durumunda yüklemeyi bitir
                }
                .collect { commentList ->
                    _comments.value = commentList
                    _error.value = null
                    _isLoading.value = false // Standart: Yükleme tamamlandı
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
            val comment = LetterComment(
                letterId = letterId,
                userId = authState.uid,
                username = authState.displayName ?: "Anonim",
                content = content,
                replyToId = _replyingToComment.value?.id,
                userProfileUrl = authState.photoUrl
            )

            viewModelScope.launch {
                try {
                    repository.addComment(letterId, comment)
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

    fun onReplyClicked(comment: LetterComment) {
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
                    repository.toggleCommentLike(letterId, commentId, authState.uid)
                } catch (e: Exception) {
                    _error.value = "Beğeni işlemi sırasında bir hata oluştu: ${e.message}"
                }
            }
        } else {
            _error.value = "Beğenmek için giriş yapmalısınız."
        }
    }

    fun deleteComment(comment: LetterComment) {
        val authState = authService.authStateFlow.value
        if (authState is AuthState.Authenticated && authState.uid == comment.userId) {
            viewModelScope.launch {
                try {
                    repository.deleteComment(letterId, comment.id)
                } catch (e: Exception) {
                    _error.value = "Yorum silinirken bir hata oluştu: ${e.message}"
                }
            }
        } else {
            _error.value = "Bu yorumu silme yetkiniz yok."
        }
    }
}
