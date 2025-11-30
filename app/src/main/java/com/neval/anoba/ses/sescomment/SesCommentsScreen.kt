package com.neval.anoba.ses.sescomment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.neval.anoba.common.services.AuthServiceInterface
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.utils.DateTimeUtils
import com.neval.anoba.login.state.AuthState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesCommentsScreen(
    navController: NavController,
    sesId: String,
) {
    val viewModel: SesCommentViewModel = koinViewModel(parameters = { parametersOf(sesId) })
    val authService: AuthServiceInterface = koinInject()

    val comments by viewModel.comments.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val error by viewModel.error.collectAsState()
    val replyingToComment by viewModel.replyingToComment.collectAsState()
    val listState = rememberLazyListState()
    val authState by authService.authStateFlow.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val commentTree = remember(comments) {
        val commentMap = comments.associateBy { it.id }
        val tree = mutableMapOf<SesComment, MutableList<SesComment>>()
        comments.forEach { comment ->
            if (comment.replyToId == null) {
                tree.getOrPut(comment) { mutableListOf() }
            } else {
                commentMap[comment.replyToId]?.let { parent ->
                    tree.getOrPut(parent) { mutableListOf() }.add(comment)
                }
            }
        }
        tree.mapValues { it.value.sortedBy { reply -> reply.timestamp } }
    }
    val mainComments = commentTree.keys.sortedBy { it.timestamp }

    LaunchedEffect(mainComments.size) {
        if (mainComments.isNotEmpty()) {
            listState.animateScrollToItem(index = 0)
        }
    }

    LaunchedEffect(replyingToComment) {
        if (replyingToComment != null) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Yorumlar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.HOME_SCREEN) }) {
                        Icon(Icons.Default.Home, contentDescription = "Ana Sayfa")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column {
                    AnimatedVisibility(visible = replyingToComment != null, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${replyingToComment?.username ?: "..."} adlı kullanıcıya yanıt veriliyor",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            IconButton(onClick = { viewModel.cancelReply() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Yanıtı İptal Et")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { viewModel.onCommentTextChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Yorumunu yaz...") },
                            maxLines = 5
                        )
                        IconButton(
                            onClick = { viewModel.addComment() },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else if (comments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz hiç yorum yok. İlk yorumu sen yap!")
                }
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    mainComments.forEach { mainComment ->
                        item(key = mainComment.id) {
                            val currentUserId = (authState as? AuthState.Authenticated)?.uid
                            SesCommentItem(
                                comment = mainComment,
                                currentUserId = currentUserId,
                                onLikeClicked = { viewModel.likeComment(mainComment.id) },
                                onReplyClicked = { viewModel.onReplyClicked(mainComment) },
                                onDeleteClicked = { viewModel.deleteComment(mainComment) }
                            )
                        }
                        items(commentTree[mainComment].orEmpty(), key = { "reply_${it.id}" }) { reply ->
                            val currentUserId = (authState as? AuthState.Authenticated)?.uid
                            Row {
                                Spacer(modifier = Modifier.width(52.dp))
                                SesCommentItem(
                                    comment = reply,
                                    currentUserId = currentUserId,
                                    onLikeClicked = { viewModel.likeComment(reply.id) },
                                    onReplyClicked = { viewModel.onReplyClicked(mainComment) },
                                    onDeleteClicked = { viewModel.deleteComment(reply) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SesCommentItem(
    comment: SesComment,
    currentUserId: String?,
    onLikeClicked: () -> Unit,
    onReplyClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val isLiked = currentUserId?.let { comment.likedBy.contains(it) } ?: false

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = comment.userProfileUrl,
                contentDescription = "Profil Resmi",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                error = rememberVectorPainter(Icons.Default.AccountCircle)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateTimeUtils.formatRelativeTime(comment.timestamp?.time ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReplyClicked) {
                        Text("Yanıtla", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    if (currentUserId == comment.userId) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDeleteClicked) {
                            Text("Sil", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onLikeClicked, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Beğen",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (comment.likedBy.isNotEmpty()) {
                Text(
                    text = comment.likedBy.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
