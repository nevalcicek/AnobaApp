package com.neval.anoba.drawer

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.home.HomeScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.drawerNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Constants.HOME_SCREEN,
        route = Constants.DRAWER_GRAPH
    ) {
        composable(Constants.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                liveStreamViewModel = koinViewModel()
            )
        }
        composable(Constants.PROFILE_SCREEN) {
            ProfileScreen(navController)
        }
        composable(Constants.PROFILE_EDIT_SCREEN) {
            ProfileEditScreen(navController)
        }
    }
}
