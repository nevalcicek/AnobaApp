package com.neval.anoba.photo

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.photo.photocomment.PhotoCommentsScreen

fun NavGraphBuilder.photoNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Constants.PHOTO_HOME_SCREEN,
        route = Constants.PHOTO_NAV_GRAPH
    ) {
        composable(Constants.PHOTO_HOME_SCREEN) {
            PhotoHomeScreen(navController = navController)
        }
        composable(
            route = "${Constants.PHOTO_DETAIL_SCREEN}/{photoId}",
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoDetailScreen(navController = navController, photoId = photoId)
        }
        composable(
            route = Constants.PHOTO_COMMENTS_ROUTE,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry -> // Hata Düzeltildi
            val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
            PhotoCommentsScreen(navController = navController, photoId = photoId)
        }
        composable(Constants.PHOTO_GALLERY_SCREEN) {
            PhotoGalleryScreen(navController = navController)
        }
    }
}
