package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AbnormalityEntity
import com.example.data.PatrolLogEntity
import com.example.data.UserEntity
import com.example.ui.components.MachineStatusDonutChart
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.WeeklyPatrolBarChart
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.YamahaBlue
import com.example.ui.viewmodel.Screen

private fun getCurrentShiftKolkata(): String {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    val timeInMinutes = hour * 60 + minute

    return when {
        timeInMinutes in 420 until 945 -> "Shift A (07:00 - 15:45)"
        timeInMinutes >= 945 || timeInMinutes < 30 -> "Shift B (15:45 - 00:30)"
        else -> "Shift C (00:30 - 07:00)"
    }
}

private fun getCurrentDateTimeKolkata(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(java.util.Date())
}

@Composable
fun DashboardScreen(
    user: UserEntity,
    totalPatrols: Int,
    pendingAbnormalities: Int,
    criticalIssues: Int,
    operationalMachines: Int,
    totalMachines: Int,
    recentPatrols: List<PatrolLogEntity>,
    recentAbnormalities: List<AbnormalityEntity>,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shift & Plant Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = YamahaBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shift: ${getCurrentShiftKolkata()} | IST: ${getCurrentDateTimeKolkata()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = user.plant,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Department: ${user.department}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SYSTEM ONLINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Executive KPI Cards
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = "Today Patrols",
                    value = "$totalPatrols",
                    icon = Icons.Default.CheckCircle,
                    iconColor = StatusNormal,
                    subtitle = "100% Completed",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Abnormalities",
                    value = "$pendingAbnormalities",
                    icon = Icons.Default.Warning,
                    iconColor = StatusWarning,
                    subtitle = "$criticalIssues Critical",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val machineHealthPct = if (totalMachines > 0) (operationalMachines * 100 / totalMachines) else 100
                StatCard(
                    title = "Machine Health",
                    value = "$machineHealthPct%",
                    icon = Icons.Default.Build,
                    iconColor = StatusInfo,
                    subtitle = "$operationalMachines / $totalMachines Operational",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Critical Alerts",
                    value = "$criticalIssues",
                    icon = Icons.Default.AddAlert,
                    iconColor = StatusAbnormal,
                    subtitle = "Requires RCA",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Navigation Menu Grid
        item {
            Text(
                text = "Module Portal Access",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ActionTile(
                        title = "Start Patrol",
                        subtitle = "Execute Checkpoint",
                        icon = Icons.Default.PlayArrow,
                        color = StatusNormal,
                        onClick = { onNavigate(Screen.PatrolExecution) },
                        testTag = "action_start_patrol"
                    )
                }
                item {
                    ActionTile(
                        title = "Abnormalities",
                        subtitle = "$pendingAbnormalities Action Required",
                        icon = Icons.Default.Warning,
                        color = StatusWarning,
                        onClick = { onNavigate(Screen.AbnormalityTracker) },
                        testTag = "action_abnormalities"
                    )
                }
                item {
                    ActionTile(
                        title = "Reports & Analytics",
                        subtitle = "Export PDF / CSV",
                        icon = Icons.Default.Assignment,
                        color = StatusInfo,
                        onClick = { onNavigate(Screen.Reports) },
                        testTag = "action_reports"
                    )
                }
                if (user.role == "ADMIN" || user.role == "SUPERVISOR") {
                    item {
                        ActionTile(
                            title = "Master Data",
                            subtitle = "Plants & Users",
                            icon = Icons.Default.Settings,
                            color = YamahaBlue,
                            onClick = { onNavigate(Screen.MasterData) },
                            testTag = "action_master_data"
                        )
                    }
                }
                if (user.role == "ADMIN") {
                    item {
                        ActionTile(
                            title = "Audit Logs",
                            subtitle = "Security Trail",
                            icon = Icons.Default.Security,
                            color = StatusAbnormal,
                            onClick = { onNavigate(Screen.AuditLogs) },
                            testTag = "action_audit_logs"
                        )
                    }
                }
            }
        }

        // Visual Analytics Charts
        item {
            MachineStatusDonutChart(
                operationalCount = operationalMachines,
                maintenanceCount = criticalIssues.coerceAtLeast(1),
                underPatrolCount = (totalMachines - operationalMachines - criticalIssues).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            WeeklyPatrolBarChart(
                data = listOf(
                    "Mon" to 12,
                    "Tue" to 14,
                    "Wed" to 18,
                    "Thu" to 15,
                    "Fri" to 20,
                    "Sat" to 16,
                    "Sun" to 8
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Activity Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Patrol Inspections",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelSmall.copy(color = YamahaBlue, fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { onNavigate(Screen.Reports) }
                )
            }
        }

        if (recentPatrols.isEmpty()) {
            item {
                Text(
                    text = "No patrol records found",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            items(recentPatrols.take(4)) { log ->
                PatrolLogCard(log = log)
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(150.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun PatrolLogCard(log: PatrolLogEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.machineName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = log.overallStatus)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.lineName} | Inspector: ${log.employeeName} (${log.employeeId})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )
            }
        }
    }
}
