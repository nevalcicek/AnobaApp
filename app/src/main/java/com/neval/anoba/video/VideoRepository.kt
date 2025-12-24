package com.neval.anoba.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class VideoRepository(
    firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val videoCollection = firestore.collection("videos")

    fun getVideos(): Flow<List<VideoModel>> {
        return videoCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .dataObjects()
    }

    fun getVideo(videoId: String): Flow<VideoModel?> {
        return videoCollection.document(videoId).dataObjects()
    }

    suspend fun incrementViewCount(videoId: String) {
        try {
            videoCollection.document(videoId).update("viewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error incrementing view count for video $videoId", e)
        }
    }

    suspend fun uploadVideo(
        userId: String,
        username: String,
        videoUri: Uri,
        title: String,
        duration: Long
    ): String? {
        return try {
            val videoId = UUID.randomUUID().toString()
            val videoStorageRef = storage.reference.child("videos/$videoId")
            val thumbnailUrl: String?
            val uploadTask = videoStorageRef.putFile(videoUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            thumbnailUrl = downloadUrl

            val video = VideoModel(
                id = videoId,
                ownerId = userId,
                username = username,
                videoUrl = downloadUrl,
                thumbnailUrl = thumbnailUrl,
                title = title,
                duration = duration,
                timestamp = com.google.firebase.Timestamp.now().toDate()
            )
            videoCollection.document(videoId).set(video).await()
            videoId
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error uploading video", e)
            null
        }
    }

    suspend fun deleteVideo(videoId: String): Boolean {
        return try {
            val videoDoc = videoCollection.document(videoId).get().await()
            val videoUrl = videoDoc.getString("videoUrl")

            // Önce Firestore belgesini sil
            videoCollection.document(videoId).delete().await()

            // Sonra Storage'dan dosyayı sil
            if (!videoUrl.isNullOrEmpty()) {
                storage.getReferenceFromUrl(videoUrl).delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error deleting video: $videoId", e)
            false
        }
    }

    suspend fun getVideoUriForSharing(context: Context, videoUrl: String, videoId: String): Uri {
        val storageRef = storage.getReferenceFromUrl(videoUrl)
        val localFile = File(context.cacheDir, "$videoId.mp4")

        storageRef.getFile(localFile).await()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            localFile
        )
    }
}
