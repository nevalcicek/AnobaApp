package com.neval.anoba.chat.general

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GeneralChatRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("chatRooms")

    fun getMessagesStream(roomId: String): Flow<List<GeneralChatMessage>> {
        return collection.document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addMessage(roomId: String, message: GeneralChatMessage) {
        collection.document(roomId)
            .collection("messages")
            .add(message).await()
    }

    suspend fun deleteMessage(roomId: String, messageId: String) {
        collection.document(roomId)
            .collection("messages").document(messageId)
            .delete().await()
    }

    suspend fun editMessage(roomId: String, messageId: String, newContent: String) {
        collection.document(roomId)
            .collection("messages").document(messageId)
            .update("content", newContent)
            .await()
    }

    suspend fun addMessages(roomId: String, messages: List<GeneralChatMessage>) {
        val batch = firestore.batch()
        val messagesCollection = collection.document(roomId).collection("messages")
        messages.forEach { message ->
            val docRef = messagesCollection.document(message.id)
            batch.set(docRef, message)
        }
        batch.commit().await()
    }

    // Admin yetkisiyle tüm mesajları silmek için yeni fonksiyon
    suspend fun deleteAllMessages(roomId: String) {
        val messagesCollection = collection.document(roomId).collection("messages")
        val snapshot = messagesCollection.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }
        batch.commit().await()
    }
}
