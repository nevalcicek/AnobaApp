package com.neval.anoba.livestream

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LiveStream(
    @DocumentId val streamId: String = "",
    val streamerId: String = "",
    val streamerName: String = "",
    val title: String = "",
    val description: String? = null,
    @ServerTimestamp val startTime: Date? = null,
    val isActive: Boolean = false
)
