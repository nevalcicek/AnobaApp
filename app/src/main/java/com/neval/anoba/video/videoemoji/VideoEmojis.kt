package com.neval.anoba.video.videoemoji

import androidx.compose.ui.graphics.Color

object VideoEmojis {
    val list = listOf(
        "❤️", // Sevgi / Çok Beğenme
        "😂", // Kahkaha / Çok Komik
        "👍", // Onay / Katılıyorum
        "😢", // Üzüntü / Duygusal
        "😲", // Vay Canına! / Şaşkınlık
        "🤔", // Düşündürücü
        "😠"  // Kızgınlık / Katılmıyorum

    )
    val emojiColors = mapOf(
        "❤️" to Color(0xFFFDECEC),
        "😂" to Color(0xFFFFF9E6),
        "👍" to Color(0xFFE8F5E9),
        "😢" to Color(0xFFE3F2FD),
        "😲" to Color(0xFFF3E5F5),
        "🤔" to Color(0xFFECEFF1),
        "😠" to Color(0xFFFFEBEE)
    )
}
