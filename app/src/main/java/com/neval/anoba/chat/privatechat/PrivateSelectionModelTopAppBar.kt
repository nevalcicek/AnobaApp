package com.neval.anoba.chat.privatechat

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSelectionModeTopAppBar(
    selectedCount: Int,
    onCloseSelectionMode: () -> Unit,
    canDeleteAnySelected: Boolean,
    onDeleteSelected: () -> Unit,
    canCopyAnySelected: Boolean,
    onCopySelected: () -> Unit,
    canEdit: Boolean,
    onEditSelected: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount seçili mesaj") },
        navigationIcon = {
            IconButton(onClick = onCloseSelectionMode) {
                Icon(Icons.Filled.Close, contentDescription = "Seçimi Kapat")
            }
        },
        actions = {
            if (canCopyAnySelected) {
                IconButton(onClick = onCopySelected) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala")
                }
            }
            if (canDeleteAnySelected) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (canEdit) {
                IconButton(onClick = onEditSelected) {
                    Icon(Icons.Filled.Edit, contentDescription = "Düzenle")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.height(56.dp)
    )
}
