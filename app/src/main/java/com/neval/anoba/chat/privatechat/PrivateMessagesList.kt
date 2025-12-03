package com.neval.anoba.chat.privatechat

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
fun PrivateMessageRowItem(
    message: PrivateMessage,
    currentUserId: String?,
    isSelected: Boolean,
    onMessageClick: (PrivateMessage) -> Unit,
    onMessageLongClick: (PrivateMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSender = message.senderId == currentUserId
    val formattedTime = remember(message.timestampMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestampMillis))
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
                        text = message.content,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivateMessagesList(
    modifier: Modifier = Modifier,
    messages: List<PrivateMessage>,
    listState: LazyListState,
    currentUserId: String?,
    selectedMessages: Set<PrivateMessage>,
    onMessageClick: (PrivateMessage) -> Unit,
    onMessageLongClick: (PrivateMessage) -> Unit,
    bringIntoViewRequester: BringIntoViewRequester
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    val todayDateString = dateFormat.format(Date())

    val sortedMessageGroups = messages
        .groupBy { message -> dateFormat.format(Date(message.timestampMillis)) }
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
                PrivateDateHeader(displayDate)
            }

            items(
                items = messagesInDate.sortedBy { it.timestampMillis },
                key = { message -> message.id.ifBlank { UUID.randomUUID().toString() } }
            ) { message ->
                PrivateMessageRowItem(
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
fun PrivateDateHeader(text: String) {
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
