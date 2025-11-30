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
        route = Constants.PHOTO_NAV_GRAPH,
        startDestination = Constants.PHOTO_HOME_SCREEN
    ) {
        // Ana fotoğraf listeleme ekranı
        composable(Constants.PHOTO_HOME_SCREEN) {
            PhotoHomeScreen(navController = navController)
        }

        // Yeni fotoğraf oluşturma ekranı
        composable(Constants.PHOTO_CREATE_SCREEN) {
            PhotoCreateScreen(navController = navController)
        }

        // Bir fotoğrafa tıklandığında açılacak detay ekranı.
        composable(
            route = Constants.PHOTO_DETAIL_SCREEN,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")
            if (photoId != null) {
                PhotoDetailScreen(
                    navController = navController,
                    photoId = photoId
                )
            } else {
                navController.popBackStack()
            }
        }

        // Fotoğraf yorumları ekranı.
        composable(
            route = Constants.PHOTO_COMMENTS_ROUTE,
            arguments = listOf(navArgument("photoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")
            if (photoId != null) {
                PhotoCommentsScreen(
                    navController = navController,
                    photoId = photoId
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
