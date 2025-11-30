package com.neval.anoba.ses.sescomment

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class SesCommentRepository(private val firestore: FirebaseFirestore) {

    private fun getCommentsCollection(sesId: String) =
        firestore.collection("ses").document(sesId).collection("comments")

    fun getCommentsStream(sesId: String): Flow<List<SesComment>> {
        return getCommentsCollection(sesId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addComment(sesId: String, comment: SesComment) {
        getCommentsCollection(sesId).add(comment).await()
        firestore.collection("ses").document(sesId).update("commentCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(sesId: String, commentId: String) {
        getCommentsCollection(sesId).document(commentId).delete().await()
        firestore.collection("ses").document(sesId).update("commentCount", FieldValue.increment(-1)).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun toggleCommentLike(sesId: String, commentId: String, userId: String) {
        val commentRef = getCommentsCollection(sesId).document(commentId)
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