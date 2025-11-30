package com.neval.anoba.photo.photoemoji

import androidx.compose.ui.graphics.Color

/**
 * Sadece 'photo' paketi içinde kullanılacak olan standart reaksiyon emoji setini ve renklerini barındırır.
 * Bu nesne, fotoğraflara verilebilecek tepkileri ve bu tepkilerin renklerini tanımlar.
 */
object PhotoEmojis {
    val list = listOf(
        "❤️", // Sevgi / Çok Beğenme
        "😂", // Kahkaha / Çok Komik
        "👍", // Onay / Katılıyorum
        "😢", // Üzüntü / Duygusal
        "😲", // Vay Canına! / Şaşkınlık
        "🤔", // Düşündürücü
        "😠",  // Kızgınlık / Katılmıyorum
        "👮"  // Polis / Rapor Et
    )
    val emojiColors = mapOf(
        "❤️" to Color(0xFFFDECEC),
        "😂" to Color(0xFFFFF9E6),
        "👍" to Color(0xFFE8F5E9),
        "😢" to Color(0xFFE3F2FD),
        "😲" to Color(0xFFF3E5F5),
        "🤔" to Color(0xFFECEFF1),
        "😠" to Color(0xFFFFEBEE),
        "👮" to Color(0xFFE0E0E0)
    )
}
