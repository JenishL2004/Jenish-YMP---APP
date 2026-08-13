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

    fun syncData() {
        viewModelScope.launch {
            repository.syncFromSupabase()
            _userMessage.value = "Central database synced with Supabase"
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

    // --- Reactive Data Streams ---
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

    // Stats
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

    val allRevisions: StateFlow<List<com.example.data.PatrolPointRevisionEntity>> = repository.allRevisions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication ---
    fun login(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (usernameInput.isBlank() || passwordInput.isBlank()) {
                _loginError.value = "Username and password cannot be empty"
                return@launch
            }

            val user = repository.authenticateUser(usernameInput, passwordInput)
            if (user != null) {
                if (user.status == "Inactive") {
                    _loginError.value = "User account is inactive. Contact Admin."
                    return@launch
                }
                _currentUser.value = user

                val prefs = getApplication<Application>().getSharedPreferences("yamaha_prefs", Context.MODE_PRIVATE)
                val passwordChangedInPrefs = prefs.getBoolean("admin_password_changed_${user.username}", false)
                
                // Force password change ONLY if password is default "Admin@123" AND not yet updated in prefs
                if (user.passwordHash == "Admin@123" && !passwordChangedInPrefs) {
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
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
            _mustChangePassword.value = false
            _currentScreen.value = Screen.Dashboard

            val prefs = getApplication<Application>().getSharedPreferences("yamaha_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("admin_password_changed_${user.username}", true).apply()

            repository.logAudit(updatedUser, "PASSWORD_CHANGED", "Authentication", "Changed default password on initial login")
            _userMessage.value = "Password updated successfully!"
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
            repository.insertShop(ShopEntity(shopName = shopName.trim()))
            _userMessage.value = "Shop '$shopName' added successfully"
        }
    }

    fun deleteShop(shopId: Int) {
        viewModelScope.launch {
            repository.deleteShop(shopId)
            _userMessage.value = "Shop deleted"
        }
    }

    // --- Line Management ---
    fun addLine(shopId: Int, shopName: String, lineName: String) {
        viewModelScope.launch {
            repository.insertLine(LineEntity(shopId = shopId, shopName = shopName, lineName = lineName.trim()))
            _userMessage.value = "Line '$lineName' added under $shopName"
        }
    }

    fun deleteLine(lineId: Int) {
        viewModelScope.launch {
            repository.deleteLine(lineId)
            _userMessage.value = "Line deleted"
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
            repository.insertMachine(
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
            _userMessage.value = "Machine '$machineName' added to $lineName"
        }
    }

    fun deleteMachine(machineId: Int) {
        viewModelScope.launch {
            repository.deleteMachine(machineId)
            _userMessage.value = "Machine deleted"
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
            repository.insertPatrolPoint(
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
            _userMessage.value = "Patrol Point '$pointName' added for $machineName"
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
            repository.revisePatrolPoint(point, newStandardValue, newCategory, newFrequency, reason, user.employeeName)
            repository.logAudit(user, "REVISE", "Patrol Points", "Revised patrol point: ${point.pointName}")
            _userMessage.value = "Patrol point revised to Rev #${point.revisionNumber + 1}"
        }
    }

    fun deletePatrolPoint(pointId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deletePatrolPoint(pointId)
            repository.logAudit(user, "DELETE", "Patrol Points", "Deleted patrol point ID $pointId")
            _userMessage.value = "Patrol point deleted"
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
                            _userMessage.value = "Warning: Failed to upload evidence photo to Supabase storage"
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

            // Create individual abnormality entry for EACH abnormal checkpoint
            val abnormalEntities = mutableListOf<AbnormalityEntity>()
            var abCounter = 1
            checkpointResults.filter { it.second.first == "ABNORMAL" }.forEachIndexed { idx, (point, statusAndRemarks, detail) ->
                val abNo = "ABN-$dateStr-${((timeMs + abCounter) % 1000000).toString().padStart(6, '0')}"
                abCounter++
                val probDesc = detail.first.ifBlank { statusAndRemarks.second }
                val severity = detail.second
                val countermeasure = detail.third.first
                val photoUrl = resultsEntities.getOrNull(checkpointResults.indexOfFirst { it.first.id == point.id })?.photoUri

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

            val logId = repository.submitPatrolLog(patrolLog, resultsEntities, null)
            abnormalEntities.forEach { ab ->
                repository.updateAbnormality(ab.copy(patrolLogId = logId.toInt()), user)
            }

            _userMessage.value = "Patrol $patrolNo submitted! (${abnormalEntities.size} abnormalities recorded)"
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
                "2026-07-30"
            } else abnormality.completedDate

            val updated = abnormality.copy(
                status = newStatus,
                correctiveAction = correctiveAction,
                rootCause = rootCause,
                responsiblePerson = responsiblePerson,
                priority = priority,
                completedDate = completedDate
            )
            repository.updateAbnormality(updated, user)
            _userMessage.value = "Abnormality #${abnormality.id} updated to $newStatus"
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
            repository.insertUser(newUser)
            repository.logAudit(adminUser, "CREATE", "User Management", "Created/Updated user $cleanUsername ($employeeId)")
            _userMessage.value = "User $employeeName ($employeeId) saved!"
        }
    }

    fun deleteUser(employeeId: String) {
        val adminUser = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteUser(employeeId)
            repository.logAudit(adminUser, "DELETE", "User Management", "Deleted user ID $employeeId")
            _userMessage.value = "User deleted successfully"
        }
    }

    fun clearTransactionalData() {
        val user = _currentUser.value ?: return
        if (user.role != "ADMIN") return
        viewModelScope.launch {
            repository.clearTransactionalData()
            repository.logAudit(user, "CLEAR_DATA", "System Management", "Cleared test/demo transactional records")
            _userMessage.value = "All test/demo transactional records cleared successfully!"
        }
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
