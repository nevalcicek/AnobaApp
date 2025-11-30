package com.neval.anoba.letter.letteremoji

import com.google.firebase.firestore.DocumentId

/**
 * Bir kullanıcının belirli bir mektuba verdiği reaksiyonu temsil eder.
 * 'letter' modülüne özeldir.
 */
data class LetterReaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val letterId: String = "",
    val emoji: String = ""
)
