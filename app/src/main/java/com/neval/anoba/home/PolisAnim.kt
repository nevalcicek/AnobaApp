package com.neval.anoba.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun PolisAnim(
    imageRes: Int,
    maxWidth: Dp,
    maxHeight: Dp,
    startDelay: Long,
    animationDuration: Long,
    onFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    // Başlangıç noktası (sağ üst köşe - karakol kartı)
    val startX = maxWidth - 60.dp
    val startY = 16.dp

    // Hedef belirleme mantığını "akıllı" hale getiriyoruz.
    val (targetX, targetY) = remember(maxWidth, maxHeight) {
        // Her polis için rastgele bir "varış stratejisi" seçilir.
        when (Random.nextInt(4)) {
            // Strateji 0: Ekranın sol tarafına doğru git ve dışarı çık.
            0 -> (0.dp - 40.dp) to (maxHeight.value * (0.2f + Random.nextFloat() * 0.7f)).dp

            // Strateji 1: Ekranın alt tarafına doğru git ve dışarı çık.
            1 -> (maxWidth.value * (0.1f + Random.nextFloat() * 0.8f)).dp to (maxHeight + 40.dp)

            // Strateji 2: Ana içerik akışının olduğu orta/alt bölgelere git.
            2 -> (maxWidth.value * (Random.nextFloat() * 0.9f)).dp to (maxHeight.value * (0.5f + Random.nextFloat() * 0.5f)).dp

            // Strateji 3 (Varsayılan): Hızlı erişim butonlarının olduğu sol üst bölgeye git.
            else -> (maxWidth.value * (0.1f + Random.nextFloat() * 0.5f)).dp to (maxHeight.value * (0.15f + Random.nextFloat() * 0.3f)).dp
        }
    }

    // X konumu animasyonu
    val currentX by animateDpAsState(
        targetValue = if (isVisible) targetX else startX,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "x_offset"
    )

    // Y konumu animasyonu
    val currentY by animateDpAsState(
        targetValue = if (isVisible) targetY else startY,
        animationSpec = tween(durationMillis = animationDuration.toInt()),
        label = "y_offset"
    )

    // Görselin şeffaflık (fade) animasyonu
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 0.85f else 0f,
        animationSpec = tween(durationMillis = (animationDuration / 2).toInt()),
        label = "alpha_fade"
    )

    // Animasyonun başlatılması
    LaunchedEffect(Unit) {
        delay(startDelay)
        isVisible = true
        delay(animationDuration)
        onFinished()
    }

    // Görseli ekrana çiz
    Box(
        modifier = Modifier
            .offset(x = currentX, y = currentY)
            .size(40.dp)
            .alpha(alpha)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Polis",
            contentScale = ContentScale.Fit
        )
    }
}
