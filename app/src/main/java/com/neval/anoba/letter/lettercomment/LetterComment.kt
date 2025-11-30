package com.neval.anoba.letter.lettercomment

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Sadece bir mektuba ait bir yorumu temsil eden veri sınıfı.
 */
data class LetterComment(
    @DocumentId
    val id: String = "",
    val letterId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileUrl: String? = null,
    val content: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val likedBy: List<String> = emptyList(),
    val replyToId: String? = null
)
