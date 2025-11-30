package com.neval.anoba.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neval.anoba.common.utils.Constants

@Suppress("unused")
@Composable
fun NavBarControls(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp) // ✅ Daha dar bir yükseklik
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // ✅ İkonları dikeyde ortala
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Ana Sayfa",
            modifier = Modifier
                .size(20.dp) // ✅ ikon boyutu
                .clickable { navController.navigate(Constants.HOME_SCREEN) }
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Geri Dön",
            modifier = Modifier
                .size(20.dp)
                .clickable { navController.popBackStack() }
        )
    }
}
