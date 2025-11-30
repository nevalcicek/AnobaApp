package com.neval.anoba.ses

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class SesRepository(
    firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val sesCollection = firestore.collection("ses")

    fun getSesler(): Flow<List<SesModel>> {
        return sesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .dataObjects()
    }

    fun getSes(sesId: String): Flow<SesModel?> {
        return sesCollection.document(sesId).dataObjects()
    }

    suspend fun incrementViewCount(sesId: String) {
        try {
            sesCollection.document(sesId).update("viewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e("SesRepository", "Error incrementing view count for ses $sesId", e)
        }
    }

    suspend fun uploadAudio(userId: String, username: String, file: File, duration: Long): String? {
        return try {
            val sesId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("ses/$sesId")
            val audioUri = Uri.fromFile(file)

            storageRef.putFile(audioUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val ses = SesModel(
                id = sesId,
                ownerId = userId,
                username = username,
                senderName = username,
                audioUrl = downloadUrl,
                duration = duration,
                timestamp = Timestamp.now().toDate()
            )
            sesCollection.document(sesId).set(ses).await()
            sesId
        } catch (e: Exception) {
            Log.e("SesRepository", "Error uploading audio", e)
            null
        }
    }

    suspend fun deleteSes(sesId: String): Boolean {
        return try {
            val sesDoc = sesCollection.document(sesId).get().await()
            val audioUrl = sesDoc.getString("audioUrl")

            sesCollection.document(sesId).delete().await()

            if (!audioUrl.isNullOrEmpty()) {
                storage.getReferenceFromUrl(audioUrl).delete().await()
            }
            true
        } catch (e: Exception) {
            Log.e("SesRepository", "Error deleting ses: $sesId", e)
            false
        }
    }
}
