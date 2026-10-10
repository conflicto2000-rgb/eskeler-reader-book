package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple

@Composable
fun WaveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.5.dp,
    maxHeight: Dp = 28.dp,
    gradientColors: List<Color> = listOf(ElectricCyan, ElectricPurple)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    // Generate different phase animations
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "anim3"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pattern = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.3f, 0.95f, 0.6f, 0.4f, 0.85f, 0.5f, 0.75f)

        for (i in 0 until barCount) {
            val baseScale = pattern[i % pattern.size]
            val animFactor = when (i % 3) {
                0 -> anim1
                1 -> anim2
                else -> anim3
            }
            val heightFraction = if (isPlaying) {
                (baseScale * animFactor).coerceIn(0.12f, 1.0f)
            } else {
                (baseScale * 0.3f).coerceIn(0.15f, 0.4f)
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(gradientColors))
            )
        }
    }
}
