package com.neval.anoba.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.R
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.drawer.DrawerContent
import com.neval.anoba.letter.LetterCard
import com.neval.anoba.letter.LetterViewModel
import com.neval.anoba.livestream.LiveSheet
import com.neval.anoba.livestream.LiveStreamViewModel
import com.neval.anoba.photo.PhotoCard
import com.neval.anoba.photo.PhotoViewModel
import com.neval.anoba.ses.SesCard
import com.neval.anoba.ses.SesViewModel
import com.neval.anoba.video.VideoCard
import com.neval.anoba.video.VideoViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = koinViewModel(),
    letterViewModel: LetterViewModel = koinViewModel(),
    photoViewModel: PhotoViewModel = koinViewModel(),
    sesViewModel: SesViewModel = koinViewModel(),
    liveStreamViewModel: LiveStreamViewModel = koinViewModel(),
    videoViewModel: VideoViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    // Load initial data for all content types
    LaunchedEffect(key1 = Unit) {
        letterViewModel.loadInitialData()
        photoViewModel.loadInitialData()
        videoViewModel.loadVideos()
        sesViewModel.loadSesler()
    }

    // Collect states from ViewModels
    val letters by letterViewModel.letters.collectAsState()
    val photos by photoViewModel.photos.collectAsState()
    val audios by sesViewModel.sesler.collectAsState()
    val videos by videoViewModel.videos.collectAsState()

    // Combine all content into a single, sorted list
    val allContentItems by remember(photos, letters, audios, videos) {
        derivedStateOf {
            val photoItems = photos.map { ContentItem.PhotoContent(it) }
            val letterItems = letters.map { ContentItem.LetterContent(it) }
            val audioItems = audios.map { ContentItem.AudioContent(it) }
            val videoItems = videos.map { ContentItem.VideoContent(it) }
            (photoItems + letterItems + audioItems + videoItems)
                .sortedByDescending { it.timestamp ?: Long.MIN_VALUE }
        }
    }

    val listState = rememberLazyListState()
    var showSearchDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var startPoliceAnimation by remember { mutableStateOf(false) }
    
    val redContainer = Color(0xFFFFEBEE)
    val redIcon = Color(0xFFD32F2F)
    val purpleContainer = Color(0xFFF3E5F5)
    val purpleIcon = Color(0xFF7B1FA2)

    DrawerContent(navController = navController, drawerState = drawerState) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenMaxWidth = this.maxWidth
            val screenMaxHeight = this.maxHeight
            val isCompact = screenMaxWidth < 600.dp

            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetPeekHeight = 120.dp,
                sheetContent = {
                    LiveSheet(
                        scope = scope,
                        bottomSheetState = bottomSheetScaffoldState.bottomSheetState,
                        liveStreamViewModel = liveStreamViewModel,
                        isCompact = isCompact
                    )
                },
                topBar = {
                    Column {
                        TopAppBar(
                            title = { },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menü")
                                }
                            },
                            actions = {
                                Card(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { startPoliceAnimation = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (startPoliceAnimation) Color.Red else Color(0xFFE3F2FD)
                                    )
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.polis_karakolu),
                                            contentDescription = "Polis",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Card(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { showEntryDialog = true },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.dur_el),
                                            contentDescription = "Girilmez",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Ara")
                                }
                            }
                        )

                        if (showSearchDialog) {
                            AlertDialog(
                                onDismissRequest = { showSearchDialog = false },
                                confirmButton = {
                                    TextButton(onClick = { showSearchDialog = false }) { Text("Tamam") }
                                },
                                title = { Text("Yapım Aşamasında") },
                                text = { Text("Arama özelliği henüz aktif değil.") }
                            )
                        }

                        if (showEntryDialog) {
                            AlertDialog(
                                onDismissRequest = { showEntryDialog = false },
                                confirmButton = {
                                    TextButton(onClick = { showEntryDialog = false }) { Text("Tamam") }
                                },
                                title = { Text("Girilmez") },
                                text = { Text("Bu giriş şu anda kapalı.") }
                            )
                        }

                        HorizontalDivider(Modifier.fillMaxWidth(), color = Color.LightGray, thickness = 1.dp)

                        // Quick Access Buttons
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                        ) {
                            item {
                                IconInCardButton(
                                    onClick = { navController.navigate(Constants.VIDEO_NAV_GRAPH) },
                                    icon = Icons.Filled.Videocam, 
                                    contentDescription = "Videolar",
                                    cardColor = purpleContainer, 
                                    iconColor = purpleIcon
                                )
                            }
                            item {
                                IconInCardButton(
                                    onClick = { navController.navigate(Constants.PHOTO_NAV_GRAPH) },
                                    icon = Icons.Filled.CameraAlt, 
                                    contentDescription = "Fotoğraflar",
                                    cardColor = redContainer, 
                                    iconColor = redIcon
                                )
                            }
                            item {
                                IconInCardButton(
                                    onClick = { navController.navigate(Constants.SES_NAV_GRAPH) },
                                    icon = Icons.Filled.Mic, 
                                    contentDescription = "Ses Kayıtları",
                                    cardColor = Color(0xFFFFF3E0), 
                                    iconColor = Color(0xFFE65100)
                                )
                            }
                            item {
                                IconInCardButton(
                                    onClick = { navController.navigate(Constants.LETTER_NAV_GRAPH) },
                                    icon = Icons.Filled.Edit, 
                                    contentDescription = "Mektuplar",
                                    cardColor = Color(0xFFE3F2FD), 
                                    iconColor = Color(0xFF1976D2)
                                )
                            }
                            item {
                                IconInCardButton(
                                    onClick = { navController.navigate(Constants.CHAT_NAV_GRAPH) },
                                    icon = Icons.AutoMirrored.Filled.Chat, 
                                    contentDescription = "Chat",
                                    cardColor = Color(0xFFE8F5E9), 
                                    iconColor = Color(0xFF388E3C)
                                )
                            }
                        }

                        HorizontalDivider(Modifier.fillMaxWidth(), color = Color.LightGray, thickness = 1.dp)
                    }
                }
            ) { innerPadding ->
                // Değişiklik burada: Column -> Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 1. Ana içerik (altta kalacak katman)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().animateContentSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (allContentItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Henüz görüntülenecek bir içerik yok.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            items(items = allContentItems, key = { it.id }) { contentItem ->
                                when (contentItem) {
                                    is ContentItem.PhotoContent -> {
                                        val canDelete = currentUserId == contentItem.photo.ownerId || userRole == "ADMIN"
                                        PhotoCard(
                                            photo = contentItem.photo, navController = navController,
                                            canDelete = canDelete,
                                            onDeleteClicked = { photoViewModel.deletePhoto(contentItem.photo.id) },
                                            isCommentSectionClickable = false
                                        )
                                    }
                                    is ContentItem.LetterContent -> {
                                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            LetterCard(
                                                letter = contentItem.letter, navController = navController,
                                                currentUserId = currentUserId ?: "", userRole = userRole,
                                                isCommentSectionClickable = false, showContentPreview = true
                                            )
                                        }
                                    }
                                    is ContentItem.AudioContent -> {
                                        val canDelete = currentUserId == contentItem.audio.ownerId || userRole == "ADMIN"
                                        SesCard(
                                            ses = contentItem.audio, navController = navController,
                                            canDelete = canDelete,
                                            onDeleteClicked = { sesViewModel.deleteSes(contentItem.audio.id) },
                                            isCommentSectionClickable = false, showPlayIcon = false
                                        )
                                    }
                                    is ContentItem.VideoContent -> {
                                        val canDelete = currentUserId == contentItem.video.ownerId || userRole == "ADMIN"
                                        VideoCard(
                                            video = contentItem.video, navController = navController,
                                            canDelete = canDelete,
                                            onDeleteClicked = { videoViewModel.deleteVideo(contentItem.video.id) },
                                            onClick = {
                                                navController.navigate(
                                                    Constants.VIDEO_DETAIL_SCREEN.replace("{videoId}", contentItem.video.id)
                                                )
                                            },
                                            isCommentSectionClickable = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Animasyon (üstte kalacak katman)
                    if (startPoliceAnimation) {
                        // POLİS LİSTESİ
                        val polisImages = listOf(
                            R.drawable.polis_2, R.drawable.polis_3,
                            R.drawable.polis_4, R.drawable.polis_motorsiklet3,
                            R.drawable.polis_3, R.drawable.polis_4,
                            R.drawable.polis_araba2, R.drawable.polis_5,
                            R.drawable.polis_4, R.drawable.polis_6,
                            R.drawable.polis_araba2, R.drawable.polis_7,
                            R.drawable.polis_6, R.drawable.polis_8,
                            R.drawable.polis_7, R.drawable.polis_9,
                            R.drawable.polis_8, R.drawable.polis_10,
                            R.drawable.polis_araba2,R.drawable.polis_11,
                            R.drawable.polis_minibus1, R.drawable.polis_4,
                            R.drawable.polis_motorsiklet, R.drawable.polis_2,
                            R.drawable.polis_motorsiklet3, R.drawable.polis_3,
                            R.drawable.polis_11, R.drawable.polis_12,


                        ).shuffled()

                        polisImages.forEachIndexed { index, imageRes ->
                            PolisAnim(
                                imageRes = imageRes,
                                maxWidth = screenMaxWidth, maxHeight = screenMaxHeight,
                                startDelay = (index * 200L),
                                animationDuration = (2000..4000).random().toLong(),
                                onFinished = {
                                    if (index == polisImages.lastIndex) {
                                        startPoliceAnimation = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconInCardButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconSize: Dp = 24.dp,
    cardShape: RoundedCornerShape = RoundedCornerShape(8.dp),
    cardElevationDefaults: CardElevation = CardDefaults.cardElevation(2.dp)
) {
    Card(
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick)
            .size(48.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = cardElevationDefaults
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
