package com.neval.anoba.letter.lettercomment

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class LetterCommentRepository(private val firestore: FirebaseFirestore) {

    private fun getCommentsCollection(letterId: String) =
        firestore.collection("letters").document(letterId).collection("comments")

    fun getCommentsStream(letterId: String): Flow<List<LetterComment>> {
        return getCommentsCollection(letterId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addComment(letterId: String, comment: LetterComment) {
        getCommentsCollection(letterId).add(comment).await()
        // Standart: Yorum sayısını artır
        firestore.collection("letters").document(letterId).update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(letterId: String, commentId: String) {
        getCommentsCollection(letterId).document(commentId).delete().await()
        // Standart: Yorum sayısını azalt
        firestore.collection("letters").document(letterId).update("commentsCount", FieldValue.increment(-1)).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun toggleCommentLike(letterId: String, commentId: String, userId: String) {
        val commentRef = getCommentsCollection(letterId).document(commentId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

            if (likedBy.contains(userId)) {
                transaction.update(commentRef, "likedBy", FieldValue.arrayRemove(userId))
            } else {
                transaction.update(commentRef, "likedBy", FieldValue.arrayUnion(userId))
            }
            null
        }.await()
    }
}
