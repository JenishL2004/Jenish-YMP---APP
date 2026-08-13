package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AbnormalityEntity
import com.example.data.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed

@Composable
fun AbnormalityTrackerScreen(
    user: UserEntity,
    abnormalities: List<AbnormalityEntity>,
    onUpdateAbnormality: (
        abnormality: AbnormalityEntity,
        newStatus: String,
        correctiveAction: String,
        rootCause: String,
        responsiblePerson: String,
        priority: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedAbnormalityForEdit by remember { mutableStateOf<AbnormalityEntity?>(null) }

    val filteredList = abnormalities.filter { item ->
        when (selectedFilter) {
            "PENDING" -> item.status == "PENDING"
            "IN_PROGRESS" -> item.status == "IN_PROGRESS"
            "RESOLVED" -> item.status == "RESOLVED"
            "VERIFIED" -> item.status == "VERIFIED"
            "CRITICAL" -> item.priority == "CRITICAL"
            else -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Chips Bar
        Text(
            text = "Abnormality & RCA Corrective Action Tracker",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("ALL", "PENDING", "IN_PROGRESS", "RESOLVED", "VERIFIED", "CRITICAL")
            items(filters) { f ->
                FilterChip(
                    selected = selectedFilter == f,
                    onClick = { selectedFilter = f },
                    label = { Text(f.replace("_", " ")) },
                    modifier = Modifier.testTag("filter_$f")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No Abnormalities",
                        tint = StatusNormal,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No abnormalities matching filter '$selectedFilter'",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredList) { item ->
                    AbnormalityCard(
                        item = item,
                        currentUser = user,
                        onEditClick = { selectedAbnormalityForEdit = item }
                    )
                }
            }
        }
    }

    // Edit & Action Dialog
    selectedAbnormalityForEdit?.let { abnormality ->
        EditAbnormalityDialog(
            abnormality = abnormality,
            onDismiss = { selectedAbnormalityForEdit = null },
            onSave = { newStatus, correctiveAction, rootCause, responsiblePerson, priority ->
                onUpdateAbnormality(abnormality, newStatus, correctiveAction, rootCause, responsiblePerson, priority)
                selectedAbnormalityForEdit = null
            }
        )
    }
}

@Composable
fun AbnormalityCard(
    item: AbnormalityEntity,
    currentUser: UserEntity,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.priority == "CRITICAL") StatusAbnormal else StatusWarning
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${item.machineName} (${item.lineName})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Ticket #${item.id} | Reported by: ${item.reportedBy}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Checkpoint: ${item.checkpointName}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            if (!item.photoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AsyncImage(
                    model = item.photoUri,
                    contentDescription = "Abnormality Evidence",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Root Cause & Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "RCA Root Cause:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = YamahaBlue)
                    )
                    Text(
                        text = item.rootCause,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Corrective Action Plan:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusNormal)
                    )
                    Text(
                        text = item.correctiveAction,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Responsible: ${item.responsiblePerson}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                    Text(
                        text = "Target Resolution: ${item.targetDate}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = YamahaRed)
                    )
                }

                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("action_edit_abnormality_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Action",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Action RCA", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun EditAbnormalityDialog(
    abnormality: AbnormalityEntity,
    onDismiss: () -> Unit,
    onSave: (
        newStatus: String,
        correctiveAction: String,
        rootCause: String,
        responsiblePerson: String,
        priority: String
    ) -> Unit
) {
    var status by remember { mutableStateOf(abnormality.status) }
    var priority by remember { mutableStateOf(abnormality.priority) }
    var rootCause by remember { mutableStateOf(abnormality.rootCause) }
    var correctiveAction by remember { mutableStateOf(abnormality.correctiveAction) }
    var responsiblePerson by remember { mutableStateOf(abnormality.responsiblePerson) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Update Abnormality Ticket #${abnormality.id}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Machine: ${abnormality.machineName}", style = MaterialTheme.typography.bodySmall)

                // Status Selector Chips
                Text(text = "Update Status:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PENDING", "IN_PROGRESS", "RESOLVED", "VERIFIED").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                // RCA Field
                OutlinedTextField(
                    value = rootCause,
                    onValueChange = { rootCause = it },
                    label = { Text("Root Cause Analysis (RCA)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Corrective Action Field
                OutlinedTextField(
                    value = correctiveAction,
                    onValueChange = { correctiveAction = it },
                    label = { Text("Corrective Action Executed") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Responsible Engineer
                OutlinedTextField(
                    value = responsiblePerson,
                    onValueChange = { responsiblePerson = it },
                    label = { Text("Responsible Engineer / Person") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(status, correctiveAction, rootCause, responsiblePerson, priority) },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) {
                Text("Save RCA Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
