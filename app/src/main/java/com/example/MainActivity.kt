package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.YamahaTopHeader
import com.example.ui.screens.AbnormalityTrackerScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MasterDataScreen
import com.example.ui.screens.PatrolExecutionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaPatrolTheme
import com.example.ui.theme.YamahaRed
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.YamahaViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: YamahaViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      YamahaPatrolTheme {
        YamahaMainContent(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.syncData(showToast = false)
  }
}

@Composable
fun YamahaMainContent(viewModel: YamahaViewModel) {
  val context = LocalContext.current
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val loginError by viewModel.loginError.collectAsStateWithLifecycle()
  val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
  val mustChangePassword by viewModel.mustChangePassword.collectAsStateWithLifecycle()

  // State Flows
  val totalPatrols by viewModel.totalPatrols.collectAsStateWithLifecycle()
  val pendingAbnormalities by viewModel.pendingAbnormalities.collectAsStateWithLifecycle()
  val criticalIssues by viewModel.criticalIssues.collectAsStateWithLifecycle()
  val operationalMachines by viewModel.operationalMachines.collectAsStateWithLifecycle()
  val totalMachines by viewModel.totalMachines.collectAsStateWithLifecycle()

  val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
  val allShops by viewModel.allShops.collectAsStateWithLifecycle()
  val allLines by viewModel.allLines.collectAsStateWithLifecycle()
  val allMachines by viewModel.allMachines.collectAsStateWithLifecycle()
  val allPoints by viewModel.allPatrolPoints.collectAsStateWithLifecycle()
  val allRevisions by viewModel.allRevisions.collectAsStateWithLifecycle()
  val allPatrolLogs by viewModel.allPatrolLogs.collectAsStateWithLifecycle()
  val allAbnormalities by viewModel.allAbnormalities.collectAsStateWithLifecycle()
  val allAuditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()

  // Display User Toast Messages
  LaunchedEffect(userMessage) {
    userMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.clearUserMessage()
    }
  }

  val user = currentUser

  if (user == null || currentScreen is Screen.Login) {
    LoginScreen(
      onLoginClick = { u, p -> viewModel.login(u, p) },
      errorMessage = loginError,
      mustChangePassword = mustChangePassword,
      onChangePasswordSubmit = { newPass -> viewModel.changePassword(newPass) }
    )
  } else {
    Scaffold(
      topBar = {
        YamahaTopHeader(
          title = when (currentScreen) {
            Screen.Dashboard -> "Executive Dashboard"
            Screen.PatrolExecution -> "Patrol Execution"
            Screen.AbnormalityTracker -> "Abnormality & RCA"
            Screen.MasterData -> "Master Data (RBAC)"
            Screen.Reports -> "Reports & Analytics"
            Screen.AuditLogs -> "System Audit Trail"
            Screen.Profile -> "Employee Profile"
            else -> "Yamaha Portal"
          },
          user = user,
          onLogoutClick = { viewModel.logout() },
          onSyncClick = { viewModel.syncData(showToast = true) }
        )
      },
      bottomBar = {
        NavigationBar(
          containerColor = YamahaBlue,
          contentColor = androidx.compose.ui.graphics.Color.White
        ) {
          NavigationBarItem(
            selected = currentScreen is Screen.Dashboard,
            onClick = { viewModel.navigateTo(Screen.Dashboard) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = YamahaRed,
              selectedTextColor = YamahaRed,
              unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
              unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("nav_dashboard")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.PatrolExecution,
            onClick = { viewModel.navigateTo(Screen.PatrolExecution) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Patrol") },
            label = { Text("Patrol", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = YamahaRed,
              selectedTextColor = YamahaRed,
              unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
              unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("nav_patrol")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.AbnormalityTracker,
            onClick = { viewModel.navigateTo(Screen.AbnormalityTracker) },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Abnormality") },
            label = { Text("Issues", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = YamahaRed,
              selectedTextColor = YamahaRed,
              unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
              unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("nav_issues")
          )

          NavigationBarItem(
            selected = currentScreen is Screen.Reports,
            onClick = { viewModel.navigateTo(Screen.Reports) },
            icon = { Icon(Icons.Default.Assignment, contentDescription = "Reports") },
            label = { Text("Reports", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = YamahaRed,
              selectedTextColor = YamahaRed,
              unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
              unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("nav_reports")
          )

          if (user.role == "ADMIN" || user.role == "SUPERVISOR") {
            NavigationBarItem(
              selected = currentScreen is Screen.MasterData,
              onClick = { viewModel.navigateTo(Screen.MasterData) },
              icon = { Icon(Icons.Default.Settings, contentDescription = "Master Data") },
              label = { Text("Master", fontSize = 10.sp) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = YamahaRed,
                selectedTextColor = YamahaRed,
                unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
              ),
              modifier = Modifier.testTag("nav_master")
            )
          }

          if (user.role == "ADMIN") {
            NavigationBarItem(
              selected = currentScreen is Screen.AuditLogs,
              onClick = { viewModel.navigateTo(Screen.AuditLogs) },
              icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
              label = { Text("Audit", fontSize = 10.sp) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = YamahaRed,
                selectedTextColor = YamahaRed,
                unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
              ),
              modifier = Modifier.testTag("nav_audit")
            )
          }

          NavigationBarItem(
            selected = currentScreen is Screen.Profile,
            onClick = { viewModel.navigateTo(Screen.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = YamahaRed,
              selectedTextColor = YamahaRed,
              unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
              unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("nav_profile")
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentScreen) {
          Screen.Dashboard -> DashboardScreen(
            user = user,
            totalPatrols = totalPatrols,
            pendingAbnormalities = pendingAbnormalities,
            criticalIssues = criticalIssues,
            operationalMachines = operationalMachines,
            totalMachines = totalMachines,
            recentPatrols = allPatrolLogs,
            recentAbnormalities = allAbnormalities,
            onNavigate = { viewModel.navigateTo(it) }
          )

          Screen.PatrolExecution -> PatrolExecutionScreen(
            user = user,
            shops = allShops,
            lines = allLines,
            machines = allMachines,
            points = allPoints,
            onSubmitPatrolWithPhotos = { shop, line, machine, machineId, shift, notes, results ->
              viewModel.submitPatrolWithPhotos(shop, line, machine, machineId, shift, notes, results)
            }
          )

          Screen.AbnormalityTracker -> AbnormalityTrackerScreen(
            user = user,
            abnormalities = allAbnormalities,
            onUpdateAbnormality = { ab, status, action, rca, resp, prio ->
              viewModel.updateAbnormality(ab, status, action, rca, resp, prio)
            }
          )

          Screen.MasterData -> MasterDataScreen(
            currentUser = user,
            users = allUsers,
            shops = allShops,
            lines = allLines,
            machines = allMachines,
            points = allPoints,
            revisions = allRevisions,
            onCreateUser = { empId, name, uname, role, dept, plant, pass ->
              viewModel.createOrUpdateUser(empId, name, uname, role, dept, plant, pass)
            },
            onDeleteUser = { empId -> viewModel.deleteUser(empId) },
            onAddShop = { shopName -> viewModel.addShop(shopName) },
            onDeleteShop = { shopId -> viewModel.deleteShop(shopId) },
            onAddLine = { shopId, shopName, lineName -> viewModel.addLine(shopId, shopName, lineName) },
            onDeleteLine = { lineId -> viewModel.deleteLine(lineId) },
            onAddMachine = { lineId, shopName, lineName, machineName, machineType, manufacturer, model ->
              viewModel.addMachine(lineId, shopName, lineName, machineName, machineType, manufacturer, model)
            },
            onDeleteMachine = { machineId -> viewModel.deleteMachine(machineId) },
            onAddPoint = { machineId, machineName, pointName, category, standardValue, sequenceNo, frequency, description ->
              viewModel.addPatrolPoint(machineId, machineName, pointName, category, standardValue, sequenceNo, frequency, description)
            },
            onRevisePoint = { pt, std, cat, freq, reason ->
              viewModel.revisePatrolPoint(pt, std, cat, freq, reason)
            },
            onDeletePoint = { ptId -> viewModel.deletePatrolPoint(ptId) },
            onClearTransactionalData = { viewModel.clearTransactionalData() }
          )

          Screen.Reports -> ReportsScreen(
            user = user,
            patrolLogs = allPatrolLogs,
            abnormalities = allAbnormalities,
            onGetResultsForLog = { logId -> viewModel.getResultsForLog(logId) },
            onGetAllResults = { viewModel.getAllResultsDirect() },
            onGenerateExport = { type -> viewModel.generateReportData(type) }
          )

          Screen.AuditLogs -> AuditLogScreen(
            auditLogs = allAuditLogs
          )

          Screen.Profile -> ProfileScreen(
            user = user,
            onLogoutClick = { viewModel.logout() }
          )

          else -> DashboardScreen(
            user = user,
            totalPatrols = totalPatrols,
            pendingAbnormalities = pendingAbnormalities,
            criticalIssues = criticalIssues,
            operationalMachines = operationalMachines,
            totalMachines = totalMachines,
            recentPatrols = allPatrolLogs,
            recentAbnormalities = allAbnormalities,
            onNavigate = { viewModel.navigateTo(it) }
          )
        }
      }
    }
  }
}

