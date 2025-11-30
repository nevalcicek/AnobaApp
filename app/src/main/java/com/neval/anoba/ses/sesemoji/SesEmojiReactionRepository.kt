package com.neval.anoba.ses.sesemoji

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Kullanıcıların seslere verdiği reaksiyonları yönetir.
 * Veritabanı yolu: /userReactions/{userId}/ses_reactions/{sesId}
 * ve /ses/{sesId}
 */
class SesEmojiReactionRepository(private val firestore: FirebaseFirestore) {

    /**
     * Bir kullanıcının belirli bir sese verdiği reaksiyonu alır.
     */
    private fun getUserSesReactionRef(userId: String, sesId: String) =
        firestore.collection("userReactions").document(userId)
            .collection("ses_reactions").document(sesId)

    /**
     * Ana ses koleksiyonundaki belirli bir sesin referansını alır.
     */
    private fun getSesRef(sesId: String) = firestore.collection("ses").document(sesId)

    /**
     * Kullanıcının bir sese verdiği reaksiyonu değiştirir/yönetir.
     */
    suspend fun toggleReaction(userId: String, sesId: String, emoji: String) {
        val userReactionRef = getUserSesReactionRef(userId, sesId)
        val sesRef = getSesRef(sesId)

        firestore.runTransaction {
            transaction ->
            val userReactionSnapshot = transaction.get(userReactionRef)
            val currentEmoji = userReactionSnapshot.getString("emoji")

            if (currentEmoji == emoji) {
                // Kullanıcı aynı emojiye tekrar tıkladı, reaksiyonu geri al
                transaction.delete(userReactionRef)
                transaction.update(sesRef, "reactions.$emoji", FieldValue.increment(-1))
            } else {
                // Yeni bir reaksiyon veya farklı bir emojiye geçiş
                if (currentEmoji != null) {
                    // Eski reaksiyonu sayaçtan düşür
                    transaction.update(sesRef, "reactions.$currentEmoji", FieldValue.increment(-1))
                }
                // Yeni reaksiyonu ekle
                val newReaction = SesReaction(userId = userId, sesId = sesId, emoji = emoji)
                transaction.set(userReactionRef, newReaction)
                transaction.update(sesRef, "reactions.$emoji", FieldValue.increment(1))
            }
            null
        }.await()
    }

    /**
     * Belirli bir kullanıcının tüm ses reaksiyonlarını getirir.
     */
    suspend fun getUserSesReactions(userId: String): Map<String, String> {
        val snapshot = firestore.collection("userReactions").document(userId)
            .collection("ses_reactions").get().await()
        return snapshot.documents.mapNotNull { it.id to (it.getString("emoji") ?: "") }.toMap()
    }
}
