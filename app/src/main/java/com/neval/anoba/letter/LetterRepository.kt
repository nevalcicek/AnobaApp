package com.neval.anoba.letter

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class LetterRepository(
    firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "LetterRepository"
    }

    private val lettersCollection = firestore.collection("letters")

    // Tüm mektupları çek
    fun getLetters(): Flow<List<LetterModel>> {
        return lettersCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .dataObjects()
    }

    // Belirli mektubu ID ile çek
    fun getLetterById(letterId: String): Flow<LetterModel?> {
        return lettersCollection.document(letterId).dataObjects()
    }

    // Yeni mektup gönder
    suspend fun sendLetter(letter: LetterModel): Boolean {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
            val newDocRef = lettersCollection.document()
            letter.id = newDocRef.id
            letter.ownerId = uid // 🔐 rules için şart
            letter.viewCount = 0 // 🔢 varsa başlat
            letter.timestamp = Timestamp.now()
            newDocRef.set(letter).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending letter", e)
            false
        }
    }
    
    // Mektubu sil
    suspend fun deleteLetter(letterId: String): Boolean {
        return try {
            lettersCollection.document(letterId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting letter: $letterId", e)
            false
        }
    }

    // Görüntülenme sayısını artır
    suspend fun incrementLetterViewCount(letterId: String) {
        try {
            val letterRef = lettersCollection.document(letterId)
            letterRef.update("viewCount", FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing view count for letter: $letterId", e)
        }
    }
}