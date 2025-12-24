package com.neval.anoba.chat.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.neval.anoba.common.utils.Constants

@Composable
fun GroupItem(
    group: ChatGroup,
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel
) {
    val groupId = group.id.ifBlank { "defaultGroup" }
    val groupName = group.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                groupChatViewModel.setActiveGroupId(groupId)
                navController.navigate("${Constants.GROUP_CHAT_SCREEN}/$groupId")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Grup Resmi veya Baş Harf Avatarı
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!group.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = group.imageUrl,
                        contentDescription = "Grup Resmi: $groupName",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    GroupInitialsAvatar(
                        name = group.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = groupName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (group.isPrivate) "Özel Grup" else "Genel Grup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = {
                navController.navigate("${Constants.GROUP_INFO_SCREEN}/$groupId")
            }) {
                Icon(Icons.Filled.Info, contentDescription = "Grup Bilgisi")
            }
        }
    }
}
