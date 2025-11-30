package com.neval.anoba.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.neval.anoba.R
import com.neval.anoba.common.viewmodel.AuthViewModel
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.drawer.DrawerContent
import com.neval.anoba.letter.LetterCard
import com.neval.anoba.letter.LetterViewModel
import com.neval.anoba.livestream.LiveStream
import com.neval.anoba.livestream.LiveStreamViewModel
import com.neval.anoba.photo.PhotoCard
import com.neval.anoba.photo.PhotoViewModel
import com.neval.anoba.ses.SesCard
import com.neval.anoba.ses.SesViewModel
import com.neval.anoba.video.VideoCard
import com.neval.anoba.video.VideoViewModel
import kotlinx.coroutines.CoroutineScope
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val standardBottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = standardBottomSheetState
    )

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()

    LaunchedEffect(key1 = Unit) {
        letterViewModel.loadInitialData()
        photoViewModel.loadInitialData()
        videoViewModel.loadVideos()
        sesViewModel.loadSesler()
    }

    val letters by letterViewModel.letters.collectAsState()
    val photos by photoViewModel.photos.collectAsState()
    val audios by sesViewModel.sesler.collectAsState()
    val videos by videoViewModel.videos.collectAsState()

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

    LaunchedEffect(allContentItems) {
        if (allContentItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val redContainer = Color(0xFFFFEBEE)
    val redIcon = Color(0xFFD32F2F)
    val purpleContainer = Color(0xFFF3E5F5)
    val purpleIcon = Color(0xFF7B1FA2)

    var showSearchDialog by remember { mutableStateOf(false) }
    var showPoliceDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }

    DrawerContent(navController = navController, drawerState = drawerState) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 120.dp,
            sheetContent = {
                LiveStreamBottomSheetContent(
                    scope = scope,
                    bottomSheetState = bottomSheetScaffoldState.bottomSheetState,
                    liveStreamViewModel = liveStreamViewModel
                )
            },
            topBar = {
                Column {
                    TopAppBar(
                        title = { /* Başlık kaldırıldı */ },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menü")
                            }
                        },
                        actions = {
                            Card(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { showPoliceDialog = true },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
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
                                TextButton(onClick = { showSearchDialog = false }) {
                                    Text("Tamam")
                                }
                            },
                            title = { Text("Yapım Aşamasında") },
                            text = { Text("Arama özelliği henüz aktif değil.") }
                        )
                    }

                    HorizontalDivider(
                        Modifier.fillMaxWidth(),
                        color = Color.LightGray,
                        thickness = 1.dp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
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
                    }
                    HorizontalDivider(
                        Modifier.fillMaxWidth(),
                        color = Color.LightGray,
                        thickness = 1.dp
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPoliceDialog) {
                    AlertDialog(
                        onDismissRequest = { showPoliceDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showPoliceDialog = false }) {
                                Text("Tamam")
                            }
                        },
                        title = { Text("Yapım Aşamasında") },
                        text = { Text("Polis kartı henüz aktif değil.") }
                    )
                }

                if (showEntryDialog) {
                    AlertDialog(
                        onDismissRequest = { showEntryDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showEntryDialog = false }) {
                                Text("Tamam")
                            }
                        },
                        title = { Text("Yapım Aşamasında") },
                        text = { Text("Girilmez kartı henüz aktif değil.") }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (allContentItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(16.dp),
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
                        items(
                            items = allContentItems,
                            key = { it.id }
                        ) { contentItem ->
                            when (contentItem) {
                                is ContentItem.PhotoContent -> {
                                    val canDelete =
                                        currentUserId == contentItem.photo.ownerId || userRole == "ADMIN"
                                    PhotoCard(
                                        photo = contentItem.photo,
                                        navController = navController,
                                        canDelete = canDelete,
                                        onDeleteClicked = { photoViewModel.deletePhoto(contentItem.photo.id) },
                                        isCommentSectionClickable = false
                                    )
                                }
                                is ContentItem.LetterContent -> {
                                    // KART GENİŞLİĞİNİ AYARLAMAK İÇİN SAĞDAN VE SOLDAN BOŞLUK
                                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        LetterCard(
                                            letter = contentItem.letter,
                                            navController = navController,
                                            currentUserId = currentUserId ?: "",
                                            userRole = userRole,
                                            isCommentSectionClickable = false,
                                            showContentPreview = true
                                        )
                                    }
                                }
                                is ContentItem.AudioContent -> {
                                    val canDelete =
                                        currentUserId == contentItem.audio.ownerId || userRole == "ADMIN"
                                    SesCard(
                                        ses = contentItem.audio,
                                        navController = navController,
                                        canDelete = canDelete,
                                        onDeleteClicked = { sesViewModel.deleteSes(contentItem.audio.id) },
                                        isCommentSectionClickable = false,
                                        showPlayIcon = false // Oynat ikonu sadece bu ekranda gizlenir
                                    )
                                }

                                is ContentItem.VideoContent -> {
                                    val canDelete =
                                        currentUserId == contentItem.video.ownerId || userRole == "ADMIN"
                                    VideoCard(
                                        video = contentItem.video,
                                        navController = navController,
                                        canDelete = canDelete,
                                        onDeleteClicked = { videoViewModel.deleteVideo(contentItem.video.id) },
                                        onClick = {
                                            navController.navigate(
                                                Constants.VIDEO_DETAIL_SCREEN.replace(
                                                    "{videoId}",
                                                    contentItem.video.id
                                                )
                                            )
                                        },
                                        isCommentSectionClickable = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamBottomSheetContent(
    scope: CoroutineScope,
    bottomSheetState: SheetState,
    liveStreamViewModel: LiveStreamViewModel
) {
    val currentStream by liveStreamViewModel.currentLiveStream.collectAsState()
    var showUnderConstructionDialog by rememberSaveable { mutableStateOf(false) }

    if (showUnderConstructionDialog) {
        AlertDialog(
            onDismissRequest = { showUnderConstructionDialog = false },
            title = { Text(text = "Yapım Aşamasında") },
            text = { Text("Bu özellik şu anda geliştirilmektedir. Lütfen daha sonra tekrar deneyin.") },
            confirmButton = {
                TextButton(onClick = { showUnderConstructionDialog = false }) {
                    Text("Geri dönmek için tıkla")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 250.dp)
            .background(Color(0xFFF3E5F5))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Uygulama Kürsüsü", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = {
                    scope.launch {
                        bottomSheetState.partialExpand()
                    }
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Kapat")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (currentStream != null) {
                val stream = currentStream!!
                Text(
                    "Şu An Sahnede:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                LiveStreamCard(stream = stream, onClick = { /* TODO: Canlı yayın detayına git */ })
                Spacer(Modifier.height(8.dp))
                if (!stream.description.isNullOrBlank()) {
                    Text(
                        stream.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        "Yayın için özel bir açıklama bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 4.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Filled.Podcasts,
                    contentDescription = "Aktif yayın yok",
                    modifier = Modifier
                        .size(60.dp)
                        .padding(vertical = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    "Şu anda sahnede aktif bir yayın bulunmuyor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bir sonraki yayın için takipte kalın!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { showUnderConstructionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text("Sahneye Çıkmak İçin Talepte Bulun")
            }
        }
    }
}

@Composable
fun LiveStreamCard(stream: LiveStream, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stream.title.takeIf { it.isNotBlank() } ?: "Başlıksız Yayın",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Yayıncı",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stream.streamerName.takeIf { it.isNotBlank() } ?: "Bilinmeyen Yayıncı",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Podcasts,
                    contentDescription = "Canlı Yayın Aktif",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Red
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "CANLI",
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.Red),
                    )
                    stream.startTime?.let { date ->
                        Text(
                            text = "Başlangıç: ${
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    .format(date)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            if (!stream.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stream.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
