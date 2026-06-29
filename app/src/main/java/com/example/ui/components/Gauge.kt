package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.PowerGreen
import com.example.ui.theme.PowerRed
import com.example.ui.theme.PowerYellow
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc800

@Composable
fun BatteryGauge(
    soc: Int,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        soc > 60 -> Emerald500
        soc > 30 -> PowerYellow
        else -> PowerRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow Effect
        Box(
            modifier = Modifier
                .size(192.dp)
                .blur(60.dp)
                .background(Emerald500.copy(alpha = 0.1f), CircleShape)
        )

        // Gauge Container
        Box(
            modifier = Modifier.size(192.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val backgroundStroke = 4.dp.toPx()
                val foregroundStroke = 5.dp.toPx()
                val sizeRadius = size.minDimension / 2 - foregroundStroke / 2
                val topLeft = Offset(center.x - sizeRadius, center.y - sizeRadius)
                val arcSize = Size(sizeRadius * 2, sizeRadius * 2)

                // Background circle
                drawArc(
                    color = Zinc800,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = backgroundStroke)
                )

                // Foreground arc
                val sweepAngle = (soc / 100f) * 360f
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = foregroundStroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        append("$soc")
                        withStyle(style = SpanStyle(fontSize = 24.sp, color = Emerald500)) {
                            append("%")
                        }
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 48.sp,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "SOC CAPACITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Zinc400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
