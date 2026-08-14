package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AbnormalityEntity
import com.example.data.AuditLogEntity
import com.example.data.LineEntity
import com.example.data.MachineEntity
import com.example.data.PatrolLogEntity
import com.example.data.PatrolPointEntity
import com.example.data.PatrolPointResultEntity
import com.example.data.PatrolPointRevisionEntity
import com.example.data.ShopEntity
import com.example.data.UserEntity
import com.example.data.YamahaDatabase
import com.example.data.YamahaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object PatrolExecution : Screen()
    object AbnormalityTracker : Screen()
    object MasterData : Screen()
    object Reports : Screen()
    object AuditLogs : Screen()
    object Profile : Screen()
}

class YamahaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: YamahaRepository

    init {
        val database = YamahaDatabase.getDatabase(application, viewModelScope)
        repository = YamahaRepository(database.yamahaDao())
        viewModelScope.launch {
            repository.syncFromSupabase()
        }
    }

    fun syncData(showToast: Boolean = true) {
        viewModelScope.launch {
            val result = repository.syncFromSupabase()
            if (showToast) {
                if (result.isSuccess) {
                    _userMessage.value = result.getOrNull() ?: "Central database synced with Supabase"
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Sync failed"
                    _userMessage.value = "Central sync: $err"
                }
            }
        }
    }

    suspend fun uploadEvidencePhoto(context: Context, uri: Uri): String? {
        return repository.uploadEvidencePhoto(context, uri)
    }

    // --- Current Session State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // --- Reactive Data Streams from Local Room Cache (Synced from Supabase) ---
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShops: StateFlow<List<ShopEntity>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLines: StateFlow<List<LineEntity>> = repository.allLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMachines: StateFlow<List<MachineEntity>> = repository.allMachines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatrolPoints: StateFlow<List<PatrolPointEntity>> = repository.allPatrolPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatrolLogs: StateFlow<List<PatrolLogEntity>> = repository.allPatrolLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAbnormalities: StateFlow<List<AbnormalityEntity>> = repository.allAbnormalities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRevisions: StateFlow<List<PatrolPointRevisionEntity>> = repository.allRevisions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val totalPatrols: StateFlow<Int> = repository.totalPatrolsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingAbnormalities: StateFlow<Int> = repository.pendingAbnormalitiesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val criticalIssues: StateFlow<Int> = repository.criticalIssuesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val operationalMachines: StateFlow<Int> = repository.operationalMachinesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMachines: StateFlow<Int> = repository.totalMachinesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _mustChangePassword = MutableStateFlow(false)
    val mustChangePassword: StateFlow<Boolean> = _mustChangePassword.asStateFlow()

    // --- Authentication ---
    fun login(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (usernameInput.isBlank() || passwordInput.isBlank()) {
                _loginError.value = "Username and password cannot be empty"
                return@launch
            }

            // Central-first authentication: refreshes UserEntity from Supabase
            val user = repository.authenticateUser(usernameInput, passwordInput)
            if (user != null) {
                if (user.status == "Inactive") {
                    _loginError.value = "User account is inactive. Contact Admin."
                    return@launch
                }
                _currentUser.value = user

                // Multi-Device Central Rule:
                // If user is Admin and passwordHash is still the initial default "Admin@123",
                // mandatory password change is required. Once changed centrally on any device,
                // passwordHash is no longer "Admin@123", so no device will prompt again!
                if ((user.role == "ADMIN" || user.username == "admin") && user.passwordHash == "Admin@123") {
                    _mustChangePassword.value = true
                } else {
                    _mustChangePassword.value = false
                    _currentScreen.value = Screen.Dashboard
                }
                repository.logAudit(user, "LOGIN", "Authentication", "Logged into Yamaha Patrol Portal")
            } else {
                _loginError.value = "Invalid username or password."
            }
        }
    }

    fun changePassword(newPassword: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (newPassword.length < 6 || newPassword == "Admin@123") {
                _userMessage.value = "Please choose a strong password different from Admin@123"
                return@launch
            }
            val updatedUser = user.copy(passwordHash = newPassword)
            val result = repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
            _mustChangePassword.value = false
            _currentScreen.value = Screen.Dashboard

            repository.logAudit(updatedUser, "PASSWORD_CHANGED", "Authentication", "Changed default password on central Supabase database")
            if (result.isSuccess) {
                _userMessage.value = "Password updated in central database successfully!"
            } else {
                _userMessage.value = "Password updated locally (will sync to Supabase when connected)"
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user, "LOGOUT", "Authentication", "User logged out")
            }
        }
        _currentUser.value = null
        _currentScreen.value = Screen.Login
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Shop Management ---
    fun addShop(shopName: String) {
        viewModelScope.launch {
            val result = repository.insertShop(ShopEntity(shopName = shopName.trim()))
            if (result.isSuccess) {
                _userMessage.value = "Shop '$shopName' added to central database"
            } else {
                _userMessage.value = "Shop '$shopName' added locally"
            }
        }
    }

    fun deleteShop(shopId: Int) {
        viewModelScope.launch {
            val result = repository.deleteShop(shopId)
            if (result.isSuccess) {
                _userMessage.value = "Shop deleted centrally"
            } else {
                _userMessage.value = "Shop deleted"
            }
        }
    }

    // --- Line Management ---
    fun addLine(shopId: Int, shopName: String, lineName: String) {
        viewModelScope.launch {
            val result = repository.insertLine(LineEntity(shopId = shopId, shopName = shopName, lineName = lineName.trim()))
            if (result.isSuccess) {
                _userMessage.value = "Line '$lineName' added under $shopName in central database"
            } else {
                _userMessage.value = "Line '$lineName' added under $shopName"
            }
        }
    }

    fun deleteLine(lineId: Int) {
        viewModelScope.launch {
            val result = repository.deleteLine(lineId)
            if (result.isSuccess) {
                _userMessage.value = "Line deleted centrally"
            } else {
                _userMessage.value = "Line deleted"
            }
        }
    }

    // --- Machine Management ---
    fun addMachine(
        lineId: Int,
        shopName: String,
        lineName: String,
        machineName: String,
        machineType: String,
        manufacturer: String,
        model: String
    ) {
        viewModelScope.launch {
            val result = repository.insertMachine(
                MachineEntity(
                    lineId = lineId,
                    shopName = shopName,
                    lineName = lineName,
                    machineName = machineName.trim(),
                    machineType = machineType.trim(),
                    manufacturer = manufacturer.trim(),
                    model = model.trim(),
                    status = "Operational"
                )
            )
            if (result.isSuccess) {
                _userMessage.value = "Machine '$machineName' added to central database"
            } else {
                _userMessage.value = "Machine '$machineName' added locally"
            }
        }
    }

    fun deleteMachine(machineId: Int) {
        viewModelScope.launch {
            val result = repository.deleteMachine(machineId)
            if (result.isSuccess) {
                _userMessage.value = "Machine deleted centrally"
            } else {
                _userMessage.value = "Machine deleted"
            }
        }
    }

    // --- Patrol Point Management ---
    fun addPatrolPoint(
        machineId: Int,
        machineName: String,
        pointName: String,
        category: String,
        standardValue: String,
        sequenceNo: Int,
        frequency: String,
        description: String
    ) {
        viewModelScope.launch {
            val result = repository.insertPatrolPoint(
                PatrolPointEntity(
                    machineId = machineId,
                    machineName = machineName,
                    pointName = pointName.trim(),
                    category = category.trim(),
                    standardValue = standardValue.trim(),
                    sequenceNo = sequenceNo,
                    frequency = frequency.trim(),
                    active = true,
                    description = description.trim(),
                    revisionNumber = 1
                )
            )
            if (result.isSuccess) {
                _userMessage.value = "Patrol Point '$pointName' added to central database"
            } else {
                _userMessage.value = "Patrol Point '$pointName' added locally"
            }
        }
    }

    fun revisePatrolPoint(
        point: PatrolPointEntity,
        newStandardValue: String,
        newCategory: String,
        newFrequency: String,
        reason: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.revisePatrolPoint(point, newStandardValue, newCategory, newFrequency, reason, user.employeeName)
            repository.logAudit(user, "REVISE", "Patrol Points", "Revised patrol point: ${point.pointName}")
            if (result.isSuccess) {
                _userMessage.value = "Patrol point revised centrally to Rev #${point.revisionNumber + 1}"
            } else {
                _userMessage.value = "Patrol point revised locally to Rev #${point.revisionNumber + 1}"
            }
        }
    }

    fun deletePatrolPoint(pointId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.deletePatrolPoint(pointId)
            repository.logAudit(user, "DELETE", "Patrol Points", "Deleted patrol point ID $pointId")
            if (result.isSuccess) {
                _userMessage.value = "Patrol point deleted centrally"
            } else {
                _userMessage.value = "Patrol point deleted"
            }
        }
    }

    // --- Patrol Submission ---
    fun submitPatrolWithPhotos(
        shopName: String,
        lineName: String,
        machineName: String,
        machineId: Int,
        shift: String,
        notes: String,
        checkpointResults: List<Triple<PatrolPointEntity, Pair<String, String>, Triple<String, String, Triple<String, String?, String?>>>>
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
            val timeMs = System.currentTimeMillis()
            val patrolNo = "PTL-$dateStr-${(timeMs % 1000000).toString().padStart(6, '0')}"

            val hasAbnormal = checkpointResults.any { it.second.first == "ABNORMAL" }
            val overallStatus = if (hasAbnormal) "ABNORMAL" else "NORMAL"

            val patrolLog = PatrolLogEntity(
                patrolNumber = patrolNo,
                shopName = shopName,
                lineName = lineName,
                machineName = machineName,
                machineId = machineId,
                employeeId = user.employeeId,
                employeeName = user.employeeName,
                shift = shift,
                timestamp = timeMs,
                overallStatus = overallStatus,
                notes = notes
            )

            val resultsEntities = checkpointResults.map { (point, statusAndRemarks, detail) ->
                val probDesc = detail.first
                val severity = detail.second
                val countermeasure = detail.third.first
                val localPhotoUri = detail.third.second

                var remotePhotoUrl: String? = localPhotoUri
                if (!localPhotoUri.isNullOrBlank() && (localPhotoUri.startsWith("content://") || localPhotoUri.startsWith("file://"))) {
                    try {
                        val uploaded = repository.uploadEvidencePhoto(getApplication(), Uri.parse(localPhotoUri))
                        if (uploaded != null) {
                            remotePhotoUrl = uploaded
                        } else {
                            remotePhotoUrl = null
                            _userMessage.value = "Note: Photo stored locally"
                        }
                    } catch (e: Exception) {
                        remotePhotoUrl = null
                    }
                }

                PatrolPointResultEntity(
                    patrolLogId = 0,
                    patrolPointId = point.id,
                    checkpointName = point.pointName,
                    category = point.category,
                    standardValue = point.standardValue,
                    status = statusAndRemarks.first,
                    remarks = statusAndRemarks.second,
                    problemDescription = probDesc,
                    severity = severity,
                    countermeasure = countermeasure,
                    photoUri = remotePhotoUrl
                )
            }

            val abnormalEntities = mutableListOf<AbnormalityEntity>()
            var abCounter = 1
            checkpointResults.filter { it.second.first == "ABNORMAL" }.forEach { (point, statusAndRemarks, detail) ->
                val abNo = "ABN-$dateStr-${((timeMs + abCounter) % 1000000).toString().padStart(6, '0')}"
                abCounter++
                val probDesc = detail.first.ifBlank { statusAndRemarks.second }
                val severity = detail.second
                val countermeasure = detail.third.first
                val photoUrl = resultsEntities.firstOrNull { it.patrolPointId == point.id }?.photoUri

                abnormalEntities.add(
                    AbnormalityEntity(
                        abnormalityNumber = abNo,
                        shopName = shopName,
                        lineName = lineName,
                        machineName = machineName,
                        machineId = machineId,
                        checkpointName = point.pointName,
                        category = point.category,
                        priority = severity,
                        problemDescription = probDesc,
                        rootCause = "Maintenance inspection issue: $probDesc",
                        correctiveAction = countermeasure.ifBlank { "Pending maintenance inspection & corrective action" },
                        responsiblePerson = "Weld Shop Maintenance Engineer",
                        targetDate = "2026-08-05",
                        status = "PENDING",
                        reportedBy = "${user.employeeName} (${user.employeeId})",
                        timestamp = timeMs,
                        photoUri = photoUrl
                    )
                )
            }

            val submitResult = repository.submitPatrolLog(patrolLog, resultsEntities, null)
            val finalLogId = submitResult.getOrDefault(0L).toInt()

            abnormalEntities.forEach { ab ->
                repository.updateAbnormality(ab.copy(patrolLogId = finalLogId), user)
            }

            _userMessage.value = "Patrol $patrolNo submitted centrally! (${abnormalEntities.size} abnormalities recorded)"
            _currentScreen.value = Screen.Dashboard
        }
    }

    // --- Abnormality Actions ---
    fun updateAbnormality(
        abnormality: AbnormalityEntity,
        newStatus: String,
        correctiveAction: String,
        rootCause: String,
        responsiblePerson: String,
        priority: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val completedDate = if (newStatus == "RESOLVED" || newStatus == "VERIFIED") {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            } else abnormality.completedDate

            val updated = abnormality.copy(
                status = newStatus,
                correctiveAction = correctiveAction,
                rootCause = rootCause,
                responsiblePerson = responsiblePerson,
                priority = priority,
                completedDate = completedDate
            )
            val result = repository.updateAbnormality(updated, user)
            if (result.isSuccess) {
                _userMessage.value = "Abnormality #${abnormality.id} updated centrally to $newStatus"
            } else {
                _userMessage.value = "Abnormality #${abnormality.id} updated locally"
            }
        }
    }

    // --- User Administration (Admin only) ---
    fun createOrUpdateUser(
        employeeId: String,
        employeeName: String,
        username: String,
        role: String,
        department: String,
        plant: String,
        password: String
    ) {
        val adminUser = _currentUser.value ?: return
        viewModelScope.launch {
            val cleanUsername = username.trim().lowercase().removeSuffix("@yamaha-motor-india.com")
            val email = "$cleanUsername@yamaha-motor-india.com"

            val newUser = UserEntity(
                employeeId = employeeId.trim(),
                employeeName = employeeName.trim(),
                username = cleanUsername,
                email = email,
                department = department,
                plant = plant,
                role = role,
                passwordHash = password,
                createdBy = adminUser.employeeName
            )
            val result = repository.insertUser(newUser)
            repository.logAudit(adminUser, "CREATE", "User Management", "Created/Updated user $cleanUsername ($employeeId)")
            if (result.isSuccess) {
                _userMessage.value = "User $employeeName saved in central database!"
            } else {
                _userMessage.value = "User $employeeName saved locally"
            }
        }
    }

    fun deleteUser(employeeId: String) {
        val adminUser = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.deleteUser(employeeId)
            repository.logAudit(adminUser, "DELETE", "User Management", "Deleted user ID $employeeId")
            if (result.isSuccess) {
                _userMessage.value = "User deleted from central database"
            } else {
                _userMessage.value = "User deleted locally"
            }
        }
    }

    fun clearTransactionalData() {
        val user = _currentUser.value ?: return
        if (user.role != "ADMIN") return
        viewModelScope.launch {
            val result = repository.clearTransactionalData()
            repository.logAudit(user, "CLEAR_DATA", "System Management", "Cleared test/demo transactional records")
            if (result.isSuccess) {
                _userMessage.value = "All transactional records cleared from central and local databases!"
            } else {
                _userMessage.value = "Transactional records cleared"
            }
        }
    }

    suspend fun getResultsForLog(logId: Int): List<PatrolPointResultEntity> {
        return repository.getResultsForLog(logId)
    }

    suspend fun getAllResultsDirect(): List<PatrolPointResultEntity> {
        return repository.getAllResultsDirect()
    }

    fun generateReportData(reportType: String): String {
        val logs = allPatrolLogs.value
        val sb = StringBuilder()
        sb.append("Patrol Number,Shop Name,Line Name,Machine Name,Inspector Name,Employee ID,Shift,Timestamp,Overall Status,Notes\n")
        logs.forEach { log ->
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp))
            val cleanNotes = log.notes.replace(",", ";").replace("\n", " ")
            sb.append("${log.patrolNumber},${log.shopName},${log.lineName},${log.machineName},${log.employeeName},${log.employeeId},${log.shift},$dateStr,${log.overallStatus},$cleanNotes\n")
        }
        return sb.toString()
    }
}
