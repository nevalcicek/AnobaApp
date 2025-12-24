package com.neval.anoba.chat.general

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.neval.anoba.chat.ui.GROUP_CHAT_FLOW_ROUTE
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatHomeScreen(
    navController: NavHostController,
    chatViewModel: GeneralChatViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val selectedMessages by chatViewModel.selectedMessages.collectAsStateWithLifecycle()
    val isSelectionModeActive = selectedMessages.isNotEmpty()

    var messageToEdit by remember { mutableStateOf<GeneralChatMessage?>(null) }
    val isEditMode by remember { derivedStateOf { messageToEdit != null } }

    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid
    val chatRoomId = "GeneralChat"

    val isAdmin by authViewModel.isAdmin.collectAsStateWithLifecycle()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(snackbarHostState) {
        chatViewModel.undoState.collectLatest { lastDeletedMessages ->
            if (lastDeletedMessages.isNotEmpty()) {
                val result = snackbarHostState.showSnackbar(
                    message = "${lastDeletedMessages.size} mesaj silindi",
                    actionLabel = "Geri Al",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    chatViewModel.undoDelete()
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            chatViewModel.loadChatMessages(chatRoomId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSelectionModeActive) {
                SelectionModeTopAppBar(
                    selectedCount = selectedMessages.size,
                    onCloseSelectionMode = { chatViewModel.clearMessageSelection() },
                    canDeleteAnySelected = selectedMessages.any { it.senderId == currentUserId || isAdmin },
                    onDeleteSelected = {
                        chatViewModel.deleteMessages(chatRoomId, selectedMessages.toList())
                        chatViewModel.clearMessageSelection()
                    },
                    canCopyAnySelected = true,
                    onCopySelected = { chatViewModel.copySelectedMessagesToClipboard(context) },
                    canEdit = selectedMessages.size == 1 && selectedMessages.first().senderId == currentUserId,
                    onEditSelected = {
                        val message = selectedMessages.first()
                        messageToEdit = message
                        inputMessage = message.content
                        chatViewModel.clearMessageSelection()
                        focusRequester.requestFocus()
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(if (isEditMode) "Mesajı Düzenle" else "Genel Sohbet") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isEditMode) {
                                messageToEdit = null
                                inputMessage = ""
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        if (isAdmin && !isEditMode) {
                            IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Tüm mesajları sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (!isEditMode) {
                            IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                                Icon(Icons.Filled.Home, contentDescription = "Ana Sayfa")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (showDeleteConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = { Text("Tüm Mesajları Sil") },
                text = { Text("Bu odadaki tüm mesajlar kalıcı olarak silinecektir. Bu işlem geri alınamaz. Emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            chatViewModel.deleteAllMessagesForAdmin(chatRoomId)
                            showDeleteConfirmationDialog = false
                        }
                    ) {
                        Text("Sil")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            ChatScreenHeader(navController = navController)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        messages,
                        key = { _, message -> message.id.ifBlank { message.hashCode() } }) { index, message ->
                        val previousMillis = messages.getOrNull(index - 1)?.timestampMillis
                        val currentMillis = message.timestampMillis
                        val isFirstMessageOfDay =
                            index == 0 || !isSameDay(previousMillis, currentMillis)

                        if (isFirstMessageOfDay) {
                            GeneralDateHeader(dateText = getFormattedDate(currentMillis))

                        }

                        ChatMessageItem(
                            message = message,
                            isSentByCurrentUser = message.senderId == currentUserId,
                            isSelected = selectedMessages.contains(message),
                            onLongPress = {
                                if (!isEditMode) chatViewModel.onMessageLongClicked(it)
                            },
                            onClick = {
                                if (isSelectionModeActive) {
                                    chatViewModel.onMessageClicked(it)
                                }
                            }
                        )
                    }
                }
            }

            if (currentUserId != null) {
                ChatInput(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    onSendClick = {
                        if (inputMessage.isNotBlank()) {
                            if (isEditMode) {
                                messageToEdit?.let {
                                    chatViewModel.editMessage(
                                        chatRoomId,
                                        it.id,
                                        inputMessage.trim()
                                    )
                                }
                                messageToEdit = null
                            } else {
                                chatViewModel.addMessage(
                                    chatRoomId,
                                    GeneralChatMessage(
                                        senderId = currentUserId,
                                        sender = currentUser.displayName ?: "Anonim Kullanıcı",
                                        receiverId = chatRoomId,
                                        receiverName = "Genel Sohbet",
                                        content = inputMessage.trim()
                                    )
                                )
                            }
                            inputMessage = ""
                        }
                    },
                    bringIntoViewRequester = bringIntoViewRequester,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatScreenHeader(navController: NavHostController) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatCard(
                title = "Grup Sohbeti",
                description = "Gruplarınıza katılın",
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { navController.navigate(GROUP_CHAT_FLOW_ROUTE) },
                modifier = Modifier.weight(1f).height(100.dp)
            )
            ChatCard(
                title = "Özel Sohbet",
                description = "Birebir konuşmalar",
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        navController.navigate(Constants.PRIVATE_CHAT_SCREEN)
                    } else {
                        navController.navigate(Constants.LOGIN_SCREEN)
                    }
                },
                modifier = Modifier.weight(1f).height(100.dp)
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun GeneralDateHeader(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: GeneralChatMessage,
    isSentByCurrentUser: Boolean,
    isSelected: Boolean,
    onLongPress: (GeneralChatMessage) -> Unit,
    onClick: (GeneralChatMessage) -> Unit
) {
    val horizontalArrangement = if (isSentByCurrentUser) Arrangement.End else Arrangement.Start
    val bubbleColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isSentByCurrentUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick(message) },
                onLongClick = { onLongPress(message) }
            ),
        horizontalArrangement = horizontalArrangement
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.8f)) {
            Column(
                modifier = Modifier
                    .align(if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSentByCurrentUser) 16.dp else 0.dp,
                            bottomEnd = if (isSentByCurrentUser) 0.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (!isSentByCurrentUser) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestampMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        keyboardController?.show()
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            placeholder = { Text("Mesajınızı yazın...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendClick() }),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            singleLine = true
        )
        IconButton(
            onClick = onSendClick,
            enabled = value.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gönder",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ChatCard(
    title: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
private fun isSameDay(millis1: Long?, millis2: Long?): Boolean {
    if (millis1 == null || millis2 == null) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
private fun getFormattedDate(millis: Long?): String {
    if (millis == null) return ""
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        isSameDay(millis, today.timeInMillis) -> "Bugün"
        isSameDay(millis, yesterday.timeInMillis) -> "Dün"
        else -> SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr")).format(Date(millis))
    }
}
