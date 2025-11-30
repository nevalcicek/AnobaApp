package com.neval.anoba.chat.ui

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.neval.anoba.chat.general.ChatHomeScreen
import com.neval.anoba.chat.group.GroupChatListScreen
import com.neval.anoba.chat.group.GroupChatScreen
import com.neval.anoba.chat.group.GroupChatViewModel
import com.neval.anoba.chat.group.GroupInfoScreen
import com.neval.anoba.chat.privatechat.PrivateChatScreen
import com.neval.anoba.chat.privatechat.PrivateChatViewModel
import com.neval.anoba.common.utils.Constants
import org.koin.androidx.compose.koinViewModel

const val GROUP_CHAT_FLOW_ROUTE = "group_chat_flow"

fun NavGraphBuilder.chatNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Constants.CHAT_HOME_SCREEN,
        route = Constants.CHAT_NAV_GRAPH
    ) {

        composable(Constants.CHAT_HOME_SCREEN) {
            ChatHomeScreen(
                navController = navController,
                chatViewModel = koinViewModel()
            )
        }

        composable(Constants.PRIVATE_CHAT_SCREEN) {
            val privateChatViewModel: PrivateChatViewModel = koinViewModel()
            val userId = privateChatViewModel.currentUserId

            if (!userId.isNullOrBlank()) {
                PrivateChatScreen(
                    navController = navController,
                    privateChatViewModel = privateChatViewModel
                )
            } else {
                Log.w("NavGraphError", "Kullanıcı oturumu yok (PrivateChatScreen). Giriş ekranına yönlendiriliyor.")
                LaunchedEffect(Unit) {
                    navController.navigate(Constants.LOGIN_SCREEN) {
                        popUpTo(Constants.CHAT_NAV_GRAPH) { inclusive = true }
                    }
                }
            }
        }

        navigation(
            startDestination = Constants.GROUP_CHAT_LIST_SCREEN,
            route = GROUP_CHAT_FLOW_ROUTE
        ) {
            composable(Constants.GROUP_CHAT_LIST_SCREEN) {
                val parentEntry = remember(it) { navController.getBackStackEntry(GROUP_CHAT_FLOW_ROUTE) }
                val groupChatViewModel: GroupChatViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
                GroupChatListScreen(
                    navController = navController,
                    groupChatViewModel = groupChatViewModel
                )
            }

            composable(
                route = "${Constants.GROUP_CHAT_SCREEN}/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                if (groupId != null) {
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(GROUP_CHAT_FLOW_ROUTE) }
                    val groupChatViewModel: GroupChatViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
                    GroupChatScreen(
                        navController = navController,
                        groupChatViewModel = groupChatViewModel,
                        groupId = groupId
                    )
                } else {
                    Log.e("NavGraphError", "GroupChatScreen'e groupId olmadan ulaşıldı.")
                    navController.popBackStack()
                }
            }

            composable(
                route = "${Constants.GROUP_INFO_SCREEN}/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                if (groupId != null) {
                    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(GROUP_CHAT_FLOW_ROUTE) }
                    val groupChatViewModel: GroupChatViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
                    GroupInfoScreen(
                        navController = navController,
                        groupChatViewModel = groupChatViewModel,
                        groupId = groupId,
                        currentUserId = groupChatViewModel.currentUserId
                    )
                } else {
                    Log.e("NavGraphError", "GroupInfoScreen'e groupId olmadan ulaşıldı.")
                    navController.popBackStack()
                }
            }
        }
    }
}
