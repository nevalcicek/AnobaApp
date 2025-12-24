package com.neval.anoba.letter

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.letter.lettercomment.LetterCommentViewModel
import com.neval.anoba.letter.letteremoji.LetterEmojiReactionRow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterDetailScreen(
    letterId: String,
    navController: NavController,
    viewModel: LetterViewModel = koinViewModel(),
    commentViewModel: LetterCommentViewModel = koinViewModel { parametersOf(letterId) },
    authViewModel: AuthViewModel = koinViewModel()
) {
    val letterState by viewModel.selectedLetter.collectAsState()
    val comments by commentViewModel.comments.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(letterId) {
        if (letterId.isNotBlank()) {
            viewModel.getLetterDetails(letterId)
            viewModel.incrementLetterViewCount(letterId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mektup Detayı") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, letterState?.content ?: "")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Paylaş")
                    }
                    IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                        Icon(Icons.Default.Home, contentDescription = "Ana Sayfa")
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        letterState?.let { safeLetter ->
            DeleteConfirmationDialog(
                showDialog = showDeleteConfirmDialog,
                onConfirm = {
                    viewModel.deleteLetter(safeLetter.id)
                    showDeleteConfirmDialog = false
                    Toast.makeText(context, "🗑️ Mektup silindi", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )

            LetterDetailContent(
                modifier = Modifier.padding(innerPadding),
                safeLetter = safeLetter,
                commentsCount = comments.size,
                viewModel = viewModel,
                authViewModel = authViewModel,
                navController = navController,
                letterId = letterId,
                onDeleteClick = { showDeleteConfirmDialog = true }
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("⏳ Mektup yükleniyor...")
            }
        }
    }
}

@Composable
private fun LetterDetailContent(
    modifier: Modifier = Modifier,
    safeLetter: LetterModel,
    commentsCount: Int,
    viewModel: LetterViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    letterId: String,
    onDeleteClick: () -> Unit
) {
    val userReactions by viewModel.userReactions.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val selectedEmoji = userReactions[letterId]

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gönderen: ${safeLetter.username.ifBlank { "Anonim" }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            safeLetter.timestamp?.let{
                Text(
                    text = DateTimeUtils.formatRelativeTime(it.toDate().time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LetterContentCard(
            modifier = Modifier.weight(2f),
            safeLetter = safeLetter
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (safeLetter.viewCount > 0) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Görüntülenme Sayısı",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = safeLetter.viewCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                val topReaction = safeLetter.reactions.maxByOrNull { it.value }
                if (topReaction != null && topReaction.value > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topReaction.key,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = topReaction.value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val canDelete = currentUserId == safeLetter.ownerId || userRole == "ADMIN"
            if (canDelete) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Mektubu Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LetterInteractionControls(
            reactions = safeLetter.reactions,
            selectedEmoji = selectedEmoji,
            commentsCount = commentsCount,
            onReact = { emoji -> viewModel.toggleReaction(letterId, emoji) },
            onCommentClick = {
                val route = Constants.LETTER_COMMENTS_ROUTE.replace("{letterId}", letterId)
                navController.navigate(route)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.width(200.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Mektubu Kapat")
        }

        Spacer(modifier = Modifier.height(34.dp))
    }
}

@Composable
private fun LetterContentCard(
    modifier: Modifier = Modifier,
    safeLetter: LetterModel
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = safeLetter.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun LetterInteractionControls(
    reactions: Map<String, Long>,
    selectedEmoji: String?,
    commentsCount: Int,
    onReact: (String) -> Unit,
    onCommentClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LetterEmojiReactionRow(
            reactions = reactions,
            selectedEmoji = selectedEmoji,
            onReact = onReact,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCommentClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = "Yorum Yap",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Yorum Yap",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (commentsCount > 0) {
                Text(
                    text = "$commentsCount Yorum",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Mektubu Sil") },
            text = { Text("Bu mektubu kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Hayır") }
            }
        )
    }
}
