package com.neval.anoba.chat.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.neval.anoba.common.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatListScreen(
    navController: NavHostController,
    groupChatViewModel: GroupChatViewModel,
) {
    val groups by groupChatViewModel.groups.collectAsStateWithLifecycle()
    val currentUserId = groupChatViewModel.currentUserId
    val scope = rememberCoroutineScope()

    var groupName by rememberSaveable { mutableStateOf("") }
    var groupDescription by rememberSaveable { mutableStateOf("") }
    var isPrivate by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showGroupList by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Grup Ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopAppBar(
                title = { Text("Grup Sohbeti") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri Dön")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                        Icon(Icons.Filled.Home, contentDescription = "Ana Sayfa")
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = { showGroupList = !showGroupList },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(if (showGroupList) "Grup Listesini Gizle" else "Grup Listesini Göster")
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(visible = showGroupList) {
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Henüz hiç grup yok. Bir tane oluşturun!")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = groups, key = { it.id.ifBlank { UUID.randomUUID().toString() } }) { group ->
                                GroupItem(
                                    group = group,
                                    navController = navController,
                                    groupChatViewModel = groupChatViewModel
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmedName = groupName.trim()
                            when {
                                trimmedName.isBlank() -> errorMessage = "Grup adı boş olamaz."
                                currentUserId.isNullOrBlank() -> errorMessage = "Kullanıcı tanımlanamadı."
                                else -> {
                                    errorMessage = null
                                    isLoading = true
                                    scope.launch {
                                        val newGroupId = groupChatViewModel.createGroup(
                                            trimmedName,
                                            isPrivate,
                                            groupDescription.ifBlank { "Yeni bir grup" }
                                        )
                                        isLoading = false
                                        if (newGroupId != null) {
                                            successMessage = "✅ Grup başarıyla oluşturuldu!"
                                            delay(1500)
                                            successMessage = null
                                            groupName = ""
                                            groupDescription = ""
                                            isPrivate = false
                                            showDialog = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("Oluştur")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("İptal")
                    }
                },
                title = { Text("Yeni Grup Oluştur") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Grup Adı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = groupDescription,
                            onValueChange = { groupDescription = it },
                            label = { Text("Grup Açıklaması (isteğe bağlı)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Özel Grup mu?", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isPrivate,
                                onCheckedChange = { isPrivate = it },
                                enabled = !isLoading,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                        if (successMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = successMessage!!, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}
