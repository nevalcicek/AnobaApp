package com.neval.anoba.chat.group

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatGroup(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var ownerId: String = "",
    @get:PropertyName("isPrivate")
    var isPrivate: Boolean = false,
    var members: List<String> = emptyList(),
    var imageUrl: String? = null,
    val typingUsers: Map<String, String> = emptyMap(),
    @ServerTimestamp
    var createdAt: Date? = null
) {
    @Suppress("unused")
    constructor() : this("", "", "", "", false, emptyList(), null, emptyMap(), null)
}
