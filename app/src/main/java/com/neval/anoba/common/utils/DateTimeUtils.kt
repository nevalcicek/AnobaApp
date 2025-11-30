package com.neval.anoba.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatRelativeTime(dateMillis: Long?): String {
        if (dateMillis == null) return "Bilinmiyor"

        val now = Date().time

        val diff = now - dateMillis

        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 1 -> "az önce"
            minutes < 60 -> "$minutes dakika önce"
            hours < 24 -> "$hours saat önce"
            days < 7 -> "$days gün önce"
            else -> {
                val actualDate = Date(dateMillis)
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(actualDate)
            }
        }
    }
}