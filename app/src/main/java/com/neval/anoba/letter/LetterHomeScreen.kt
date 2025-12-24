package com.neval.anoba.letter

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.models.User
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterHomeScreen(
    navController: NavController,
    viewModel: LetterViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val letters by viewModel.letters.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var newLetterContent by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRecipientDialog by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Mektup Gizliliği") },
            text = { Text("Bu mektubu kimler görebilsin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendLetter(
                            content = newLetterContent,
                            privacy = LetterPrivacy.PUBLIC,
                            onResult = { success ->
                                if (success) {
                                    newLetterContent = ""
                                    Toast.makeText(context, "Mektup gönderildi!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Mektup gönderilemedi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        showPrivacyDialog = false
                    }
                ) {
                    Text("Herkese Açık")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPrivacyDialog = false
                        showRecipientDialog = true
                    }
                ) {
                    Text("Mühürlü")
                }
            }
        )
    }

    if (showRecipientDialog) {
        LetterRecipientSelectionDialog(
            allUsers = allUsers.filter { it.uid != currentUserId },
            searchQuery = userSearchQuery,
            onSearchQueryChange = { userSearchQuery = it },
            onUserSelected = { selectedUser ->
                viewModel.sendLetter(
                    content = newLetterContent,
                    privacy = LetterPrivacy.SEALED,
                    recipientId = selectedUser.uid,
                    recipientUsername = selectedUser.displayName,
                    onResult = { success ->
                        if (success) {
                            newLetterContent = ""
                            userSearchQuery = ""
                            Toast.makeText(context, "Mühürlü mektup, ${selectedUser.displayName} adlı kullanıcıya gönderildi!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Mektup gönderilemedi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showRecipientDialog = false
            },
            onDismiss = {
                showRecipientDialog = false
                userSearchQuery = ""
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mektuplar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                        Icon(Icons.Default.Home, "Ana Sayfa")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (isSending && letters.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(letters, key = { it.id.ifBlank { UUID.randomUUID().toString() } }) {
                        LetterCard(
                            letter = it,
                            navController = navController,
                            currentUserId = currentUserId ?: "",
                            userRole = userRole
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = newLetterContent,
                    onValueChange = { newLetterContent = it },
                    label = { Text("Yeni bir mektup yaz...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (newLetterContent.isNotBlank()) {
                                showPrivacyDialog = true
                            } else {
                                Toast.makeText(context, "Mektup boş olamaz.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = newLetterContent.isNotBlank() && !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Gönder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterRecipientSelectionDialog(
    allUsers: List<User>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUserSelected: (User) -> Unit,
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
        title = { Text("Alıcı Seç") },
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
                        items(filteredUsers, key = { it.uid.ifBlank { UUID.randomUUID().toString() } }) { user ->
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
                                    text = user.displayName,
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