package com.neval.anoba.ses.sescomment

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SesComment(
    @DocumentId
    val id: String = "",
    val sesId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String? = null,
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val likedBy: List<String> = emptyList(),
    val replyToId: String? = null
)
