package com.neval.anoba.chat.general

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Mesajların ortak özelliklerini tanımlayan bir arayüz
interface Messageable {
    val id: String
    val senderId: String?
    val content: String?
    val timestampMillis: Long?
}

@Suppress("unused")
abstract class BaseChatViewModel<M : Messageable>(
    protected val firebaseAuth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val BASE_TAG = "BaseChatViewModel"
    }
    private val _selectedMessages = MutableStateFlow<Set<M>>(emptySet())
    val selectedMessages: StateFlow<Set<M>> = _selectedMessages.asStateFlow()

    val isSelectionModeActive: StateFlow<Boolean> = _selectedMessages
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    protected val currentUserDisplayName: String
        get() = firebaseAuth.currentUser?.displayName ?: firebaseAuth.currentUser?.email ?: "Siz"

    fun onMessageClicked(message: M) {
        if (isSelectionModeActive.value) {
            toggleMessageSelection(message)
        } else {
            Log.d(BASE_TAG, "Normal click on message: ${message.id} (Selection mode OFF)")
        }
    }
    fun onMessageLongClicked(message: M) {
        toggleMessageSelection(message)
        Log.d(BASE_TAG, "Long click on message: ${message.id}. Selection mode is now: ${isSelectionModeActive.value}")
    }
    private fun toggleMessageSelection(message: M) {
        viewModelScope.launch {
            val currentSelected = _selectedMessages.value.toMutableSet()
            val existing = currentSelected.find { it.id == message.id }
            if (existing != null) {
                currentSelected.remove(existing)
            } else {
                currentSelected.add(message)
            }
            _selectedMessages.value = currentSelected
        }
    }
    fun clearMessageSelection() {
        _selectedMessages.value = emptySet()
        Log.d(BASE_TAG, "Message selection cleared.")
    }
    open fun copySelectedMessagesToClipboard(context: Context) {
        val messagesToCopy = _selectedMessages.value
        if (messagesToCopy.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val textToCopy = messagesToCopy
                .sortedBy { it.timestampMillis ?: 0L }
                .joinToString("\n") { it.content ?: "" }
            val clip = ClipData.newPlainText("Copied Messages", textToCopy)
            clipboard.setPrimaryClip(clip)
            Log.d(BASE_TAG, "${messagesToCopy.size} messages copied to clipboard.")
        }
        clearMessageSelection()
    }
   abstract fun deleteSelectedMessages(chatContextId: String)
}
