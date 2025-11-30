package com.neval.anoba.livestream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

// import com.google.firebase.firestore.FirebaseFirestore // GERÇEK Entegrasyon için sonra eklenecek

class LiveStreamViewModel : ViewModel() {

    private val _currentLiveStream = MutableStateFlow<LiveStream?>(null)
    val currentLiveStream: StateFlow<LiveStream?> = _currentLiveStream.asStateFlow()

    // private lateinit var firestore: FirebaseFirestore // GERÇEK Entegrasyon için

    init {
        // firestore = FirebaseFirestore.getInstance() // GERÇEK Entegrasyon için
        listenToCurrentLiveStream()
    }

    private fun listenToCurrentLiveStream() {
        // ŞİMDİLİK SİMÜLASYON:
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // 5 saniye sonra başlasın
            _currentLiveStream.value = LiveStream(
                streamId = "sampleStream123",
                streamerId = "userABC",
                streamerName = "Acemi Yayıncı",
                title = "Test Yayını Başladı!",
                startTime = Date(),
                isActive = true
            )
            kotlinx.coroutines.delay(15000) // 15 saniye sürsün
            _currentLiveStream.value = null // Yayın bitti
        }

        // GERÇEK Firestore Dinleme Mantığı (Sonra Aktif Edilecek):
        /*
        firestore.collection("currentStage")
            .document("activeStream")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _currentLiveStream.value = null
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val stream = snapshot.toObject(LiveStream::class.java)
                    _currentLiveStream.value = if (stream?.isActive == true) stream else null
                } else {
                    _currentLiveStream.value = null
                }
            }
        */
    }

    fun requestStreamSlot() {
        println("Yayın talebi gönderildi (simülasyon)")
        // TODO: Kullanıcının yayın talebi gönderme mantığı (backend'e istek)
    }
}
