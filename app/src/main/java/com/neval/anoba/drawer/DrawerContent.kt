package com.neval.anoba.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.neval.anoba.R
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.common.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = koinViewModel()

    // DİNAMİK VERİ: ViewModel'den kullanıcı adı ve profil resmi URL'si alınıyor.
    val userName by authViewModel.userName.collectAsState()
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsUnderConstructionDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Çıkış Yap") },
            text = { Text("Oturumu kapatmak istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        scope.launch {
                            drawerState.close()
                            authViewModel.logout {
                                navController.navigate(Constants.LOGIN_NAV_GRAPH) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                ) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Hayır")
                }
            }
        )
    }

    if (showSettingsUnderConstructionDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsUnderConstructionDialog = false },
            title = { Text(text = "Yapım Aşamasında") },
            text = { Text("Bu özellik şu anda geliştirilmektedir. Lütfen daha sonra tekrar deneyin.") },
            confirmButton = {
                TextButton(onClick = { showSettingsUnderConstructionDialog = false }) {
                    Text("Geri dön")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Constants.PROFILE_SCREEN)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profil Resmi",
                                placeholder = painterResource(id = R.drawable.ic_default_profile),
                                error = painterResource(id = R.drawable.ic_default_profile),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Çekmeceyi Kapat")
                        }
                    }

                    HorizontalDivider()

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        NavigationDrawerItem(
                            label = { Text("Profilim") },
                            selected = navController.currentDestination?.route == Constants.PROFILE_SCREEN,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Constants.PROFILE_SCREEN)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Bildirimlerim") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsUnderConstructionDialog = true
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Kaydettiklerim") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsUnderConstructionDialog = true
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Uygulama Ayarları") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsUnderConstructionDialog = true
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("Uygulamadan Çık") },
                        selected = false,
                        onClick = {
                            showLogoutConfirmDialog = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        content = content
    )
}
