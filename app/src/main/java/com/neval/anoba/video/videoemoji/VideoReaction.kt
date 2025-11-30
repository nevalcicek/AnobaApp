package com.neval.anoba.video.videoemoji

import com.google.firebase.firestore.DocumentId

data class VideoReaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val videoId: String = "",
    val emoji: String = ""
)
