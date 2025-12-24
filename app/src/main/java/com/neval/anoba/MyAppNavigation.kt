package com.neval.anoba

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.neval.anoba.chat.ui.chatNavGraph
import com.neval.anoba.common.utils.Constants
import com.neval.anoba.drawer.drawerNavGraph
import com.neval.anoba.letter.letterNavGraph
import com.neval.anoba.login.ui.loginNavGraph
import com.neval.anoba.photo.photoNavGraph
import com.neval.anoba.ses.sesNavGraph
import com.neval.anoba.video.videoNavGraph

@Composable
fun MyAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Constants.LOGIN_NAV_GRAPH,
        route = Constants.ROOT_GRAPH
    ) {
        loginNavGraph(navController)
        drawerNavGraph(navController)
        chatNavGraph(navController)
        letterNavGraph(navController)
        sesNavGraph(navController)
        photoNavGraph(navController)
        videoNavGraph(navController)
    }
}
