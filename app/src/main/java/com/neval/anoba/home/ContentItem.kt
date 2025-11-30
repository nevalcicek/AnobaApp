package com.neval.anoba.home

import com.neval.anoba.letter.LetterModel
import com.neval.anoba.photo.PhotoModel
import com.neval.anoba.ses.SesModel
import com.neval.anoba.video.VideoModel

sealed class ContentItem {
    abstract val id: String
    abstract val timestamp: Long?

    data class PhotoContent(val photo: PhotoModel) : ContentItem() {
        override val id: String get() = photo.id
        override val timestamp: Long? get() = photo.timestamp?.time
    }

    data class LetterContent(val letter: LetterModel) : ContentItem() {
        override val id: String get() = letter.id
        override val timestamp: Long? get() = letter.timestamp?.toDate()?.time
    }

    data class AudioContent(val audio: SesModel) : ContentItem() {
        override val id: String get() = audio.id
        override val timestamp: Long? get() = audio.timestamp?.time
    }

    data class VideoContent(val video: VideoModel) : ContentItem() {
        override val id: String get() = video.id
        override val timestamp: Long? get() = video.timestamp?.time
    }
}
