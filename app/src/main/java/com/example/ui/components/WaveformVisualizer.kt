package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isMuted: Boolean = false,
    barCount: Int = 9,
    maxHeight: Dp = 48.dp,
    modifier: Modifier = Modifier,
    barColor: Color = CyberCyan
) {
    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val multipliers = listOf(0.3f, 0.6f, 0.85f, 1.0f, 0.9f, 0.7f, 0.95f, 0.5f, 0.4f)

        for (i in 0 until barCount) {
            val mult = multipliers.getOrElse(i) { 0.5f }
            val baseAmp = if (isMuted) 0.05f else (amplitude * mult).coerceIn(0.08f, 1.0f)

            val anim = remember { Animatable(0.1f) }
            LaunchedEffect(baseAmp) {
                anim.animateTo(
                    targetValue = baseAmp,
                    animationSpec = tween(durationMillis = 80, easing = LinearEasing)
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(anim.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i % 2 == 0) barColor else ElectricViolet
                    )
            )
        }
    }
}
