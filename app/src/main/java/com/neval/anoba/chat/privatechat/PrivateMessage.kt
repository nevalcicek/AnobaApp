package com.neval.anoba.chat.privatechat

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PrivateMessage(
    @DocumentId
    override val id: String = "",
    override val senderId: String = "",

    @get:PropertyName("sender")
    @set:PropertyName("sender")
    override var senderName: String = "",

    override val content: String = "",
    override val timestampMillis: Long = 0L,

    val receiverId: String = "",
    val receiverName: String = "",
    val status: String = "SENT"
) : Parcelable, PrivateMessageable