package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.LineEntity
import com.example.data.MachineEntity
import com.example.data.PatrolPointEntity
import com.example.data.ShopEntity
import com.example.data.UserEntity
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed

private fun calculateAsiaKolkataShift(): String {
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

@Composable
fun PatrolExecutionScreen(
    user: UserEntity,
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    machines: List<MachineEntity>,
    points: List<PatrolPointEntity>,
    onSubmitPatrolWithPhotos: (
        shopName: String,
        lineName: String,
        machineName: String,
        machineId: Int,
        shift: String,
        notes: String,
        results: List<Triple<PatrolPointEntity, Pair<String, String>, Triple<String, String, Triple<String, String?, String?>>>>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sequential Step Index: 0 (Shop) -> 1 (Line) -> 2 (Machine) -> 3 (Patrol Points)
    var stepIndex by remember { mutableStateOf(0) }

    var selectedShop by remember { mutableStateOf(shops.firstOrNull()) }
    var selectedLine by remember { mutableStateOf(lines.firstOrNull()) }
    var selectedMachine by remember { mutableStateOf(machines.firstOrNull()) }

    var selectedShift by remember { mutableStateOf(calculateAsiaKolkataShift()) }
    var patrolNotes by remember { mutableStateOf("") }

    // Checkpoint States: Point.id -> Pair(Status: "NORMAL" | "ABNORMAL" | "N/A", Remarks)
    val checkpointState = remember { mutableStateMapOf<Int, Pair<String, String>>() }
    // Checkpoint Detail: Point.id -> Triple(ProblemDescription, Severity, Triple(Countermeasure, PhotoUri?, Category?))
    val checkpointDetail = remember { mutableStateMapOf<Int, Triple<String, String, Triple<String, String?, String?>>>() }

    // Filter points belonging to selected machine
    val targetPoints = remember(selectedMachine, points) {
        if (selectedMachine == null) points else points.filter { it.machineId == selectedMachine?.id || it.machineName == selectedMachine?.machineName }
    }

    // Initialize checkpoint states
    targetPoints.forEach { pt ->
        if (!checkpointState.containsKey(pt.id)) {
            checkpointState[pt.id] = "NORMAL" to "Inspected & OK"
        }
        if (!checkpointDetail.containsKey(pt.id)) {
            checkpointDetail[pt.id] = Triple("", "MAJOR", Triple("", null, pt.category))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stepper Banner Header
        Card(
            colors = CardDefaults.cardColors(containerColor = YamahaBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAINTENANCE PATROL",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "STEP ${stepIndex + 1} OF 4",
                        style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Black)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val stepTitles = listOf("Select Shop", "Select Line", "Select Machine", "Check Points")
                Text(
                    text = stepTitles[stepIndex],
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i <= stepIndex) YamahaRed else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // Sequential Step Screens
        Box(modifier = Modifier.weight(1f)) {
            when (stepIndex) {
                0 -> StepShopSelection(
                    shops = shops,
                    selectedShop = selectedShop,
                    onSelect = {
                        selectedShop = it
                        selectedLine = lines.firstOrNull { l -> l.shopId == it.id }
                        stepIndex = 1
                    }
                )
                1 -> StepLineSelection(
                    lines = lines.filter { selectedShop == null || it.shopId == selectedShop?.id },
                    selectedLine = selectedLine,
                    onSelect = {
                        selectedLine = it
                        selectedMachine = machines.firstOrNull { m -> m.lineId == it.id }
                        stepIndex = 2
                    }
                )
                2 -> StepMachineSelection(
                    machines = machines.filter { selectedLine == null || it.lineId == selectedLine?.id },
                    selectedMachine = selectedMachine,
                    onSelect = {
                        selectedMachine = it
                        stepIndex = 3
                    }
                )
                3 -> StepCheckpointsExecution(
                    shop = selectedShop?.shopName ?: "Weld Shop",
                    line = selectedLine?.lineName ?: "Frame Line 1",
                    machine = selectedMachine?.machineName ?: "OTC Daihen Robot FD-V8",
                    machineId = selectedMachine?.id ?: 1,
                    shift = selectedShift,
                    points = targetPoints,
                    checkpointState = checkpointState,
                    checkpointDetail = checkpointDetail,
                    patrolNotes = patrolNotes,
                    onNotesChange = { patrolNotes = it },
                    onSubmit = {
                        val tripleList = targetPoints.map { pt ->
                            Triple(
                                pt,
                                checkpointState[pt.id] ?: ("NORMAL" to ""),
                                checkpointDetail[pt.id] ?: Triple("", "MAJOR", Triple("", null, pt.category))
                            )
                        }
                        onSubmitPatrolWithPhotos(
                            selectedShop?.shopName ?: "Weld Shop",
                            selectedLine?.lineName ?: "Frame Line 1",
                            selectedMachine?.machineName ?: "OTC Daihen Robot FD-V8",
                            selectedMachine?.id ?: 1,
                            selectedShift,
                            patrolNotes.ifBlank { "Maintenance patrol round completed." },
                            tripleList
                        )
                    }
                )
            }
        }

        // Stepper Navigation Controls
        if (stepIndex < 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (stepIndex > 0) {
                    OutlinedButton(
                        onClick = { stepIndex-- },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PREVIOUS")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = { stepIndex++ },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("NEXT STEP")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StepShopSelection(
    shops: List<ShopEntity>,
    selectedShop: ShopEntity?,
    onSelect: (ShopEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("1. Select Shop:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        shops.forEach { s ->
            val isSel = selectedShop?.id == s.id
            Card(
                onClick = { onSelect(s) },
                colors = CardDefaults.cardColors(containerColor = if (isSel) YamahaBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, YamahaBlue) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Business, contentDescription = s.shopName, tint = YamahaBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = s.shopName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    if (isSel) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Selected", tint = YamahaBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun StepLineSelection(
    lines: List<LineEntity>,
    selectedLine: LineEntity?,
    onSelect: (LineEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("2. Select Line:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        lines.forEach { l ->
            val isSel = selectedLine?.id == l.id
            Card(
                onClick = { onSelect(l) },
                colors = CardDefaults.cardColors(containerColor = if (isSel) YamahaBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, YamahaBlue) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = l.lineName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Shop: ${l.shopName}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    if (isSel) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Selected", tint = YamahaBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun StepMachineSelection(
    machines: List<MachineEntity>,
    selectedMachine: MachineEntity?,
    onSelect: (MachineEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("3. Select Machine:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(machines) { m ->
                val isSel = selectedMachine?.id == m.id
                Card(
                    onClick = { onSelect(m) },
                    colors = CardDefaults.cardColors(containerColor = if (isSel) YamahaBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                    border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, YamahaBlue) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PrecisionManufacturing, contentDescription = m.machineName, tint = YamahaBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = m.machineName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(text = "${m.manufacturer} ${m.model} | Type: ${m.machineType}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (isSel) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Selected", tint = YamahaBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCheckpointsExecution(
    shop: String,
    line: String,
    machine: String,
    machineId: Int,
    shift: String,
    points: List<PatrolPointEntity>,
    checkpointState: MutableMap<Int, Pair<String, String>>,
    checkpointDetail: MutableMap<Int, Triple<String, String, Triple<String, String?, String?>>>,
    patrolNotes: String,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    // Photo Attachment State
    var activePhotoPickerPointId by remember { mutableStateOf<Int?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            activePhotoPickerPointId?.let { pointId ->
                val currentDetail = checkpointDetail[pointId]
                val probDesc = currentDetail?.first ?: ""
                val severity = currentDetail?.second ?: "HIGH"
                val countermeasure = currentDetail?.third?.first ?: ""
                val category = currentDetail?.third?.third ?: "Welding"
                checkpointDetail[pointId] = Triple(probDesc, severity, Triple(countermeasure, selectedUri.toString(), category))
            }
        }
    }
    // Check if any abnormal checkpoint is missing required problem description or photo
    val abnormalCheckpoints = points.filter { pt ->
        val status = checkpointState[pt.id]?.first ?: "NORMAL"
        status == "ABNORMAL"
    }

    val missingPhotoOrDesc = abnormalCheckpoints.filter { pt ->
        val detail = checkpointDetail[pt.id]
        val probDesc = detail?.first.orEmpty()
        val photoUri = detail?.third?.second
        probDesc.isBlank() || photoUri.isNullOrBlank()
    }

    val normalCount = points.count { (checkpointState[it.id]?.first ?: "NORMAL") == "NORMAL" }
    val abnormalCount = points.count { (checkpointState[it.id]?.first ?: "NORMAL") == "ABNORMAL" }
    val naCount = points.count { (checkpointState[it.id]?.first ?: "NORMAL") == "N/A" }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Scope Header & Quick Action Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "4. Check Points for Machine:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                            Text(text = "$shop > $line > $machine", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = YamahaBlue))
                        }

                        Button(
                            onClick = {
                                points.forEach { pt ->
                                    checkpointState[pt.id] = "NORMAL" to "Inspected & OK"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormal),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = "ALL OK", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ALL OK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Warning banner if photo or description missing for abnormal points
        if (missingPhotoOrDesc.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusAbnormal.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = StatusAbnormal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${missingPhotoOrDesc.size} abnormal point(s) require Problem Description and Photo attachment before submission!",
                            style = MaterialTheme.typography.bodySmall.copy(color = StatusAbnormal, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // List of Patrol Checkpoints
        items(points, key = { it.id }) { pt ->
            val currentEval = checkpointState[pt.id] ?: ("NORMAL" to "Inspected & OK")
            val currentDetail = checkpointDetail[pt.id] ?: Triple("", "MAJOR", Triple("", null, pt.category))

            val currentStatus = currentEval.first
            val probDesc = currentDetail.first
            val severity = currentDetail.second
            val countermeasure = currentDetail.third.first
            val photoUri = currentDetail.third.second

            val isAbnormal = currentStatus == "ABNORMAL"

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAbnormal) StatusAbnormal.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isAbnormal && (probDesc.isBlank() || photoUri.isNullOrBlank())) androidx.compose.foundation.BorderStroke(2.dp, YamahaRed) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pt.pointName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Category: ${pt.category} | Standard: ${pt.standardValue}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        // Status Selection: NORMAL / ABNORMAL / N/A
                        Row {
                            listOf("NORMAL", "ABNORMAL", "N/A").forEach { st ->
                                val isSelected = currentStatus == st
                                val bg = when (st) {
                                    "NORMAL" -> if (isSelected) StatusNormal else StatusNormal.copy(alpha = 0.12f)
                                    "ABNORMAL" -> if (isSelected) StatusAbnormal else StatusAbnormal.copy(alpha = 0.12f)
                                    else -> if (isSelected) YamahaBlue else Color.LightGray.copy(alpha = 0.3f)
                                }
                                val textColor = if (isSelected) Color.White else when (st) {
                                    "NORMAL" -> StatusNormal
                                    "ABNORMAL" -> StatusAbnormal
                                    else -> Color.DarkGray
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .clickable {
                                            checkpointState[pt.id] = st to if (st == "NORMAL") "Inspected & OK" else "Issue identified"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = st,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isAbnormal) {
                        OutlinedTextField(
                            value = currentEval.second,
                            onValueChange = { checkpointState[pt.id] = currentStatus to it },
                            label = { Text("Inspection Remarks (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // ABNORMAL Expanded Required Input Fields
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = probDesc,
                                onValueChange = {
                                    checkpointDetail[pt.id] = Triple(it, severity, Triple(countermeasure, photoUri, pt.category))
                                },
                                label = { Text("Problem Description (Mandatory)") },
                                isError = probDesc.isBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Severity:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("CRITICAL", "MAJOR", "MINOR").forEach { sev ->
                                        FilterChip(
                                            selected = severity == sev,
                                            onClick = {
                                                checkpointDetail[pt.id] = Triple(probDesc, sev, Triple(countermeasure, photoUri, pt.category))
                                            },
                                            label = { Text(sev, fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = countermeasure,
                                onValueChange = {
                                    checkpointDetail[pt.id] = Triple(probDesc, severity, Triple(it, photoUri, pt.category))
                                },
                                label = { Text("Countermeasure / Immediate Action Taken") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Photo Attachment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        activePhotoPickerPointId = pt.id
                                        photoPickerLauncher.launch("image/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!photoUri.isNullOrBlank()) StatusNormal else YamahaRed
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = if (!photoUri.isNullOrBlank()) Icons.Default.Check else Icons.Default.AddAPhoto, contentDescription = "Photo", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (!photoUri.isNullOrBlank()) "Photo Attached (Tap to Change)" else "ATTACH EVIDENCE PHOTO",
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Abnormality Evidence Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Banner & Submit Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = YamahaBlue.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("PATROL SUMMARY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = YamahaBlue))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Checkpoints: ${points.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Normal: $normalCount", color = StatusNormal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Abnormal: $abnormalCount", color = StatusAbnormal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("N/A: $naCount", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = patrolNotes,
                onValueChange = onNotesChange,
                label = { Text("General Patrol Log Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSubmit,
                enabled = missingPhotoOrDesc.isEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = YamahaRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_patrol_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Submit", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUBMIT OFFICIAL PATROL LOG",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        }
    }
}
