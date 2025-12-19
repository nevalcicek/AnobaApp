package com.neval.anoba.letter

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class LetterModel(
    @DocumentId var id: String = "",
    var ownerId: String = "",
    var username: String = "",
    var content: String = "",
    var commentsCount: Long = 0,
    var timestamp: Timestamp? = null,
    var reactions: Map<String, Long> = mapOf(),
    var userReactions: Map<String, String> = mapOf(),
    var viewCount: Long = 0,

    // YENİ: Mühürlü Mektup Özelliği İçin Alanlar
    var privacy: String = "PUBLIC", // Mektup gizliliği: PUBLIC, SEALED
    var recipientId: String? = null, // Alıcı ID'si (mühürlü ise)
    var recipientUsername: String? = null // Alıcı kullanıcı adı (mühürlü ise)
) : Parcelable