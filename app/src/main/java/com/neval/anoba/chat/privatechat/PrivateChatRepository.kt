package com.neval.anoba.chat.privatechat

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PrivateChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val tag = "PrivateChatRepository"

    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    fun getMessagesStream(currentUserId: String, otherUserId: String): Flow<List<PrivateMessage>> {
        val chatRoomId = getChatRoomId(currentUserId, otherUserId)
        return db.collection("private_chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestampMillis")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(PrivateMessage::class.java) }
    }

    suspend fun sendPrivateMessage(message: PrivateMessage) {
        val senderId = message.senderId
        val receiverId = message.receiverId
        val chatRoomId = getChatRoomId(senderId, receiverId)

        val chatRoomRef = db.collection("private_chats").document(chatRoomId)
        val messageRef = chatRoomRef.collection("messages").document()

        try {
            db.runTransaction { transaction ->
                val roomSnapshot = transaction.get(chatRoomRef)

                if (!roomSnapshot.exists()) {
                    Log.d(tag, "Chat room $chatRoomId does not exist. Creating new room.")
                    val participantsList = listOf(senderId, receiverId)

                    val newRoomData = mapOf(
                        "participants" to participantsList,
                        "lastMessage" to message.content,
                        "lastActivity" to FieldValue.serverTimestamp(),
                    )
                    transaction.set(chatRoomRef, newRoomData)
                } else {
                    Log.d(tag, "Chat room $chatRoomId exists. Updating room.")
                    val roomUpdates = mapOf(
                        "lastMessage" to message.content,
                        "lastActivity" to FieldValue.serverTimestamp()
                    )
                    transaction.update(chatRoomRef, roomUpdates)
                }

                transaction.set(messageRef, message)
                null
            }.await()
            Log.i(tag, "Transaction successful for message to $chatRoomId")
        } catch (e: Exception) {
            Log.e(tag, "Error in sendPrivateMessage transaction for $chatRoomId", e)
            throw e
        }
    }

    suspend fun deletePrivateMessage(currentUserId: String, partnerUserId: String, messageId: String) {
        val chatRoomId = getChatRoomId(currentUserId, partnerUserId)
        db.collection("private_chats")
            .document(chatRoomId)
            .collection("messages")
            .document(messageId)
            .delete()
            .await()
    }

    suspend fun updatePrivateMessageContent(
        senderId: String,
        receiverId: String,
        messageId: String,
        newContent: String
    ) {
        val chatRoomId = getChatRoomId(senderId, receiverId)
        val messageRef = db.collection("private_chats")
            .document(chatRoomId)
            .collection("messages")
            .document(messageId)
        messageRef.update("content", newContent).await()
    }

    suspend fun getAllUsers(): QuerySnapshot {
        return db.collection("users").get().await()
    }
}
