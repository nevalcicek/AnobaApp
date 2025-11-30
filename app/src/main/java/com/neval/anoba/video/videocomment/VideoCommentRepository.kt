package com.neval.anoba.video.videocomment

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class VideoCommentRepository(private val firestore: FirebaseFirestore) {

    private fun getCommentsCollection(videoId: String) =
        firestore.collection("videos").document(videoId).collection("comments")

    fun getCommentsStream(videoId: String): Flow<List<VideoComment>> {
        return getCommentsCollection(videoId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addComment(videoId: String, comment: VideoComment) {
        getCommentsCollection(videoId).add(comment).await()
        firestore.collection("videos").document(videoId).update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(videoId: String, commentId: String) {
        getCommentsCollection(videoId).document(commentId).delete().await()
        firestore.collection("videos").document(videoId).update("commentsCount", FieldValue.increment(-1)).await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun toggleCommentLike(videoId: String, commentId: String, userId: String) {
        val commentRef = getCommentsCollection(videoId).document(commentId)
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
