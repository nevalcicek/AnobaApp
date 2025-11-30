package com.neval.anoba.photo.photoemoji

import com.google.firebase.firestore.DocumentId

/**
 * Bir kullanıcının belirli bir fotoğrafa verdiği reaksiyonu temsil eder.
 * 'photo' modülüne özeldir.
 */
data class PhotoReaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val photoId: String = "",
    val emoji: String = ""
)
