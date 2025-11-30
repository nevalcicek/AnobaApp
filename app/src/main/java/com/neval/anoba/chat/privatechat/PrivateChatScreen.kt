package com.neval.anoba.chat.privatechat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.neval.anoba.chat.general.GeneralChatUser
import com.neval.anoba.common.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    navController: NavHostController,
    privateChatViewModel: PrivateChatViewModel,
) {
    LaunchedEffect(Unit) {
        privateChatViewModel.loadAllUsersExceptCurrent()
    }
    val uiState by privateChatViewModel.uiState.collectAsStateWithLifecycle()
    val selectedMessages by privateChatViewModel.selectedMessages.collectAsStateWithLifecycle()
    val isSelectionModeActive by privateChatViewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val currentUserId = privateChatViewModel.currentUserId
    val editingMessage by privateChatViewModel.editingMessage.collectAsStateWithLifecycle()
    val messageText by privateChatViewModel.messageText.collectAsStateWithLifecycle()
    val allUsers by privateChatViewModel.userList.collectAsStateWithLifecycle()
    val chatPartnerProfile by privateChatViewModel.chatPartnerProfile.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val context = LocalContext.current

    var selectedUserForChat by remember { mutableStateOf<GeneralChatUser?>(null) }
    var showUserSelectionDialog by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(allUsers, selectedUserForChat) {
        if (allUsers.isNotEmpty() && selectedUserForChat == null) {
            showUserSelectionDialog = true
        }
    }

    val sortedMessages by remember(uiState) {
        derivedStateOf {
            (uiState as? PrivateChatUiState.Success)?.messages
                ?.sortedBy { it.timestampMillis }
                ?: emptyList()
        }
    }

    val canEditMessage by remember(selectedMessages, currentUserId) {
        derivedStateOf {
            selectedMessages.size == 1 && selectedMessages.firstOrNull()?.senderId == currentUserId
        }
    }

    val editableMessage by remember(canEditMessage, selectedMessages) {
        derivedStateOf {
            if (canEditMessage) selectedMessages.firstOrNull() else null
        }
    }

    LaunchedEffect(selectedUserForChat?.id) {
        selectedUserForChat?.id?.let { privateChatViewModel.listenForMessages(it) }
    }

    LaunchedEffect(sortedMessages.size) {
        if (sortedMessages.isNotEmpty()) {
            coroutineScope.launch {
                delay(100) // Listenin çizilmesi için küçük bir gecikme
                listState.animateScrollToItem(sortedMessages.size - 1)
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            isSelectionModeActive -> {
                privateChatViewModel.clearMessageSelection()
            }
            selectedUserForChat != null -> {
                selectedUserForChat = null
                privateChatViewModel.resetChatState() // ViewModel'deki sohbeti sıfırla
            }
            else -> {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionModeActive) {
                PrivateSelectionModeTopAppBar(
                    selectedCount = selectedMessages.size,
                    onCloseSelectionMode = { privateChatViewModel.clearMessageSelection() },
                    canDeleteAnySelected = selectedMessages.any { it.senderId == currentUserId },
                    onDeleteSelected = {
                        selectedUserForChat?.id?.let { privateChatViewModel.deleteSelectedMessages(it) }
                    },
                    canCopyAnySelected = selectedMessages.isNotEmpty(),
                    onCopySelected = { privateChatViewModel.copySelectedMessagesToClipboard(context) },
                    canEdit = editableMessage != null,
                    onEditSelected = {
                        editableMessage?.let { privateChatViewModel.startEditingMessage(it) }
                        privateChatViewModel.clearMessageSelection() // Seçim modunu kapat
                    }
                )
            } else {
                TopAppBar(            title = {
                    Text(chatPartnerProfile?.safeDisplayName ?: selectedUserForChat?.safeDisplayName ?: "Özel Sohbet")
                },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                selectedUserForChat != null -> {
                                    selectedUserForChat = null
                                    privateChatViewModel.resetChatState()
                                }
                                else -> {
                                    navController.popBackStack()
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                            Icon(Icons.Filled.Home, contentDescription = "Ana Sayfa")
                        }
                    }
                )
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                Box(modifier = Modifier.weight(1f)) {
                    when (val currentState = uiState) {
                        is PrivateChatUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is PrivateChatUiState.Success -> {
                            if (selectedUserForChat == null && !isSelectionModeActive) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sohbete başlamak için bir kullanıcı seçin.")
                                }
                            } else if (currentState.messages.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${selectedUserForChat?.safeDisplayName ?: "Seçili kullanıcı"} ile henüz mesajınız yok.")
                                }
                            } else {
                                PrivateMessagesList(
                                    modifier = Modifier.fillMaxSize(),
                                    messages = sortedMessages,
                                    listState = listState,
                                    currentUserId = currentUserId,
                                    selectedMessages = selectedMessages,
                                    onMessageClick = { privateChatViewModel.onMessageClicked(it) },
                                    onMessageLongClick = {
                                        privateChatViewModel.onMessageLongClicked(
                                            it
                                        )
                                    },
                                    bringIntoViewRequester = bringIntoViewRequester
                                )
                            }
                        }

                        is PrivateChatUiState.Error -> {
                            Text(
                                "Hata: ${currentState.message}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                if (selectedUserForChat != null && !isSelectionModeActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        if (editingMessage != null) {
                            Text(
                                text = "Düzenleme modunda: ${editingMessage?.content.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { privateChatViewModel.cancelEditing() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Düzenlemeyi İptal Et")
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = messageText,
                                onValueChange = { privateChatViewModel.updateMessageText(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Mesajınızı yazın...") },
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val receiverId = selectedUserForChat!!.id
                                    val contentToSend = messageText
                                    if (contentToSend.isNotBlank()) {
                                        privateChatViewModel.addMessage(
                                            receiverId,
                                            contentToSend
                                        )
                                    }
                                },
                                enabled = messageText.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                            }
                        }
                    }
                }
            }

            if (showUserSelectionDialog) {
                UserSelectionDialog(
                    allUsers = allUsers.filter { it.id != currentUserId },                    searchQuery = userSearchQuery,
                    onSearchQueryChange = { userSearchQuery = it },
                    onUserSelected = {
                        selectedUserForChat = it
                        showUserSelectionDialog = false
                        userSearchQuery = ""
                    },
                    onDismiss = {
                        showUserSelectionDialog = false
                        userSearchQuery = ""
                        if (selectedUserForChat == null) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun UserSelectionDialog(
    allUsers: List<GeneralChatUser>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUserSelected: (GeneralChatUser) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredUsers by remember(searchQuery, allUsers) {
        derivedStateOf {
            if (searchQuery.isNotBlank()) {
                allUsers.filter { user ->
                    user.displayName.contains(searchQuery, ignoreCase = true)
                }
            } else {
                allUsers
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kullanıcı Seç") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Kullanıcı ara...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (filteredUsers.isEmpty()) {
                    Text("Sonuç bulunamadı.", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(filteredUsers, key = { it.id.ifBlank { UUID.randomUUID().toString() } }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Kullanıcı ikonu",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    user.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
