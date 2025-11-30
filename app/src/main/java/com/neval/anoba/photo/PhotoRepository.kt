package com.neval.anoba.photo

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PhotoRepository(
    firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val photoCollection = firestore.collection("photos")

    fun getPhotos(): Flow<List<PhotoModel>> {
        return photoCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .dataObjects()
    }

    fun getPhoto(photoId: String): Flow<PhotoModel?> {
        return photoCollection.document(photoId).dataObjects()
    }

    suspend fun incrementViewCount(photoId: String) {
        try {
            photoCollection.document(photoId).update("viewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error incrementing view count for photo $photoId", e)
        }
    }

    suspend fun uploadPhoto(userId: String, username: String, photoUri: Uri, title: String): String? {
        return try {
            val photoId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("photos/$photoId")
            val uploadTask = storageRef.putFile(photoUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            val photo = PhotoModel(
                id = photoId,
                ownerId = userId,
                username = username,
                photoUrl = downloadUrl,
                title = title
            )
            photoCollection.document(photoId).set(photo).await()
            photoId
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error uploading photo", e)
            null
        }
    }

    suspend fun deletePhoto(photoId: String): Boolean {
        return try {
            val photoDoc = photoCollection.document(photoId).get().await()
            val photoUrl = photoDoc.getString("photoUrl")

            // Önce Firestore belgesini sil
            photoCollection.document(photoId).delete().await()

            // Sonra Storage'dan dosyayı sil
            if (!photoUrl.isNullOrEmpty()) {
                 storage.getReferenceFromUrl(photoUrl).delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error deleting photo: $photoId", e)
            false
        }
    }
}
