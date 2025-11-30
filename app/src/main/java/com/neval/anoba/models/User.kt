package com.neval.anoba.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Tüm alanların varsayılan bir değeri olduğu için, Firestore veriyi doğrudan değişmez 'val' alanlarına atayabilir.
data class User(
    @DocumentId
    @get:Exclude // Firestore'un bu alanı okumasını/yazmasını engeller.
    val documentId: String = "", // Bu, dokümanın kendi ID'sini alır.

    val uid: String = "", // Bu, Firestore dokümanı içindeki 'uid' alanına karşılık gelir.
    val userId: String = "", // Eski kodlarla uyumluluk için.

    val displayName: String = "",
    val username: String? = null,
    val email: String = "",
    val photoURL: String? = null,
    val bio: String? = null,
    val role: String = "MEMBER",

    @ServerTimestamp
    val createdAt: Date? = null,

    val userNumber: Int? = null
)
