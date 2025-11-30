package com.neval.anoba.chat.general

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class GeneralChatMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val sender: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val status: String = "SENT"
) {
    @get:Exclude
    val optimisticTimestamp: Long = System.currentTimeMillis()

    val timestampMillis: Long
        get() = timestamp?.time ?: optimisticTimestamp
}
