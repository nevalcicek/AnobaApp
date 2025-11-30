package com.neval.anoba.chat.privatechat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neval.anoba.chat.general.GeneralChatUser
import com.neval.anoba.common.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PrivateChatUiState {
    data object Loading : PrivateChatUiState
    data class Success(val messages: List<PrivateMessage>) : PrivateChatUiState
    data class Error(val message: String) : PrivateChatUiState
}

class PrivateChatViewModel(
    private val privateChatRepository: PrivateChatRepository,
    firebaseAuth: FirebaseAuth,
    private val userRepository: IUserRepository
) : PrivateBaseChatViewModel<PrivateMessage>(firebaseAuth) {

    companion object {
        private const val LOCAL_TAG = "PrivateChatViewModel"
    }

    private val _editingMessage = MutableStateFlow<PrivateMessage?>(null)
    val editingMessage: StateFlow<PrivateMessage?> = _editingMessage.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _chatPartnerProfile = MutableStateFlow<GeneralChatUser?>(null)
    val chatPartnerProfile: StateFlow<GeneralChatUser?> = _chatPartnerProfile.asStateFlow()

    private val _uiState =
        MutableStateFlow<PrivateChatUiState>(PrivateChatUiState.Success(emptyList()))
    val uiState: StateFlow<PrivateChatUiState> = _uiState.asStateFlow()

    val userList = MutableStateFlow<List<GeneralChatUser>>(emptyList())

    fun resetChatState() {
        _uiState.value = PrivateChatUiState.Success(emptyList())
        _chatPartnerProfile.value = null
        _messageText.value = ""
        cancelEditing()
    }

    fun updateMessageText(newText: String) {
        _messageText.value = newText
    }
    fun startEditingMessage(message: PrivateMessage) {
        _editingMessage.value = message
        _messageText.value = message.content
    }
    fun cancelEditing() {
        _editingMessage.value = null
        _messageText.value = ""
    }

    fun applyEdit(receiverId: String) {
        val editedMessage = _editingMessage.value ?: return
        val newContent = _messageText.value.trim()
        if (newContent.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                privateChatRepository.updatePrivateMessageContent(
                    senderId = currentUserId ?: return@launch,
                    receiverId = receiverId,
                    messageId = editedMessage.id,
                    newContent = newContent
                )
                withContext(Dispatchers.Main) {
                    _editingMessage.value = null
                    _messageText.value = ""
                }
            } catch (e: Exception) {
                Log.e(LOCAL_TAG, "applyEdit failed: ${e.message}")
            }
        }
    }

    fun addMessage(receiverId: String, content: String) {
        if (editingMessage.value != null) {
            applyEdit(receiverId)
            return
        }

        val currentSenderId = this.currentUserId ?: run {
            Log.e(LOCAL_TAG, "Sender ID is null. Cannot send message.")
            _uiState.value = PrivateChatUiState.Error("Oturum açılmadan mesaj gönderilemez.")
            return
        }

        if (receiverId.isBlank() || content.trim().isEmpty()) {
            Log.d(LOCAL_TAG, "Receiver ID or message is invalid. Not sending.")
            return
        }

        val privateMessage = PrivateMessage(
            senderId = currentSenderId,
            senderName = this.currentUserDisplayName,
            receiverId = receiverId,
            receiverName = chatPartnerProfile.value?.displayName ?: "Bilinmeyen",
            content = content.trim(),
            timestampMillis = System.currentTimeMillis(),
            status = "SENT"
        )

        Log.d(LOCAL_TAG, "Attempting to send message via repository: $privateMessage")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                privateChatRepository.sendPrivateMessage(privateMessage)
                Log.i(LOCAL_TAG, "Message sent successfully to receiver: $receiverId")
                withContext(Dispatchers.Main) {
                    _messageText.value = ""
                }
            } catch (e: Exception) {
                Log.e(LOCAL_TAG, "Error sending message via repository to $receiverId", e)
                withContext(Dispatchers.Main) {
                    _uiState.value =
                        PrivateChatUiState.Error("Mesaj gönderilirken hata: ${e.localizedMessage}")
                }
            }
        }
    }

    fun listenForMessages(otherUserId: String) {
        val currentUserId = this.currentUserId
        loadChatPartnerProfile(otherUserId)

        if (currentUserId.isNullOrBlank() || otherUserId.isBlank()) {
            val errorMsg = if (currentUserId.isNullOrBlank()) "Geçerli kullanıcı kimliği eksik." else "Partner kullanıcı kimliği eksik."
            _uiState.value = PrivateChatUiState.Error(errorMsg)
            Log.e(LOCAL_TAG, "listenForMessages: $errorMsg")
            return
        }

        Log.d(LOCAL_TAG, "listenForMessages: CurrentUser: $currentUserId, OtherUser: $otherUserId")
        viewModelScope.launch {
            _uiState.value = PrivateChatUiState.Loading

            privateChatRepository.getMessagesStream(currentUserId, otherUserId)
                .catch { e ->
                    Log.e(LOCAL_TAG, "Error in messages stream for $currentUserId & $otherUserId", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value =
                            PrivateChatUiState.Error("Mesajlar alınırken hata: ${e.message}")
                    }
                }
                .collect { newMessages ->
                    Log.d(LOCAL_TAG, "New messages received for $currentUserId & $otherUserId: Count = ${newMessages.size}")
                    _uiState.value = PrivateChatUiState.Success(newMessages)
                }
        }
    }

    override fun deleteSelectedMessages(chatContextId: String) {
        val currentUserId = this.currentUserId ?: return
        val messagesToDelete = selectedMessages.value

        if (chatContextId.isBlank()) {
            Log.e(LOCAL_TAG, "Cannot delete messages: Partner ID is blank.")
            clearMessageSelection()
            return
        }
        if (messagesToDelete.isEmpty()) {
            Log.d(LOCAL_TAG, "No messages selected for deletion.")
            clearMessageSelection()
            return
        }

        Log.d(LOCAL_TAG, "Attempting to delete ${messagesToDelete.size} selected messages between $currentUserId and $chatContextId")
        viewModelScope.launch(Dispatchers.IO) {
            messagesToDelete.forEach { message ->
                if (message.senderId == currentUserId) {
                    try {
                        privateChatRepository.deletePrivateMessage(currentUserId,
                            chatContextId, message.id)
                        Log.i(LOCAL_TAG, "Message ${message.id} deleted successfully.")
                    } catch (e: Exception) {
                        Log.e(LOCAL_TAG, "Error deleting message ${message.id}", e)
                    }
                } else {
                    Log.w(LOCAL_TAG, "Skipping deletion of message ${message.id} as it's not owned by the current user.")
                }
            }
            withContext(Dispatchers.Main) {
                clearMessageSelection()
            }
        }
    }

    private fun loadChatPartnerProfile(partnerUserId: String) {
        if (partnerUserId.isBlank()) {
            _chatPartnerProfile.value = null
            Log.w(LOCAL_TAG, "Partner User ID is blank. Cannot load profile.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userRepository.getUserById(partnerUserId) // DÜZELTME
                if (user == null) {
                    withContext(Dispatchers.Main) { _chatPartnerProfile.value = null }
                    return@launch
                }

                val chatUser = GeneralChatUser(
                    id = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoURL,
                    role = user.role
                )

                withContext(Dispatchers.Main) {
                    _chatPartnerProfile.value = chatUser
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _chatPartnerProfile.value = null
                }
            }
        }
    }

    fun loadAllUsersExceptCurrent() {
        val currentUserId = this.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allUsers = userRepository.getAllUsers() // DÜZELTME
                val filteredUsers = allUsers.mapNotNull { user ->
                    if (user.uid == currentUserId) return@mapNotNull null
                    GeneralChatUser(
                        id = user.uid,
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoURL,
                        role = user.role
                    )
                }
                withContext(Dispatchers.Main) {
                    userList.value = filteredUsers
                }
            } catch (e: Exception) {
                Log.e("PrivateChatViewModel", "Kullanıcı listesi alınamadı: ${e.message}", e)
            }
        }
    }
}
