package com.neval.anoba.photo.photoemoji

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Kullanıcıların fotoğraflara verdiği reaksiyonları yönetir.
 * Veritabanı yolu: /userReactions/{userId}/photo_reactions/{photoId}
 * ve /photos/{photoId}
 */
class PhotoEmojiReactionRepository(private val firestore: FirebaseFirestore) {

    /**
     * Bir kullanıcının belirli bir fotoğrafa verdiği reaksiyonun referansını alır.
     */
    private fun getUserPhotoReactionRef(userId: String, photoId: String) =
        firestore.collection("userReactions").document(userId)
            .collection("photo_reactions").document(photoId)

    /**
     * Ana fotoğraf koleksiyonundaki belirli bir fotoğrafın referansını alır.
     */
    private fun getPhotoRef(photoId: String) = firestore.collection("photos").document(photoId)

    /**
     * Kullanıcının bir fotoğrafa verdiği reaksiyonu değiştirir/yönetir.
     */
    suspend fun toggleReaction(userId: String, photoId: String, emoji: String) {
        val userReactionRef = getUserPhotoReactionRef(userId, photoId)
        val photoRef = getPhotoRef(photoId)

        firestore.runTransaction {
            transaction ->
            val userReactionSnapshot = transaction.get(userReactionRef)
            val currentEmoji = userReactionSnapshot.getString("emoji")

            if (currentEmoji == emoji) {
                // Kullanıcı aynı emojiye tekrar tıkladı, reaksiyonu geri al
                transaction.delete(userReactionRef)
                transaction.update(photoRef, "reactions.$emoji", FieldValue.increment(-1))
            } else {
                // Yeni bir reaksiyon veya farklı bir emojiye geçiş
                if (currentEmoji != null) {
                    // Eski reaksiyonu sayaçtan düşür
                    transaction.update(photoRef, "reactions.$currentEmoji", FieldValue.increment(-1))
                }
                // Yeni reaksiyonu ekle
                val newReaction = PhotoReaction(userId = userId, photoId = photoId, emoji = emoji)
                transaction.set(userReactionRef, newReaction)
                transaction.update(photoRef, "reactions.$emoji", FieldValue.increment(1))
            }
            null
        }.await()
    }

    /**
     * Belirli bir kullanıcının tüm fotoğraf reaksiyonlarını getirir.
     */
    suspend fun getUserPhotoReactions(userId: String): Map<String, String> {
        val snapshot = firestore.collection("userReactions").document(userId)
            .collection("photo_reactions").get().await()
        return snapshot.documents.mapNotNull { it.id to (it.getString("emoji") ?: "") }.toMap()
    }
}
