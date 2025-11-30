package com.neval.anoba.video.videocomment

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class VideoComment(
    @DocumentId
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String? = null,
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val likedBy: List<String> = emptyList(),
    val replyToId: String? = null
)
