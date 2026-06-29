package com.example.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DataPoint
import com.example.ui.PowerStationViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc800

@Composable
fun AnalyticsScreen(viewModel: PowerStationViewModel) {
    val history by viewModel.history.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(top = 24.dp)
        )

        UptimeCard(uptimeSeconds)

        LineChartCard(
            title = "SOC vs Time",
            dataPoints = history,
            yValueSelector = { it.soc.toFloat() },
            lineColor = Emerald400,
            yLabel = "SOC %",
            yAxisMin = 0f,
            yAxisMax = 100f
        )

        LineChartCard(
            title = "Power vs Time",
            dataPoints = history,
            yValueSelector = { it.power },
            lineColor = Color(0xFFF59E0B),
            yLabel = "Watts"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun UptimeCard(uptimeSeconds: Long) {
    val hours = uptimeSeconds / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    val seconds = uptimeSeconds % 60
    
    val uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column {
            Text("RUNTIME (UPTIME)", color = Zinc400, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(uptimeStr, color = Emerald500, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun LineChartCard(
    title: String,
    dataPoints: List<DataPoint>,
    yValueSelector: (DataPoint) -> Float,
    lineColor: Color,
    yLabel: String,
    yAxisMin: Float? = null,
    yAxisMax: Float? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Zinc400, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold))
                Text(yLabel, color = Zinc400, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dataPoints.size < 2) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Waiting for more data...", color = Zinc800)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val yVals = dataPoints.map { yValueSelector(it) }
                    val minVal = yAxisMin ?: (yVals.minOrNull() ?: 0f)
                    val maxVal = yAxisMax ?: (yVals.maxOrNull()?.let { if(it == minVal) it + 1f else it } ?: 100f)
                    val valueRange = maxVal - minVal
                    
                    val path = Path()
                    val widthPerPoint = size.width / (dataPoints.size - 1)
                    
                    dataPoints.forEachIndexed { index, dataPoint ->
                        val y = yValueSelector(dataPoint)
                        // Invert Y because canvas Y goes down
                        val normalizedY = if (valueRange == 0f) 0.5f else (y - minVal) / valueRange
                        val canvasY = size.height - (normalizedY * size.height)
                        val canvasX = index * widthPerPoint
                        
                        if (index == 0) {
                            path.moveTo(canvasX, canvasY)
                        } else {
                            path.lineTo(canvasX, canvasY)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}
