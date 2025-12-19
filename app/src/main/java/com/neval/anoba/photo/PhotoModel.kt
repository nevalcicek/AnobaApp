package com.neval.anoba.photo

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class PhotoModel(
    @DocumentId var id: String = "",
    var photoUrl: String = "",
    var ownerId: String = "",
    var username: String = "",
    var title: String = "",

    @ServerTimestamp
    var timestamp: Date? = null,

    var reactions: Map<String, Long> = emptyMap(),
    var userReactions: Map<String, String> = emptyMap(),
    var commentsCount: Long = 0,
    var viewCount: Long = 0
) : Parcelable