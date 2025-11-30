package com.neval.anoba.chat.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel,
    groupId: String
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val currentUserId = groupChatViewModel.currentUserId
    val context = LocalContext.current

    if (currentUserId == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Giriş yapılmamış. Lütfen tekrar deneyin.")
        }
        return
    }

    val messageText by groupChatViewModel.messageText.collectAsStateWithLifecycle()
    val messages by groupChatViewModel.currentGroupMessages.collectAsStateWithLifecycle()
    val activeGroup by groupChatViewModel.activeGroup.collectAsStateWithLifecycle()
    val selectedMessages by groupChatViewModel.selectedMessages.collectAsStateWithLifecycle()
    val isSelectionModeActive by groupChatViewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showEditGroupNameDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(activeGroup?.name ?: "") }

    LaunchedEffect(groupId) {
        if (groupId.isNotBlank()) {
            groupChatViewModel.setActiveGroupId(groupId)
        }
    }

    LaunchedEffect(activeGroup) {
        newGroupName = activeGroup?.name ?: ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding().imePadding(),
        topBar = {
            if (isSelectionModeActive) {
                GroupSelectionModeTopAppBar(
                    selectedCount = selectedMessages.size,
                    onCloseSelectionMode = { groupChatViewModel.clearMessageSelection() },
                    canDeleteAnySelected = selectedMessages.any {
                        it.senderId == currentUserId || activeGroup?.ownerId == currentUserId
                    },
                    onDeleteSelected = { groupChatViewModel.deleteSelectedMessages(groupId) },
                    canCopyAnySelected = selectedMessages.isNotEmpty(),
                    onCopySelected = { groupChatViewModel.copySelectedMessagesToClipboard(context) },
                    canEdit = selectedMessages.size == 1 && selectedMessages.first().senderId == currentUserId,
                    onEditSelected = {
                        // TODO: Düzenleme modunu başlatacak fonksiyon burada çağrılmalı
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = {
                                        navController.navigate("${Constants.GROUP_INFO_SCREEN}/$groupId")
                                    }
                                )
                        ) {
                            Text(activeGroup?.name ?: "Grup Sohbeti")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            groupChatViewModel.setActiveGroupId(null)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri Dön")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            groupChatViewModel.setActiveGroupId(null)
                            navController.navigate(Constants.HOME_SCREEN) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Filled.Home, contentDescription = "Ana Sayfa")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (!isSelectionModeActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { groupChatViewModel.updateMessageText(it) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(200)
                                            if (messages.isNotEmpty()) {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    }
                                },
                            placeholder = { Text("Mesajınızı yazın") },
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            maxLines = 5
                        )

                        IconButton(
                            onClick = {
                                val textToSend = messageText.trim()
                                if (textToSend.isNotBlank()) {
                                    groupChatViewModel.addMessageToCurrentGroup(textToSend)
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gönder",
                                tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            GroupMessagesList(
                modifier = Modifier.weight(1f),
                messages = messages,
                listState = listState,
                currentUserId = currentUserId,
                selectedMessages = selectedMessages,
                onMessageClick = { groupChatViewModel.onMessageClicked(it) },
                onMessageLongClick = { groupChatViewModel.onMessageLongClicked(it) },
                bringIntoViewRequester = bringIntoViewRequester
            )

            if (showEditGroupNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditGroupNameDialog = false },
                    title = { Text("Grup Adını Düzenle") },
                    text = {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Yeni Grup Adı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newGroupName.isNotBlank() && newGroupName != activeGroup?.name) {
                                groupChatViewModel.updateGroupName(groupId, newGroupName)
                            }
                            showEditGroupNameDialog = false
                        }) { Text("Kaydet") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditGroupNameDialog = false }) { Text("İptal") }
                    }
                )
            }

            if (showDeleteGroupDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteGroupDialog = false },
                    title = { Text("Grubu Sil") },
                    text = { Text("Bu grubu ve içindeki tüm mesajları silmek istediğinize emin misiniz?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                groupChatViewModel.deleteGroup(groupId)
                                showDeleteGroupDialog = false
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Evet, Sil") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteGroupDialog = false }) { Text("İptal") }
                    }
                )
            }
        }
    }
}
