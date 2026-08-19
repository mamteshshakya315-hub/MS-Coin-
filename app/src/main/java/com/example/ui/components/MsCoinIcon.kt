package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom 3D-styled MS Coin with "MS" embossed in the center.
 */
@Composable
fun MsCoinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    elevation: Dp = 2.dp
) {
    val outerGoldBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFFFD54F), // Light Gold
            Color(0xFFFFB300), // Amber Gold
            Color(0xFFFF8F00), // Deep Gold
            Color(0xFFFFE082), // Highlight Gold
            Color(0xFFFFB300), // Amber Gold
            Color(0xFFFFD54F)  // Light Gold
        )
    )

    val innerGoldBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE082), // Top shiny highlight
            Color(0xFFFFC107), // Vibrant Gold
            Color(0xFFFFA000)  // Base Amber
        )
    )

    // Calculate proportional font size based on size in dp
    val fontSizeSp = (size.value * 0.44f).sp

    Box(
        modifier = modifier
            .size(size)
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(outerGoldBrush)
            .border(width = maxOf(1.dp, (size.value * 0.05f).dp), color = Color(0xFFFFF8E1), shape = CircleShape)
            .border(width = maxOf(1.5.dp, (size.value * 0.08f).dp), color = Color(0xFFFF8F00), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner Coin Circle
        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
                .background(innerGoldBrush)
                .border(width = maxOf(0.8.dp, (size.value * 0.04f).dp), color = Color(0xFFFFD54F), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Shadow text behind for 3D embossed look
            Text(
                text = "MS",
                color = Color(0xFF5D4037).copy(alpha = 0.6f),
                fontSize = fontSizeSp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
            )
            // Foreground Crisp Text
            Text(
                text = "MS",
                color = Color(0xFF3E2723), // Deep brown/bronze for coin stamping
                fontSize = fontSizeSp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
