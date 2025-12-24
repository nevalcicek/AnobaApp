/*
package com.neval.anoba.video

import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Grayscale
import androidx.media3.effect.Sepia

@Immutable
data class VideoFilter @OptIn(UnstableApi::class) constructor(
    val name: String,
    val effect: Effect
)

@OptIn(UnstableApi::class)
val videoFilters = listOf(
    VideoFilter("Yok", object : Effect {}),
    VideoFilter("Siyah & Beyaz", Grayscale.create()),
    VideoFilter("Sepya", Sepia.create())
    // Gelecekte daha fazla filtre buraya eklenebilir
)
*/
