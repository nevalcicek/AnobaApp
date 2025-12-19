package com.neval.anoba.ses

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class SesModel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val senderName: String = "",
    val username: String = "",
    val audioUrl: String = "",
    val reactions: Map<String, Long> = emptyMap(),
    val commentCount: Long = 0L,
    val duration: Long = 0L,
    val viewCount: Long = 0L,
    @ServerTimestamp val timestamp: Date? = null
) : Parcelable