package com.neval.anoba.chat.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.neval.anoba.chat.general.GeneralChatUser
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel,
    groupId: String,
    currentUserId: String?
) {
    val currentGroup by groupChatViewModel.activeGroup.collectAsStateWithLifecycle()
    val groupMembers by groupChatViewModel.groupMembers.collectAsStateWithLifecycle()
    val errorMessage by groupChatViewModel.errorMessage.collectAsStateWithLifecycle()
    val allUsers by groupChatViewModel.allUsers.collectAsStateWithLifecycle()
    val isMuted by groupChatViewModel.isMuted.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditing by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    var groupDescriptionInput by remember { mutableStateOf("") }
    var privacyStateSwitchEditable by remember { mutableStateOf(false) }
    var originalPrivacyState by remember { mutableStateOf(false) }

    var showDeleteGroupConfirmDialog by remember { mutableStateOf(false) }
    var showInviteUserDialog by remember { mutableStateOf(false) }

    var showSelectMemberToRemoveDialog by remember { mutableStateOf(false) }
    var selectedUserToRemove by remember { mutableStateOf<GeneralChatUser?>(null) }
    var showConfirmRemoveDialog by remember { mutableStateOf(false) }

    var userSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        if (groupId.isNotBlank()) {
            groupChatViewModel.setActiveGroupId(groupId)
        }
    }

    LaunchedEffect(currentGroup) {
        currentGroup?.let { group ->
            if (!isEditing) {
                groupNameInput = group.name
                groupDescriptionInput = group.description
                privacyStateSwitchEditable = group.isPrivate
            }
            originalPrivacyState = group.isPrivate
            groupChatViewModel.loadGroupMembers(group.id)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            groupChatViewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Yönetici İşlemleri", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                            currentGroup?.let {
                                groupNameInput = it.name
                                groupDescriptionInput = it.description
                                privacyStateSwitchEditable = originalPrivacyState
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            if (isEditing) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditing) "Düzenlemeyi İptal Et" else "Geri"
                        )
                    }
                },
                actions = {
                    currentGroup?.let { group ->
                        if (group.ownerId == currentUserId) {
                            if (isEditing) {
                                IconButton(onClick = {
                                    scope.launch {
                                        groupChatViewModel.updateGroupInfo(groupId, groupNameInput, groupDescriptionInput)
                                        groupChatViewModel.setGroupPrivacy(groupId, privacyStateSwitchEditable)
                                        isEditing = false
                                        originalPrivacyState = privacyStateSwitchEditable
                                    }
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Değişiklikleri Kaydet")
                                }
                            } else {
                                IconButton(onClick = {
                                    groupNameInput = group.name
                                    groupDescriptionInput = group.description
                                    privacyStateSwitchEditable = group.isPrivate
                                    originalPrivacyState = group.isPrivate
                                    isEditing = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Grubu Düzenle")
                                }
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val group = currentGroup

        Box(modifier = Modifier.fillMaxSize()) {
            if (group == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (group.imageUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = group.imageUrl),
                                contentDescription = "Grup Profili",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Varsayılan Grup İkonu",
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Grup Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing && group.ownerId == currentUserId,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupDescriptionInput,
                        onValueChange = { groupDescriptionInput = it },
                        label = { Text("Grup Açıklaması") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 100.dp),
                        enabled = isEditing && group.ownerId == currentUserId
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (group.ownerId == currentUserId) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grubu Sessize Al", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isMuted,
                                onCheckedChange = { newState ->
                                    currentUserId.let { uid ->
                                        if (newState) groupChatViewModel.muteGroup(groupId, uid)
                                        else groupChatViewModel.unmuteGroup(groupId, uid)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grup Özel mi?", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = if (isEditing) privacyStateSwitchEditable else group.isPrivate,
                                onCheckedChange = { if (isEditing) privacyStateSwitchEditable = it },
                                enabled = isEditing
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Üye Yönetimi", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()){
                            Button(
                                onClick = { showInviteUserDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = "Üye Davet Et", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Üye Davet Et")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showSelectMemberToRemoveDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Filled.PersonRemove, contentDescription = "Üye Sil", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Üye Sil")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Grup Ayarları", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.error))
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        Button(
                            onClick = { showDeleteGroupConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Bu Grubu Kalıcı Olarak Sil") }
                    }
                }
            }
        }
    }

    if (showInviteUserDialog) {
        UserSelectionDialog(
            title = "Üye Davet Et",
            users = allUsers,
            currentMemberIds = groupMembers.map { it.id }.toSet(),
            currentUserId = currentUserId,
            searchQuery = userSearchQuery,
            onSearchQueryChange = { userSearchQuery = it },
            onUserSelected = { user ->
                scope.launch {
                    groupChatViewModel.inviteUserToGroup(groupId, user.id)
                }
                showInviteUserDialog = false
                userSearchQuery = ""
            },
            onDismiss = {
                showInviteUserDialog = false
                userSearchQuery = ""
            }
        )
    }

    if (showDeleteGroupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirmDialog = false },
            title = { Text("Grubu Sil") },
            text = { Text("Bu grubu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            groupChatViewModel.deleteGroup(groupId)
                            showDeleteGroupConfirmDialog = false
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirmDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showSelectMemberToRemoveDialog) {
        UserSelectionDialog(
            title = "Üyeyi Sil",
            users = groupMembers.filter { it.id != currentGroup?.ownerId && it.id != currentUserId },
            currentMemberIds = emptySet(),
            currentUserId = currentUserId,
            searchQuery = userSearchQuery,
            onSearchQueryChange = { userSearchQuery = it },
            onUserSelected = { user ->
                selectedUserToRemove = user
                showSelectMemberToRemoveDialog = false
                showConfirmRemoveDialog = true
                userSearchQuery = ""
            },
            onDismiss = {
                showSelectMemberToRemoveDialog = false
                userSearchQuery = ""
            }
        )
    }

    if (showConfirmRemoveDialog && selectedUserToRemove != null) {
        AlertDialog(
            onDismissRequest = { showConfirmRemoveDialog = false },
            title = { Text("Üyeyi Sil Onayı") },
            text = { Text("${selectedUserToRemove?.displayName} adlı üyeyi gruptan kalıcı olarak silmek istediğinizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedUserToRemove?.let {
                            scope.launch {
                                groupChatViewModel.removeUserFromGroup(groupId, it.id)
                                selectedUserToRemove = null
                                showConfirmRemoveDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRemoveDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun UserSelectionDialog(
    title: String,
    users: List<GeneralChatUser>,
    currentMemberIds: Set<String>,
    currentUserId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUserSelected: (GeneralChatUser) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredUsers by remember(searchQuery, users, currentMemberIds) {
        derivedStateOf {
            val availableUsers = users.filter { user -> user.id !in currentMemberIds && user.id != currentUserId }

            if (searchQuery.isNotBlank()) {
                availableUsers.filter {
                    it.displayName.contains(searchQuery, ignoreCase = true)
                }
            } else {
                availableUsers
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredUsers, key = { it.id.ifBlank { UUID.randomUUID().toString() } }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = user.photoUrl ?: "https://via.placeholder.com/40"),
                                    contentDescription = "${user.displayName} profil resmi",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
