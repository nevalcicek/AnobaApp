package com.neval.anoba.chat.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object GroupUtils {
    fun copyToClipboard(context: Context, text: String, label: String = "Copied Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "\"$label\" panoya kopyalandı.", Toast.LENGTH_SHORT).show()
    }
}
