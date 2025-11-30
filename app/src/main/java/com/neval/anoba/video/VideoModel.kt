package com.neval.anoba.video

import java.util.Date

data class VideoModel(
    val id: String = "",
    val videoUrl: String = "",
    val ownerId: String = "",
    val username: String = "",
    val timestamp: Date? = null,
    val reactions: Map<String, Long> = emptyMap(),
    val userReactions: Map<String, String> = emptyMap(),
    val commentsCount: Long = 0,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long? = null,
    val viewCount: Long = 0
)
