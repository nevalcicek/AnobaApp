package com.neval.anoba.chat.group

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

interface GroupMessageable {
    val id: String
    val senderId: String?
    val content: String?
    val timestampMillis: Long?
}

data class GroupMessage(
    @DocumentId
    override val id: String = "",
    override val senderId: String? = null,
    val senderName: String? = null,
    override val content: String? = null,
    val groupId: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
) : GroupMessageable {
    override val timestampMillis: Long?
        get() = timestamp?.time
}