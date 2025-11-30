package com.neval.anoba.video.videoemoji

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Kullanıcıların videolara verdiği reaksiyonları yönetir.
 * Veritabanı yolu: /userReactions/{userId}/video_reactions/{videoId}
 * ve /videos/{videoId}
 */
class VideoEmojiReactionRepository(private val firestore: FirebaseFirestore) {

    /**
     * Bir kullanıcının belirli bir videoya verdiği reaksiyonun referansını alır.
     */
    private fun getUserVideoReactionRef(userId: String, videoId: String) =
        firestore.collection("userReactions").document(userId)
            .collection("video_reactions").document(videoId)

    /**
     * Ana video koleksiyonundaki belirli bir videonun referansını alır.
     */
    private fun getVideoRef(videoId: String) = firestore.collection("videos").document(videoId)

    /**
     * Kullanıcının bir videoya verdiği reaksiyonu değiştirir/yönetir.
     */
    suspend fun toggleReaction(userId: String, videoId: String, emoji: String) {
        val userReactionRef = getUserVideoReactionRef(userId, videoId)
        val videoRef = getVideoRef(videoId)

        firestore.runTransaction {
            transaction ->
            val userReactionSnapshot = transaction.get(userReactionRef)
            val currentEmoji = userReactionSnapshot.getString("emoji")

            if (currentEmoji == emoji) {
                // Kullanıcı aynı emojiye tekrar tıkladı, reaksiyonu geri al
                transaction.delete(userReactionRef)
                transaction.update(videoRef, "reactions.$emoji", FieldValue.increment(-1))
            } else {
                // Yeni bir reaksiyon veya farklı bir emojiye geçiş
                if (currentEmoji != null) {
                    // Eski reaksiyonu sayaçtan düşür
                    transaction.update(videoRef, "reactions.$currentEmoji", FieldValue.increment(-1))
                }
                // Yeni reaksiyonu ekle
                val newReaction = VideoReaction(userId = userId, videoId = videoId, emoji = emoji)
                transaction.set(userReactionRef, newReaction)
                transaction.update(videoRef, "reactions.$emoji", FieldValue.increment(1))
            }
            null
        }.await()
    }

    /**
     * Belirli bir kullanıcının tüm video reaksiyonlarını getirir.
     */
    suspend fun getUserVideoReactions(userId: String): Map<String, String> {
        val snapshot = firestore.collection("userReactions").document(userId)
            .collection("video_reactions").get().await()
        return snapshot.documents.mapNotNull { it.id to (it.getString("emoji") ?: "") }.toMap()
    }
}
