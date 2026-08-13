package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LineEntity
import com.example.data.MachineEntity
import com.example.data.PatrolPointEntity
import com.example.data.ShopEntity
import com.example.data.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed

@Composable
fun MasterDataScreen(
    currentUser: UserEntity,
    users: List<UserEntity>,
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    machines: List<MachineEntity>,
    points: List<PatrolPointEntity>,
    revisions: List<com.example.data.PatrolPointRevisionEntity> = emptyList(),
    onCreateUser: (employeeId: String, name: String, username: String, role: String, department: String, plant: String, password: String) -> Unit,
    onDeleteUser: (employeeId: String) -> Unit,
    onAddShop: (shopName: String) -> Unit,
    onDeleteShop: (shopId: Int) -> Unit,
    onAddLine: (shopId: Int, shopName: String, lineName: String) -> Unit,
    onDeleteLine: (lineId: Int) -> Unit,
    onAddMachine: (lineId: Int, shopName: String, lineName: String, machineName: String, machineType: String, manufacturer: String, model: String) -> Unit,
    onDeleteMachine: (machineId: Int) -> Unit,
    onAddPoint: (machineId: Int, machineName: String, pointName: String, category: String, standardValue: String, sequenceNo: Int, frequency: String, description: String) -> Unit,
    onRevisePoint: (point: PatrolPointEntity, newStandard: String, newCategory: String, newFreq: String, reason: String) -> Unit = { _, _, _, _, _ -> },
    onDeletePoint: (pointId: Int) -> Unit = {},
    onClearTransactionalData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Shop Mgmt", "Line Mgmt", "Machine Mgmt", "Patrol Points", "Users (RBAC)")

    var showAddShopDialog by remember { mutableStateOf(false) }
    var showAddLineDialog by remember { mutableStateOf(false) }
    var showAddMachineDialog by remember { mutableStateOf(false) }
    var showAddPointDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showRevisePointDialog by remember { mutableStateOf<PatrolPointEntity?>(null) }
    var showRevisionHistoryDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YamahaBlue)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MASTER SETTINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                if (currentUser.role == "ADMIN") {
                    TextButton(onClick = { showClearDataDialog = true }) {
                        Text("Clear Demo Data", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = YamahaBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTabIndex) {
                0 -> ShopManagementTab(shops = shops, onDeleteShop = onDeleteShop)
                1 -> LineManagementTab(shops = shops, lines = lines, onDeleteLine = onDeleteLine)
                2 -> MachineManagementTab(shops = shops, lines = lines, machines = machines, onDeleteMachine = onDeleteMachine)
                3 -> PatrolPointsMasterTab(
                    currentUser = currentUser,
                    shops = shops,
                    lines = lines,
                    machines = machines,
                    points = points,
                    revisions = revisions,
                    onReviseClick = { showRevisePointDialog = it },
                    onDeleteClick = { onDeletePoint(it.id) },
                    onViewRevisionsClick = { showRevisionHistoryDialog = true }
                )
                4 -> UserManagementTab(
                    currentUser = currentUser,
                    users = users,
                    onDeleteUser = onDeleteUser
                )
            }
        }

        // Floating Action Button to Add Items
        FloatingActionButton(
            onClick = {
                when (selectedTabIndex) {
                    0 -> showAddShopDialog = true
                    1 -> showAddLineDialog = true
                    2 -> showAddMachineDialog = true
                    3 -> showAddPointDialog = true
                    4 -> showAddUserDialog = true
                }
            },
            containerColor = YamahaRed,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_master_data")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Master Entry")
        }
    }

    // Dialogs
    if (showAddShopDialog) {
        AddShopDialog(
            onDismiss = { showAddShopDialog = false },
            onSave = { shopName ->
                onAddShop(shopName)
                showAddShopDialog = false
            }
        )
    }

    if (showAddLineDialog) {
        AddLineDialog(
            shops = shops,
            onDismiss = { showAddLineDialog = false },
            onSave = { shopId, shopName, lineName ->
                onAddLine(shopId, shopName, lineName)
                showAddLineDialog = false
            }
        )
    }

    if (showAddMachineDialog) {
        AddMachineDialog(
            shops = shops,
            lines = lines,
            onDismiss = { showAddMachineDialog = false },
            onSave = { lineId, shopName, lineName, machineName, machineType, manufacturer, model ->
                onAddMachine(lineId, shopName, lineName, machineName, machineType, manufacturer, model)
                showAddMachineDialog = false
            }
        )
    }

    if (showAddPointDialog) {
        AddPointDialog(
            shops = shops,
            lines = lines,
            machines = machines,
            onDismiss = { showAddPointDialog = false },
            onSave = { machineId, machineName, pointName, category, standardValue, sequenceNo, frequency, description ->
                onAddPoint(machineId, machineName, pointName, category, standardValue, sequenceNo, frequency, description)
                showAddPointDialog = false
            }
        )
    }

    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onSave = { empId, name, uname, role, dept, plant, pass ->
                onCreateUser(empId, name, uname, role, dept, plant, pass)
                showAddUserDialog = false
            }
        )
    }

    showRevisePointDialog?.let { pt ->
        RevisePointDialog(
            point = pt,
            onDismiss = { showRevisePointDialog = null },
            onSave = { std, cat, freq, reason ->
                onRevisePoint(pt, std, cat, freq, reason)
                showRevisePointDialog = null
            }
        )
    }

    if (showRevisionHistoryDialog) {
        RevisionHistoryModal(
            revisions = revisions,
            onDismiss = { showRevisionHistoryDialog = false }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Test & Demo Data", fontWeight = FontWeight.Bold, color = StatusAbnormal) },
            text = {
                Text(
                    text = "This action will clear all test/demo patrol inspection logs, abnormality tickets, and audit history records.\n\nMaster data (Shops, Lines, Machines, Patrol Points, Users) will be preserved.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearTransactionalData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaRed)
                ) { Text("Confirm Clear Data") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ShopManagementTab(
    shops: List<ShopEntity>,
    onDeleteShop: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(shops) { s ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(10.dp),
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
                            text = s.shopName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = YamahaBlue)
                        )
                        Text(
                            text = "Shop ID: #${s.id}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    IconButton(onClick = { onDeleteShop(s.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Shop", tint = StatusAbnormal)
                    }
                }
            }
        }
    }
}

@Composable
fun LineManagementTab(
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    onDeleteLine: (Int) -> Unit
) {
    var selectedShopFilter by remember { mutableStateOf("ALL") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedShopFilter == "ALL",
                onClick = { selectedShopFilter = "ALL" },
                label = { Text("All Shops") }
            )
            shops.forEach { shop ->
                FilterChip(
                    selected = selectedShopFilter == shop.shopName,
                    onClick = { selectedShopFilter = shop.shopName },
                    label = { Text(shop.shopName) }
                )
            }
        }

        val filteredLines = lines.filter {
            selectedShopFilter == "ALL" || it.shopName == selectedShopFilter
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredLines) { line ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
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
                                text = line.lineName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Shop: ${line.shopName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = YamahaBlue)
                            )
                        }

                        IconButton(onClick = { onDeleteLine(line.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Line", tint = StatusAbnormal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MachineManagementTab(
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    machines: List<MachineEntity>,
    onDeleteMachine: (Int) -> Unit
) {
    var selectedLineFilter by remember { mutableStateOf("ALL") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLineFilter == "ALL",
                onClick = { selectedLineFilter = "ALL" },
                label = { Text("All Lines") }
            )
            lines.forEach { l ->
                FilterChip(
                    selected = selectedLineFilter == l.lineName,
                    onClick = { selectedLineFilter = l.lineName },
                    label = { Text(l.lineName) }
                )
            }
        }

        val filteredMachines = machines.filter {
            selectedLineFilter == "ALL" || it.lineName == selectedLineFilter
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredMachines) { m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = m.machineName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = YamahaBlue)
                            )
                            Text(
                                text = "Type: ${m.machineType} | ${m.manufacturer} ${m.model}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Line: ${m.lineName} (${m.shopName})",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = m.status)
                            IconButton(onClick = { onDeleteMachine(m.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Machine", tint = StatusAbnormal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatrolPointsMasterTab(
    currentUser: UserEntity,
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    machines: List<MachineEntity>,
    points: List<PatrolPointEntity>,
    revisions: List<com.example.data.PatrolPointRevisionEntity>,
    onReviseClick: (PatrolPointEntity) -> Unit,
    onDeleteClick: (PatrolPointEntity) -> Unit,
    onViewRevisionsClick: () -> Unit
) {
    var selectedMachineFilter by remember { mutableStateOf("ALL") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Patrol Points (${points.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onViewRevisionsClick) {
                Icon(imageVector = Icons.Default.ListAlt, contentDescription = "History", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Revision Log (${revisions.size})", fontSize = 11.sp, color = YamahaBlue)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedMachineFilter == "ALL",
                onClick = { selectedMachineFilter = "ALL" },
                label = { Text("All Machines") }
            )
            machines.forEach { m ->
                FilterChip(
                    selected = selectedMachineFilter == m.machineName,
                    onClick = { selectedMachineFilter = m.machineName },
                    label = { Text(m.machineName) }
                )
            }
        }

        val filteredPoints = points.filter {
            selectedMachineFilter == "ALL" || it.machineName == selectedMachineFilter
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPoints) { pt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pt.pointName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(YamahaBlue.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = pt.category,
                                        style = MaterialTheme.typography.labelSmall.copy(color = YamahaBlue, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Machine: ${pt.machineName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "Standard: ${pt.standardValue}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (currentUser.role == "ADMIN") {
                            Row {
                                IconButton(onClick = { onReviseClick(pt) }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Revise Point", tint = YamahaBlue, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onDeleteClick(pt) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Point", tint = StatusAbnormal, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserManagementTab(
    currentUser: UserEntity,
    users: List<UserEntity>,
    onDeleteUser: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users) { u ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${u.employeeName} (${u.employeeId})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Username: ${u.username} | Role: ${u.role}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = YamahaBlue)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status = u.role)
                        if (currentUser.role == "ADMIN" && u.employeeId != currentUser.employeeId) {
                            IconButton(onClick = { onDeleteUser(u.employeeId) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete User",
                                    tint = StatusAbnormal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialogs
@Composable
fun AddShopDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var shopName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shop Master", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Shop Name (e.g. Weld Shop)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (shopName.isNotBlank()) onSave(shopName) },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save Shop") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLineDialog(
    shops: List<ShopEntity>,
    onDismiss: () -> Unit,
    onSave: (shopId: Int, shopName: String, lineName: String) -> Unit
) {
    var selectedShop by remember { mutableStateOf(shops.firstOrNull()) }
    var expandedShop by remember { mutableStateOf(false) }
    var lineName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Line Master", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedShop,
                    onExpandedChange = { expandedShop = !expandedShop }
                ) {
                    OutlinedTextField(
                        value = selectedShop?.shopName ?: "Select Shop",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedShop) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedShop,
                        onDismissRequest = { expandedShop = false }
                    ) {
                        shops.forEach { shop ->
                            DropdownMenuItem(
                                text = { Text(shop.shopName) },
                                onClick = {
                                    selectedShop = shop
                                    expandedShop = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lineName,
                    onValueChange = { lineName = it },
                    label = { Text("Line Name (e.g. Frame Line 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedShop?.let { shop ->
                        if (lineName.isNotBlank()) onSave(shop.id, shop.shopName, lineName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save Line") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMachineDialog(
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    onDismiss: () -> Unit,
    onSave: (lineId: Int, shopName: String, lineName: String, machineName: String, machineType: String, manufacturer: String, model: String) -> Unit
) {
    var selectedShop by remember { mutableStateOf(shops.firstOrNull()) }
    var selectedLine by remember { mutableStateOf(lines.firstOrNull()) }
    var expandedShop by remember { mutableStateOf(false) }
    var expandedLine by remember { mutableStateOf(false) }

    var machineName by remember { mutableStateOf("") }
    var machineType by remember { mutableStateOf("Robot Welding") }
    var manufacturer by remember { mutableStateOf("OTC Daihen") }
    var model by remember { mutableStateOf("FD-V8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Machine Master", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Shop Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedShop,
                    onExpandedChange = { expandedShop = !expandedShop }
                ) {
                    OutlinedTextField(
                        value = selectedShop?.shopName ?: "Select Shop",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Shop") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedShop) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedShop,
                        onDismissRequest = { expandedShop = false }
                    ) {
                        shops.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.shopName) },
                                onClick = {
                                    selectedShop = s
                                    expandedShop = false
                                }
                            )
                        }
                    }
                }

                // Line Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedLine,
                    onExpandedChange = { expandedLine = !expandedLine }
                ) {
                    OutlinedTextField(
                        value = selectedLine?.lineName ?: "Select Line",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Line") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLine) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLine,
                        onDismissRequest = { expandedLine = false }
                    ) {
                        lines.filter { selectedShop == null || it.shopId == selectedShop?.id }.forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l.lineName) },
                                onClick = {
                                    selectedLine = l
                                    expandedLine = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = machineName, onValueChange = { machineName = it }, label = { Text("Machine Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = machineType, onValueChange = { machineType = it }, label = { Text("Machine Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = { Text("Manufacturer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = selectedLine
                    val s = selectedShop
                    if (l != null && s != null && machineName.isNotBlank()) {
                        onSave(l.id, s.shopName, l.lineName, machineName, machineType, manufacturer, model)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save Machine") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPointDialog(
    shops: List<ShopEntity>,
    lines: List<LineEntity>,
    machines: List<MachineEntity>,
    onDismiss: () -> Unit,
    onSave: (machineId: Int, machineName: String, pointName: String, category: String, standardValue: String, sequenceNo: Int, frequency: String, description: String) -> Unit
) {
    var selectedMachine by remember { mutableStateOf(machines.firstOrNull()) }
    var expandedMachine by remember { mutableStateOf(false) }

    var pointName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Robot") }
    var standardValue by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Every Shift") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inspection Patrol Point", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedMachine,
                    onExpandedChange = { expandedMachine = !expandedMachine }
                ) {
                    OutlinedTextField(
                        value = selectedMachine?.machineName ?: "Select Machine",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Machine") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMachine) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMachine,
                        onDismissRequest = { expandedMachine = false }
                    ) {
                        machines.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.machineName} (${m.lineName})") },
                                onClick = {
                                    selectedMachine = m
                                    expandedMachine = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = pointName, onValueChange = { pointName = it }, label = { Text("Patrol Point Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Robot, Welding, Quality, Electrical, Mechanical)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = standardValue, onValueChange = { standardValue = it }, label = { Text("Standard Value / Specification") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Inspection Frequency") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Inspection Notes / Guide") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedMachine?.let { m ->
                        if (pointName.isNotBlank() && standardValue.isNotBlank()) {
                            onSave(m.id, m.machineName, pointName, category, standardValue, 1, frequency, description)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save Point") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RevisePointDialog(
    point: PatrolPointEntity,
    onDismiss: () -> Unit,
    onSave: (std: String, cat: String, freq: String, reason: String) -> Unit
) {
    var stdValue by remember { mutableStateOf(point.standardValue) }
    var category by remember { mutableStateOf(point.category) }
    var freq by remember { mutableStateOf(point.frequency) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Revise Checkpoint: ${point.pointName} (Rev #${point.revisionNumber + 1})", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = stdValue, onValueChange = { stdValue = it }, label = { Text("New Standard Value") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Engineering Revision Reason (Mandatory)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onSave(stdValue, category, freq, reason) },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save Revision") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RevisionHistoryModal(
    revisions: List<com.example.data.PatrolPointRevisionEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Patrol Master Revision Log", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
                items(revisions) { rev ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Point #${rev.pointId} -> Rev #${rev.revisionNumber} by ${rev.revisedBy}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(text = "Reason: ${rev.reason}", fontSize = 10.sp)
                            Text(text = "Old: ${rev.oldValue}", fontSize = 9.sp, color = Color.Gray)
                            Text(text = "New: ${rev.newValue}", fontSize = 9.sp, color = YamahaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)) { Text("Close") } }
    )
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (employeeId: String, name: String, username: String, role: String, department: String, plant: String, pass: String) -> Unit
) {
    var empId by remember { mutableStateOf("YMH-ENG-${(100..999).random()}") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MAINTENANCE_ENGINEER") }
    var password by remember { mutableStateOf("yamaha123") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create User", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = empId, onValueChange = { empId = it }, label = { Text("Employee ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())

                Text(text = "Role Assignment:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ADMIN", "SUPERVISOR", "MAINTENANCE_ENGINEER", "OPERATOR").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 9.sp) }
                        )
                    }
                }

                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Initial Password") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(empId, name, username, role, "Weld Shop Maintenance", "Plant 1", password) },
                colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
            ) { Text("Save User") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

