package com.neval.anoba.chat.group

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessageRowItem(
    message: GroupMessage,
    currentUserId: String?,
    isSelected: Boolean,
    onMessageClick: (GroupMessage) -> Unit,
    onMessageLongClick: (GroupMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSender = message.senderId == currentUserId
    val formattedTime = remember(message.timestampMillis) {
        message.timestampMillis?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "--:--"
    }

    val bubbleColor = if (isSender) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = if (isSender) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val rowModifier = modifier
        .fillMaxWidth()
        .background(
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else Color.Transparent
        )
        .combinedClickable(
            onClick = { onMessageClick(message) },
            onLongClick = { onMessageLongClick(message) }
        )
        .padding(vertical = 4.dp, horizontal = 8.dp)

    Row(
        modifier = rowModifier,
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = if (isSender) Alignment.End else Alignment.Start) {
            if (!isSender) {
                message.senderName?.takeIf { it.isNotBlank() }?.let { senderName ->
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isSender) 16.dp else 4.dp,
                    topEnd = if (isSender) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = bubbleColor,
                tonalElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.content ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        maxLines = 10
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupMessagesList(
    modifier: Modifier = Modifier,
    messages: List<GroupMessage>,
    listState: LazyListState,
    currentUserId: String?,
    selectedMessages: Set<GroupMessage>,
    onMessageClick: (GroupMessage) -> Unit,
    onMessageLongClick: (GroupMessage) -> Unit,
    bringIntoViewRequester: BringIntoViewRequester
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    val todayDateString = dateFormat.format(Date())

    val sortedMessageGroups = messages
        .filter { it.timestampMillis != null }
        .groupBy { message -> dateFormat.format(Date(message.timestampMillis ?: 0L)) }
        .entries
        .sortedBy { (dateString, _) ->
            try {
                dateFormat.parse(dateString)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .bringIntoViewRequester(bringIntoViewRequester),
        state = listState,
        reverseLayout = false,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    ) {
        sortedMessageGroups.forEach { (date, messagesInDate) ->
            item(key = "header_$date") {
                val displayDate = if (date == todayDateString) "Bugün" else date
                GroupDateHeader(displayDate)
            }

            items(
                items = messagesInDate.sortedBy { it.timestampMillis ?: 0L },
                key = { message -> message.id.ifBlank { UUID.randomUUID().toString() } }
            ) { message ->
                GroupMessageRowItem(
                    message = message,
                    currentUserId = currentUserId,
                    isSelected = selectedMessages.contains(message),
                    onMessageClick = onMessageClick,
                    onMessageLongClick = onMessageLongClick,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun GroupDateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            tonalElevation = 0.5.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}