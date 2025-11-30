package com.neval.anoba.letter.letteremoji

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LetterEmojiReactionRepository(private val firestore: FirebaseFirestore) {

    private fun getUserLetterReactionRef(userId: String, letterId: String) =
        firestore.collection("userReactions").document(userId)
            .collection("letter_reactions").document(letterId)

    private fun getLetterRef(letterId: String) = firestore.collection("letters").document(letterId)

    suspend fun toggleReaction(userId: String, letterId: String, emoji: String) {
        val userReactionRef = getUserLetterReactionRef(userId, letterId)
        val letterRef = getLetterRef(letterId)

        firestore.runTransaction {
            transaction ->
            val userReactionSnapshot = transaction.get(userReactionRef)
            val currentEmoji = userReactionSnapshot.getString("emoji")

            if (currentEmoji == emoji) {
                transaction.delete(userReactionRef)
                transaction.update(letterRef, "reactions.$emoji", FieldValue.increment(-1))
            } else {
                if (currentEmoji != null) {
                    transaction.update(letterRef, "reactions.$currentEmoji", FieldValue.increment(-1))
                }
                val newReaction = LetterReaction(userId = userId, letterId = letterId, emoji = emoji)
                transaction.set(userReactionRef, newReaction)
                transaction.update(letterRef, "reactions.$emoji", FieldValue.increment(1))
            }
            null
        }.await()
    }
    fun getUserLetterReactions(userId: String): Flow<Map<String, String>> {
        return callbackFlow {
            val listener = firestore.collection("userReactions")
                .document(userId)
                .collection("letter_reactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val reactions = snapshot.documents.mapNotNull { doc ->
                        val letterId = doc.getString("letterId")
                        val emoji = doc.getString("emoji")
                        if (letterId != null && emoji != null) letterId to emoji else null
                    }.toMap()

                    trySend(reactions)
                }

            awaitClose { listener.remove() }
        }
    }
}