package com.neval.anoba.chat.group

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.neval.anoba.chat.general.GeneralChatUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
class GroupChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val tag = "GroupChatRepository"
    fun getGroupsStreamForUser(userId: String): Flow<List<ChatGroup>> {
        return db.collection("groups")
            .whereArrayContains("members", userId)
            .dataObjects()
    }

    fun getMessagesStreamForGroup(groupId: String): Flow<List<GroupMessage>> {
        return db.collection("groups").document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .dataObjects()
    }

    suspend fun createGroup(group: ChatGroup): ChatGroup {
        val docRef = db.collection("groups").document()
        group.id = docRef.id
        docRef.set(group).await()
        return group
    }

    suspend fun addMessageToGroup(groupId: String, message: GroupMessage) {
        db.collection("groups").document(groupId)
            .collection("messages").add(message).await()
    }

    suspend fun deleteMessageFromGroup(groupId: String, messageId: String) {
        db.collection("groups").document(groupId)
            .collection("messages").document(messageId).delete().await()
    }

    suspend fun updateGroupName(groupId: String, newName: String) {
        db.collection("groups").document(groupId).update("name", newName).await()
    }

    suspend fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete().await()
    }

    suspend fun getGroupDetails(groupId: String): ChatGroup? {
        return try {
            val document = db.collection("groups").document(groupId).get().await()
            document.toObject(ChatGroup::class.java)
        } catch (e: Exception) {
            Log.e(tag, "Error getting group details for $groupId", e)
            null
        }
    }

    suspend fun getGroupMemberDetails(memberIds: List<String>): List<GeneralChatUser> {
        if (memberIds.isEmpty()) return emptyList()
        val users = mutableListOf<GeneralChatUser>()
        try {
            memberIds.chunked(10).forEach { chunk ->
                val snapshot = db.collection("users").whereIn("uid", chunk).get().await()
                users.addAll(snapshot.toObjects(GeneralChatUser::class.java))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user details for members", e)
        }
        return users
    }

    suspend fun addUserToGroup(groupId: String, userId: String) {
        db.collection("groups").document(groupId).update("members", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun removeUserFromGroup(groupId: String, userId: String) {
        db.collection("groups").document(groupId).update("members", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun setGroupPrivacy(groupId: String, isPrivate: Boolean) {
        db.collection("groups").document(groupId).update("isPrivate", isPrivate).await()
    }

    suspend fun updateGroupInfo(groupId: String, newName: String, newDescription: String) {
        val updates = mapOf(
            "name" to newName,
            "description" to newDescription
        )
        db.collection("groups").document(groupId).update(updates).await()
    }

    @Suppress("UNUSED_PARAMETER")
    fun muteGroup(groupId: String, userId: String) { /* Implementasyon gerekli */ }

    @Suppress("UNUSED_PARAMETER")
    fun unmuteGroup(groupId: String, userId: String) { /* Implementasyon gerekli */ }
}
