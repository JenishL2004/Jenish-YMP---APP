package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.ui.theme.IndustrialSteel
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed

@Composable
fun YamahaTopHeader(
    title: String,
    user: UserEntity?,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = YamahaBlue,
        contentColor = Color.White,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(YamahaRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Yamaha Emblem",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "YAMAHA PATROL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = YamahaRed,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            if (user != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = user.employeeName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (user.role) {
                                            "ADMIN" -> YamahaRed
                                            "SUPERVISOR" -> StatusInfo
                                            else -> StatusNormal
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = user.role,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.employeeId,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier.testTag("sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync with Supabase",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = iconColor)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "NORMAL", "OPERATIONAL", "VERIFIED", "ACTIVE" -> StatusNormal to Color.White
        "ABNORMAL", "CRITICAL", "MAINTENANCE REQUIRED" -> StatusAbnormal to Color.White
        "PENDING", "HIGH" -> StatusWarning to Color.Black
        "IN_PROGRESS", "MEDIUM", "UNDER PATROL" -> StatusInfo to Color.White
        else -> IndustrialSteel to Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun MachineStatusDonutChart(
    operationalCount: Int,
    maintenanceCount: Int,
    underPatrolCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (operationalCount + maintenanceCount + underPatrolCount).coerceAtLeast(1)
    val opAngle = (operationalCount.toFloat() / total) * 360f
    val maintAngle = (maintenanceCount.toFloat() / total) * 360f
    val patrolAngle = (underPatrolCount.toFloat() / total) * 360f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Machine Health Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        var startAngle = -90f
                        val stroke = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)

                        // Operational segment
                        drawArc(
                            color = StatusNormal,
                            startAngle = startAngle,
                            sweepAngle = opAngle,
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += opAngle

                        // Maintenance segment
                        drawArc(
                            color = StatusAbnormal,
                            startAngle = startAngle,
                            sweepAngle = maintAngle,
                            useCenter = false,
                            style = stroke
                        )
                        startAngle += maintAngle

                        // Under Patrol segment
                        drawArc(
                            color = StatusWarning,
                            startAngle = startAngle,
                            sweepAngle = patrolAngle,
                            useCenter = false,
                            style = stroke
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Machines",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = StatusNormal, label = "Operational ($operationalCount)")
                    LegendItem(color = StatusAbnormal, label = "Maintenance ($maintenanceCount)")
                    LegendItem(color = StatusWarning, label = "Under Patrol ($underPatrolCount)")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
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
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
        )
    }
}

@Composable
fun WeeklyPatrolBarChart(
    data: List<Pair<String, Int>>, // Day -> Count
    modifier: Modifier = Modifier
) {
    val maxCount = (data.maxOfOrNull { it.second } ?: 10).coerceAtLeast(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Patrol Volume (Shift A/B/C)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (day, count) ->
                    val barHeightFraction = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(YamahaBlue)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
