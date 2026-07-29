package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.DayVolume
import com.example.ui.theme.*

@Composable
fun CallVolumeBarChart(
    dailyVolume: List<DayVolume>,
    modifier: Modifier = Modifier
) {
    if (dailyVolume.isEmpty()) return

    val maxCount = remember(dailyVolume) {
        dailyVolume.maxOfOrNull { it.callCount }?.coerceAtLeast(5) ?: 5
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    val progress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "chartAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Call Volume Analytics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HighUrgencyRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Urgent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height - 30.dp.toPx()
                    val barGroupWidth = width / dailyVolume.size
                    val barWidth = barGroupWidth * 0.45f

                    dailyVolume.forEachIndexed { i, day ->
                        val x = i * barGroupWidth + (barGroupWidth - barWidth) / 2
                        val barHeight = (day.callCount.toFloat() / maxCount) * height * progress
                        val urgentHeight = (day.urgentCount.toFloat() / maxCount) * height * progress
                        val y = height - barHeight

                        // Background track grid
                        drawRoundRect(
                            color = SlateSurfaceVariantDark.copy(alpha = 0.3f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Main call bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(BlueAccent, IndigoPrimary)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Urgent overlay bar
                        if (day.urgentCount > 0) {
                            val urgentY = height - urgentHeight
                            drawRoundRect(
                                color = HighUrgencyRed,
                                topLeft = Offset(x, urgentY),
                                size = Size(barWidth, urgentHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }

                // X-Axis Day Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dailyVolume.forEach { day ->
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UrgencyDistributionChart(
    urgencyDistribution: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = urgencyDistribution.values.sum().coerceAtLeast(1)
    val high = urgencyDistribution["HIGH"] ?: 0
    val medium = urgencyDistribution["MEDIUM"] ?: 0
    val low = urgencyDistribution["LOW"] ?: 0

    val highAngle = (high.toFloat() / total) * 360f
    val mediumAngle = (medium.toFloat() / total) * 360f
    val lowAngle = (low.toFloat() / total) * 360f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Urgency Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 18.dp.toPx()
                        var startAngle = -90f

                        // High urgency segment
                        if (highAngle > 0) {
                            drawArc(
                                color = HighUrgencyRed,
                                startAngle = startAngle,
                                sweepAngle = highAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += highAngle
                        }

                        // Medium urgency segment
                        if (mediumAngle > 0) {
                            drawArc(
                                color = MediumUrgencyAmber,
                                startAngle = startAngle,
                                sweepAngle = mediumAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += mediumAngle
                        }

                        // Low urgency segment
                        if (lowAngle > 0) {
                            drawArc(
                                color = LowUrgencyGreen,
                                startAngle = startAngle,
                                sweepAngle = lowAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }

                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UrgencyLegendItem("High Priority", high, HighUrgencyRed, total)
                    UrgencyLegendItem("Medium Priority", medium, MediumUrgencyAmber, total)
                    UrgencyLegendItem("Low / Routine", low, LowUrgencyGreen, total)
                }
            }
        }
    }
}

@Composable
private fun UrgencyLegendItem(
    label: String,
    count: Int,
    color: Color,
    total: Int
) {
    val pct = (count.toFloat() / total * 100).toInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "$count ($pct%)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
