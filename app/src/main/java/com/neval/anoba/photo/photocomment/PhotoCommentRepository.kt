package com.neval.anoba.photo.photocomment

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PhotoCommentRepository(private val firestore: FirebaseFirestore) {

    private fun getCommentsCollection(photoId: String) =
        firestore.collection("photos").document(photoId).collection("comments")

    fun getCommentsStream(photoId: String): Flow<List<PhotoComment>> {
        return getCommentsCollection(photoId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addComment(photoId: String, comment: PhotoComment) {
        getCommentsCollection(photoId).add(comment).await()
        firestore.collection("photos").document(photoId).update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(photoId: String, commentId: String) {
        getCommentsCollection(photoId).document(commentId).delete().await()
        firestore.collection("photos").document(photoId).update("commentsCount", FieldValue.increment(-1)).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun toggleCommentLike(photoId: String, commentId: String, userId: String) {
        val commentRef = getCommentsCollection(photoId).document(commentId)
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
