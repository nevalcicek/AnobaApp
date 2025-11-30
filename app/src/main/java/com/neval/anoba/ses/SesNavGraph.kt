package com.neval.anoba.ses

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.ses.sescomment.SesCommentsScreen

fun NavGraphBuilder.sesNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Constants.SES_HOME_SCREEN,
        route = Constants.SES_NAV_GRAPH
    ) {
        composable(Constants.SES_HOME_SCREEN) {
            SesHomeScreen(navController = navController)
        }

        composable(
            route = Constants.SES_DETAIL_SCREEN,
            arguments = listOf(navArgument("sesId") {
                type = NavType.StringType
                nullable = false
            })
        ) { backStackEntry ->
            val sesId = backStackEntry.arguments?.getString("sesId")
            if (!sesId.isNullOrBlank()) {
                SesDetailScreen(
                    sesId = sesId,
                    navController = navController
                )
            } else {
                // Eğer sesId yoksa, bir önceki ekrana geri dön.
                navController.popBackStack()
            }
        }

        composable(
            route = Constants.SES_COMMENTS_ROUTE,
            arguments = listOf(navArgument("sesId") {
                type = NavType.StringType
                nullable = false
            })
        ) { backStackEntry ->
            val sesId = backStackEntry.arguments?.getString("sesId")
            if (!sesId.isNullOrBlank()) {
                SesCommentsScreen(
                    navController = navController,
                    sesId = sesId
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
