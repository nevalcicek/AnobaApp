package com.neval.anoba.ses

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.login.state.AuthState
import com.neval.anoba.photo.ShareState
import com.neval.anoba.ses.sesemoji.SesEmojiReactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File

class SesViewModel(
    private val sesRepository: SesRepository,
    private val authService: AuthServiceInterface,
    private val reactionRepository: SesEmojiReactionRepository
) : ViewModel() {

    private val _sesler = MutableStateFlow<List<SesModel>>(emptyList())
    val sesler: StateFlow<List<SesModel>> = _sesler.asStateFlow()

    private val _sesDetails = MutableStateFlow<SesModel?>(null)
    val sesDetails: StateFlow<SesModel?> = _sesDetails.asStateFlow()

    private val _userReactions = MutableStateFlow<Map<String, String>>(emptyMap())
    val userReactions: StateFlow<Map<String, String>> = _userReactions.asStateFlow()

    private val _sesDeleted = MutableStateFlow(false)
    val sesDeleted: StateFlow<Boolean> = _sesDeleted.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()
    
    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    var audioFile: File? = null
    private val userId: String get() = authService.getCurrentUserId()

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

    fun loadSesler() {
        viewModelScope.launch {
            sesRepository.getSesler().collect { sesList ->
                sesList.forEach { ses ->
                    Log.d("SesViewModel_Debug", "Ses ID: ${ses.id}, Comment Count: ${ses.commentCount}")
                }
                _sesler.value = sesList
            }
        }
    }

    fun loadSesDetails(sesId: String) {
        viewModelScope.launch {
            try {
                sesRepository.getSes(sesId)
                    .catch { _sesDetails.value = null }
                    .collect { _sesDetails.value = it }
            } catch (e: Exception) {
                Log.e("SesViewModel", "Error getting ses details", e)
                _sesDetails.value = null
            }
        }
    }

    fun startRecording(context: Context) {
        val fileName = "${context.cacheDir.absolutePath}/audiorecord.m4a"
        audioFile = File(fileName)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(fileName)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("SesViewModel", "Kayıt başlatılamadı: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e("SesViewModel", "Kayıt durdurulamadı: ${e.message}")
            }
        }
        mediaRecorder = null
    }

    fun cancelRecording() {
        audioFile?.delete()
        audioFile = null
    }

    fun uploadAudioToFirebase(file: File) {
        val user = authService.getCurrentFirebaseUser() ?: return
        val username = user.displayName ?: "Anonim"

        viewModelScope.launch {
            _sending.value = true
            try {
                val duration = getAudioDuration(file.absolutePath)
                sesRepository.uploadAudio(user.uid, username, file, duration)
            } catch (e: Exception) {
                Log.e("SesViewModel", "Yükleme hatası", e)
            } finally {
                _sending.value = false
            }
        }
    }

    fun deleteSes(sesId: String) {
        viewModelScope.launch {
            if (sesRepository.deleteSes(sesId)) {
                _sesDeleted.value = true
            }
        }
    }

    fun resetDeleteStatus() {
        _sesDeleted.value = false
    }

    fun incrementViewCount(sesId: String) {
        viewModelScope.launch {
            sesRepository.incrementViewCount(sesId)
        }
    }

    private fun loadUserReactions(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                val reactions = reactionRepository.getUserSesReactions(userId)
                _userReactions.value = reactions
            } catch (e: Exception) {
                Log.e("SesViewModel", "Error getting user reactions", e)
            }
        }
    }

    fun toggleReaction(sesId: String, emoji: String) {
        val id = userId
        if (id.isBlank()) return

        viewModelScope.launch {
            val currentReactions = _userReactions.value.toMutableMap()
            val existingReaction = currentReactions[sesId]

            if (existingReaction == emoji) {
                currentReactions.remove(sesId)
            } else {
                currentReactions[sesId] = emoji
            }
            _userReactions.value = currentReactions

            try {
                reactionRepository.toggleReaction(id, sesId, emoji)
            } catch (_: Exception) {
                if(id.isNotBlank()) loadUserReactions(id)
            }
        }
    }

    private fun getAudioDuration(filePath: String): Long {
        return try {
            MediaPlayer().run {
                setDataSource(filePath)
                prepare()
                duration.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }
    
    fun shareSes(context: Context, audioUrl: String, sesId: String) {
        viewModelScope.launch {
            _shareState.value = ShareState.Processing
            try {
                val uri = sesRepository.getAudioUriForSharing(context, audioUrl, sesId)
                _shareState.value = ShareState.Success(uri)
            } catch (e: Exception) {
                _shareState.value = ShareState.Error(e.message ?: "Ses paylaşılırken bir hata oluştu.")
            }
        }
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
