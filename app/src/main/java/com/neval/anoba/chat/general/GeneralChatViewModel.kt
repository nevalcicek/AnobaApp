package com.neval.anoba.chat.general

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GeneralChatViewModel(private val repository: GeneralChatRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<GeneralChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _selectedMessages = MutableStateFlow<Set<GeneralChatMessage>>(emptySet())
    val selectedMessages = _selectedMessages.asStateFlow()

    private val _undoState = MutableSharedFlow<List<GeneralChatMessage>>()
    val undoState = _undoState.asSharedFlow()

    private var lastDeletedMessages: List<GeneralChatMessage> = emptyList()
    private var lastRoomId: String? = null

    private var messageListenerJob: Job? = null

    fun loadChatMessages(roomId: String) {
        messageListenerJob?.cancel()
        messageListenerJob = viewModelScope.launch {
            repository.getMessagesStream(roomId)
                .catch { _ ->

                }
                .collect { messages ->
                    _messages.value = messages
                }
        }
    }

    fun addMessage(roomId: String, message: GeneralChatMessage) {
        viewModelScope.launch {
            repository.addMessage(roomId, message)
        }
    }

    fun editMessage(roomId: String, messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(roomId, messageId, newContent)
        }
    }

    fun deleteMessages(roomId: String, messagesToDelete: List<GeneralChatMessage>) {
        lastDeletedMessages = messagesToDelete
        lastRoomId = roomId
        viewModelScope.launch {
            messagesToDelete.forEach {
                repository.deleteMessage(roomId, it.id)
            }
            _undoState.emit(messagesToDelete)
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val roomId = lastRoomId
            if (lastDeletedMessages.isNotEmpty() && roomId != null) {
                repository.addMessages(roomId, lastDeletedMessages)
                lastDeletedMessages = emptyList()
                lastRoomId = null
            }
        }
    }

    // Admin için tüm mesajları silme fonksiyonu
    fun deleteAllMessagesForAdmin(roomId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAllMessages(roomId)

            } catch (_: Exception) {

            }
        }
    }

    fun onMessageClicked(message: GeneralChatMessage) {
        val currentSelection = _selectedMessages.value.toMutableSet()
        if (currentSelection.contains(message)) {
            currentSelection.remove(message)
        } else {
            currentSelection.add(message)
        }
        _selectedMessages.value = currentSelection
    }

    fun onMessageLongClicked(message: GeneralChatMessage) {
        val currentSelection = _selectedMessages.value.toMutableSet()
        currentSelection.add(message)
        _selectedMessages.value = currentSelection
    }

    fun clearMessageSelection() {
        _selectedMessages.value = emptySet()
    }

    fun copySelectedMessagesToClipboard(context: Context) {
        val selected = _selectedMessages.value
        if (selected.isEmpty()) return

        val textToCopy = selected
            .sortedBy { it.timestampMillis }
            .joinToString("\n") { "[${it.sender}] ${it.content}" }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Messages", textToCopy)
        clipboard.setPrimaryClip(clip)

        clearMessageSelection()
    }


    override fun onCleared() {
        super.onCleared()
        messageListenerJob?.cancel()
    }
}
