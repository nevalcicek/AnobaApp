package com.neval.anoba.chat.general

import com.neval.anoba.chat.group.GroupChatUserRole

data class GeneralChatUser(
    val id: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val role: String = GroupChatUserRole.GUEST.name
) {
    val safeDisplayName: String
        get() = displayName.takeIf { it.isNotBlank() } ?: "Kullanıcı"
}