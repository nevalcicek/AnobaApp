package com.neval.anoba.chat.group

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neval.anoba.chat.general.GeneralChatUser
import com.neval.anoba.chat.group.GroupUtils.copyToClipboard
import com.neval.anoba.common.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class GroupChatViewModel(
    private val userRepository: IUserRepository,
    firebaseAuth: FirebaseAuth
) : GroupBaseChatViewModel<GroupMessage>(firebaseAuth) {

    companion object {
        private const val LOCAL_TAG = "GroupChatViewModel"
    }
    private val groupChatRepository = GroupChatRepository()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _groups = MutableStateFlow<List<ChatGroup>>(emptyList())
    val groups: StateFlow<List<ChatGroup>> = _groups.asStateFlow()

    private val _activeGroup = MutableStateFlow<ChatGroup?>(null)
    val activeGroup: StateFlow<ChatGroup?> = _activeGroup.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var typingJob: Job? = null

    val currentGroupMessages: StateFlow<List<GroupMessage>> = activeGroup.flatMapLatest { group ->
        group?.id?.let { groupId ->
            groupChatRepository.getMessagesStreamForGroup(groupId).catch { e ->
                Log.e(LOCAL_TAG, "Error in messages stream for ${group.id}", e)
                _errorMessage.value = "Mesajlar alınırken hata: ${e.message}"
                emit(emptyList())
            }
        } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _groupMembers = MutableStateFlow<List<GeneralChatUser>>(emptyList())
    val groupMembers: StateFlow<List<GeneralChatUser>> = _groupMembers.asStateFlow()

    private val _allUsers = MutableStateFlow<List<GeneralChatUser>>(emptyList())
    val allUsers: StateFlow<List<GeneralChatUser>> = _allUsers.asStateFlow()

    init {
        currentUserId?.let { listenForGroups(it) }
        loadAllUsers()
    }

    private fun listenForGroups(userId: String) {
        viewModelScope.launch {
            groupChatRepository.getGroupsStreamForUser(userId).catch { exception ->
                Log.e(LOCAL_TAG, "Error listening for groups", exception)
            }.collect { groupList ->
                _groups.value = groupList
            }
        }
    }

    fun setActiveGroupId(groupId: String?) {
        if (groupId == null) {
            _activeGroup.value = null
            _groupMembers.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val foundGroup = groups.value.find { it.id == groupId } ?: groupChatRepository.getGroupDetails(groupId)
            withContext(Dispatchers.Main) {
                _activeGroup.value = foundGroup
            }
            foundGroup?.let { loadGroupMembers(it.id) }
        }
    }

    suspend fun createGroup(name: String, isPrivate: Boolean, description: String): String? {
        val ownerId = currentUserId ?: return null
        val newGroup = ChatGroup(
            name = name,
            description = description,
            ownerId = ownerId,
            isPrivate = isPrivate,
            members = listOf(ownerId)
        )
        return try {
            val createdGroup = groupChatRepository.createGroup(newGroup)
            createdGroup.id
        } catch (e: Exception) {
            Log.e(LOCAL_TAG, "Error creating group", e)
            null
        }
    }

    fun addMessageToCurrentGroup(content: String) {
        val groupId = activeGroup.value?.id ?: return
        val senderId = currentUserId ?: return

        val message = GroupMessage(
            senderId = senderId,
            senderName = currentUserDisplayName,
            content = content,
            groupId = groupId,
            timestamp = java.util.Date()
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                groupChatRepository.addMessageToGroup(groupId, message)
                withContext(Dispatchers.Main) {
                    _messageText.value = ""
                }
            } catch (e: Exception) {
                Log.e(LOCAL_TAG, "Error sending message to group $groupId", e)
            }
        }
    }

    override fun deleteSelectedMessages(chatContextId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedMessages.value.forEach { message ->
                groupChatRepository.deleteMessageFromGroup(chatContextId, message.id)
            }
            clearMessageSelection()
        }
    }

    override fun copySelectedMessagesToClipboard(context: Context) {
        val selected = selectedMessages.value
        if (selected.isNotEmpty()) {
            val textToCopy = selected.sortedBy { it.timestampMillis }.joinToString("\n") { "[${it.senderName}]: ${it.content}" }
            copyToClipboard(context, textToCopy, "Sohbet Mesajları")
            clearMessageSelection()
        }
    }

    fun updateGroupName(groupId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.updateGroupName(groupId, newName)
        }
    }
    fun updateGroupImage(groupId: String, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageUrl = groupChatRepository.uploadGroupImage(groupId, imageUri)
                groupChatRepository.updateGroupImageUrl(groupId, imageUrl)

                // Update local state to reflect the change immediately
                val currentGroup = _activeGroup.value
                if (currentGroup != null && currentGroup.id == groupId) {
                    _activeGroup.value = currentGroup.copy(imageUrl = imageUrl)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Grup resmi güncellenemedi: ${e.message}"
                Log.e(LOCAL_TAG, "Failed to update group image", e)
            }
        }
    }

    fun updateMessageText(newText: String) {
        _messageText.value = newText
        updateTypingStatus(true)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000) // 2 saniye bekle
            updateTypingStatus(false)
        }
    }

    fun onMessageSent() {
        typingJob?.cancel()
        updateTypingStatus(false)
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        val groupId = activeGroup.value?.id ?: return
        val userId = currentUserId ?: return
        val userName = currentUserDisplayName

        viewModelScope.launch(Dispatchers.IO) {
            try {
                groupChatRepository.updateTypingStatus(groupId, userId, userName, isTyping)
            } catch (e: Exception) {
                Log.e(LOCAL_TAG, "Error updating typing status", e)
            }
        }
    }

    fun markMessagesAsRead(messageIds: List<String>) {
        val groupId = activeGroup.value?.id ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            messageIds.forEach {
                try {
                    groupChatRepository.markMessageAsRead(groupId, it, userId)
                } catch (e: Exception) {
                    Log.e(LOCAL_TAG, "Error marking message $it as read", e)
                }
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.deleteGroup(groupId)
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val group = _activeGroup.value
                if (group != null && group.id == groupId) {
                    val members = groupChatRepository.getGroupMemberDetails(group.members)
                    withContext(Dispatchers.Main) {
                        _groupMembers.value = members
                    }
                }
            } catch (e: Exception) {
                Log.e(LOCAL_TAG, "Error loading group members for $groupId", e)
            }
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            userRepository.getAllUsersStream()
                .catch { e ->
                    Log.e(LOCAL_TAG, "Error loading all users from stream", e)
                    _errorMessage.value = "Kullanıcı listesi alınamadı."
                }
                .collect { usersFromRepo ->
                    val chatUsers = usersFromRepo.map { user ->
                        GeneralChatUser(
                            id = user.uid,
                            displayName = user.displayName,
                            email = user.email,
                            photoUrl = user.photoURL ?: ""
                        )
                    }
                    _allUsers.value = chatUsers
                }
        }
    }

    fun inviteUserToGroup(groupId: String, userIdToInvite: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.addUserToGroup(groupId, userIdToInvite)
            loadGroupMembers(groupId)
        }
    }

    fun removeUserFromGroup(groupId: String, userIdToRemove: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.removeUserFromGroup(groupId, userIdToRemove)
            loadGroupMembers(groupId)
        }
    }

    fun setGroupPrivacy(groupId: String, isPrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.setGroupPrivacy(groupId, isPrivate)
        }
    }

    fun updateGroupInfo(groupId: String, newName: String, newDescription: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.updateGroupInfo(groupId, newName, newDescription)
        }
    }

    fun muteGroup(groupId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.muteGroup(groupId, userId)
            _isMuted.value = true
        }
    }

    fun unmuteGroup(groupId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupChatRepository.unmuteGroup(groupId, userId)
            _isMuted.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser).isSuccess }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}
