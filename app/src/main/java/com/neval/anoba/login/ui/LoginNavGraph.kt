package com.neval.anoba.login.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.login.viewmodel.LoginViewModel
import com.neval.anoba.login.viewmodel.SignUpViewModel
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.loginNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Constants.SPLASH_SCREEN,
        route = Constants.LOGIN_NAV_GRAPH
    ) {
        composable(route = Constants.SPLASH_SCREEN) {
            SplashScreen(navController = navController)
        }
        composable(route = Constants.LOGIN_SCREEN) {
            LoginScreen(navController = navController, viewModel = koinViewModel<LoginViewModel>())
        }
        composable(route = Constants.SIGN_UP_SCREEN) {
            SignUpScreen(
                navController = navController,
                viewModel = koinViewModel<SignUpViewModel>()
            )
        }
        composable(route = Constants.FORGOT_PASSWORD_SCREEN) {
            ForgotPasswordScreen(navController = navController)
        }
        composable(route = Constants.NEW_PASSWORD_SCREEN) {
            NewPasswordScreen(navController = navController)
        }
    }
}
