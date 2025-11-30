package com.neval.anoba.ses.sesemoji

import com.google.firebase.firestore.DocumentId

/**
 * Bir kullanıcının belirli bir sese verdiği reaksiyonu temsil eder.
 * 'ses' modülüne özeldir.
 */
data class SesReaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val sesId: String = "",
    val emoji: String = ""
)
