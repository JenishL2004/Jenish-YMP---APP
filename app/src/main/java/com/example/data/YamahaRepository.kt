package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YamahaRepository(private val dao: YamahaDao) {

    val supabaseService = SupabaseService()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val realtimeManager: SupabaseRealtimeManager by lazy {
        SupabaseRealtimeManager(
            baseUrl = supabaseService.getBaseUrl(),
            apiKey = supabaseService.getApiKey(),
            scope = repoScope,
            onEventReceived = ::handleRealtimeEvent
        )
    }

    init {
        startRealtimeSync()
    }

    fun startRealtimeSync() {
        if (supabaseService.isConfigured()) {
            realtimeManager.connect()
        }
    }

    private suspend fun handleRealtimeEvent(table: String, action: String, record: JSONObject?, oldRecord: JSONObject?) {
        try {
            when (table.lowercase()) {
                "users" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val user = UserEntity(
                                    employeeId = obj.optString("employee_id"),
                                    employeeName = obj.optString("employee_name"),
                                    username = obj.optString("username"),
                                    email = obj.optString("email"),
                                    department = obj.optString("department"),
                                    plant = obj.optString("plant"),
                                    role = obj.optString("role"),
                                    passwordHash = obj.optString("password_hash"),
                                    status = obj.optString("status", "Active"),
                                    createdBy = obj.optString("created_by")
                                )
                                if (user.employeeId.isNotBlank()) dao.insertUser(user)
                            }
                        }
                        "DELETE" -> {
                            val empId = oldRecord?.optString("employee_id")?.ifBlank { record?.optString("employee_id") }
                            if (!empId.isNullOrBlank() && empId != "admin") {
                                dao.deleteUser(empId)
                            }
                        }
                    }
                }
                "shops" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val shopName = obj.optString("shop_name")
                                if (id > 0 && shopName.isNotBlank()) {
                                    dao.insertShop(ShopEntity(id = id, shopName = shopName))
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deleteShop(id)
                        }
                    }
                }
                "lines" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val shopId = obj.optInt("shop_id")
                                val shopName = obj.optString("shop_name")
                                val lineName = obj.optString("line_name")
                                if (id > 0 && lineName.isNotBlank()) {
                                    dao.insertLine(LineEntity(id = id, shopId = shopId, shopName = shopName, lineName = lineName))
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deleteLine(id)
                        }
                    }
                }
                "machines" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val lineId = obj.optInt("line_id")
                                val shopName = obj.optString("shop_name")
                                val lineName = obj.optString("line_name")
                                val machineName = obj.optString("machine_name")
                                val machineType = obj.optString("machine_type")
                                val manufacturer = obj.optString("manufacturer")
                                val model = obj.optString("model")
                                val status = obj.optString("status", "Operational")
                                if (id > 0 && machineName.isNotBlank()) {
                                    dao.insertMachine(MachineEntity(id, lineId, shopName, lineName, machineName, machineType, manufacturer, model, status))
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deleteMachine(id)
                        }
                    }
                }
                "patrol_points" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val machineId = obj.optInt("machine_id")
                                val machineName = obj.optString("machine_name")
                                val pointName = obj.optString("point_name")
                                val category = obj.optString("category")
                                val standardValue = obj.optString("standard_value")
                                val sequenceNo = obj.optInt("sequence_no", 1)
                                val frequency = obj.optString("frequency", "Every Shift")
                                val active = obj.optBoolean("active", true)
                                val description = obj.optString("description")
                                val revisionNumber = obj.optInt("revision_number", 1)
                                if (id > 0 && pointName.isNotBlank()) {
                                    dao.insertPatrolPoint(
                                        PatrolPointEntity(
                                            id = id,
                                            machineId = machineId,
                                            machineName = machineName,
                                            pointName = pointName,
                                            category = category,
                                            standardValue = standardValue,
                                            sequenceNo = sequenceNo,
                                            frequency = frequency,
                                            active = active,
                                            description = description,
                                            revisionNumber = revisionNumber
                                        )
                                    )
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deletePatrolPoint(id)
                        }
                    }
                }
                "patrol_point_revisions" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val pointId = obj.optInt("point_id")
                                val revisionNumber = obj.optInt("revision_number")
                                val revisionDate = obj.optLong("revision_date", System.currentTimeMillis())
                                val revisedBy = obj.optString("revised_by")
                                val reason = obj.optString("reason")
                                val oldValue = obj.optString("old_value")
                                val newValue = obj.optString("new_value")
                                dao.insertPointRevision(
                                    PatrolPointRevisionEntity(
                                        id = id,
                                        pointId = pointId,
                                        revisionNumber = revisionNumber,
                                        revisionDate = revisionDate,
                                        revisedBy = revisedBy,
                                        reason = reason,
                                        oldValue = oldValue,
                                        newValue = newValue
                                    )
                                )
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deletePointRevision(id)
                        }
                    }
                }
                "patrol_logs" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val patrolNumber = obj.optString("patrol_number")
                                val shopName = obj.optString("shop_name")
                                val lineName = obj.optString("line_name")
                                val machineName = obj.optString("machine_name")
                                val machineId = obj.optInt("machine_id")
                                val employeeId = obj.optString("employee_id")
                                val employeeName = obj.optString("employee_name")
                                val shift = obj.optString("shift")
                                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                val overallStatus = obj.optString("overall_status", "NORMAL")
                                val notes = obj.optString("notes")
                                if (id > 0) {
                                    dao.insertPatrolLog(
                                        PatrolLogEntity(
                                            id = id,
                                            patrolNumber = patrolNumber,
                                            shopName = shopName,
                                            lineName = lineName,
                                            machineName = machineName,
                                            machineId = machineId,
                                            employeeId = employeeId,
                                            employeeName = employeeName,
                                            shift = shift,
                                            timestamp = timestamp,
                                            overallStatus = overallStatus,
                                            notes = notes
                                        )
                                    )
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deletePatrolLog(id)
                        }
                    }
                }
                "patrol_point_results" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val patrolLogId = obj.optInt("patrol_log_id")
                                val patrolPointId = obj.optInt("patrol_point_id")
                                val checkpointName = obj.optString("checkpoint_name")
                                val category = obj.optString("category")
                                val standardValue = obj.optString("standard_value")
                                val status = obj.optString("status")
                                val remarks = obj.optString("remarks")
                                val problemDescription = obj.optString("problem_description")
                                val severity = obj.optString("severity")
                                val countermeasure = obj.optString("countermeasure")
                                val photoUri = obj.optString("photo_uri").takeIf { it.isNotBlank() && it != "null" }
                                if (id > 0) {
                                    dao.insertPatrolPointResults(
                                        listOf(
                                            PatrolPointResultEntity(
                                                id = id,
                                                patrolLogId = patrolLogId,
                                                patrolPointId = patrolPointId,
                                                checkpointName = checkpointName,
                                                category = category,
                                                standardValue = standardValue,
                                                status = status,
                                                remarks = remarks,
                                                problemDescription = problemDescription,
                                                severity = severity,
                                                countermeasure = countermeasure,
                                                photoUri = photoUri
                                            )
                                        )
                                    )
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deletePatrolPointResult(id)
                        }
                    }
                }
                "abnormalities" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val patrolLogId = obj.optInt("patrol_log_id")
                                val abnormalityNumber = obj.optString("abnormality_number")
                                val shopName = obj.optString("shop_name")
                                val lineName = obj.optString("line_name")
                                val machineName = obj.optString("machine_name")
                                val machineId = obj.optInt("machine_id")
                                val checkpointName = obj.optString("checkpoint_name")
                                val category = obj.optString("category")
                                val priority = obj.optString("priority")
                                val problemDescription = obj.optString("problem_description")
                                val rootCause = obj.optString("root_cause")
                                val correctiveAction = obj.optString("corrective_action")
                                val responsiblePerson = obj.optString("responsible_person")
                                val targetDate = obj.optString("target_date")
                                val completedDate = obj.optString("completed_date").takeIf { it.isNotBlank() && it != "null" }
                                val status = obj.optString("status")
                                val reportedBy = obj.optString("reported_by")
                                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                val photoUri = obj.optString("photo_uri").takeIf { it.isNotBlank() && it != "null" }
                                if (id > 0) {
                                    dao.insertAbnormality(
                                        AbnormalityEntity(
                                            id = id,
                                            patrolLogId = patrolLogId,
                                            abnormalityNumber = abnormalityNumber,
                                            shopName = shopName,
                                            lineName = lineName,
                                            machineName = machineName,
                                            machineId = machineId,
                                            checkpointName = checkpointName,
                                            category = category,
                                            priority = priority,
                                            problemDescription = problemDescription,
                                            rootCause = rootCause,
                                            correctiveAction = correctiveAction,
                                            responsiblePerson = responsiblePerson,
                                            targetDate = targetDate,
                                            completedDate = completedDate,
                                            status = status,
                                            reportedBy = reportedBy,
                                            timestamp = timestamp,
                                            photoUri = photoUri
                                        )
                                    )
                                }
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deleteAbnormality(id)
                        }
                    }
                }
                "audit_logs" -> {
                    when (action) {
                        "INSERT", "UPDATE" -> {
                            record?.let { obj ->
                                val id = obj.optInt("id")
                                val employeeId = obj.optString("employee_id")
                                val employeeName = obj.optString("employee_name")
                                val actionType = obj.optString("action")
                                val module = obj.optString("module")
                                val details = obj.optString("details")
                                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                dao.insertAuditLog(
                                    AuditLogEntity(
                                        id = id,
                                        employeeId = employeeId,
                                        employeeName = employeeName,
                                        action = actionType,
                                        module = module,
                                        details = details,
                                        timestamp = timestamp
                                    )
                                )
                            }
                        }
                        "DELETE" -> {
                            val id = oldRecord?.optInt("id", 0) ?: record?.optInt("id", 0) ?: 0
                            if (id > 0) dao.deleteAuditLog(id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("YamahaRepository", "Error handling realtime event for table $table", e)
        }
    }

    /**
     * Complete Bidirectional/Centralized Synchronization:
     * Pulls the authoritative single source of truth from Supabase into local Room cache.
     * Reconciles remote deletions and additions across all master and transactional entities.
     */
    suspend fun syncFromSupabase(): Result<String> = withContext(Dispatchers.IO) {
        if (!supabaseService.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase central database is not configured."))
        }
        try {
            var syncedCount = 0

            // 1. Sync Users
            val remoteUsers = supabaseService.fetchUsers()
            if (remoteUsers != null) {
                if (remoteUsers.isNotEmpty()) {
                    val remoteUserIds = remoteUsers.map { it.employeeId }.toSet()
                    val localUsers = dao.getAllUsersDirect()
                    localUsers.forEach { local ->
                        if (!remoteUserIds.contains(local.employeeId) && local.username != "admin") {
                            dao.deleteUser(local.employeeId)
                        }
                    }
                    remoteUsers.forEach { dao.insertUser(it) }
                    syncedCount += remoteUsers.size
                } else {
                    val localUsers = dao.getAllUsersDirect()
                    localUsers.forEach { supabaseService.upsertUser(it) }
                }
            }

            // 2. Sync Shops
            val remoteShops = supabaseService.fetchShops()
            if (remoteShops != null) {
                if (remoteShops.isNotEmpty()) {
                    val remoteShopIds = remoteShops.map { it.id }.toSet()
                    val localShops = dao.getAllShopsDirect()
                    localShops.forEach { local ->
                        if (!remoteShopIds.contains(local.id)) {
                            dao.deleteShop(local.id)
                        }
                    }
                    remoteShops.forEach { dao.insertShop(it) }
                    syncedCount += remoteShops.size
                } else {
                    val localShops = dao.getAllShopsDirect()
                    localShops.forEach { supabaseService.upsertShop(it) }
                }
            }

            // 3. Sync Lines
            val remoteLines = supabaseService.fetchLines()
            if (remoteLines != null) {
                if (remoteLines.isNotEmpty()) {
                    val remoteLineIds = remoteLines.map { it.id }.toSet()
                    val localLines = dao.getAllLinesDirect()
                    localLines.forEach { local ->
                        if (!remoteLineIds.contains(local.id)) {
                            dao.deleteLine(local.id)
                        }
                    }
                    remoteLines.forEach { dao.insertLine(it) }
                    syncedCount += remoteLines.size
                } else {
                    val localLines = dao.getAllLinesDirect()
                    localLines.forEach { supabaseService.upsertLine(it) }
                }
            }

            // 4. Sync Machines
            val remoteMachines = supabaseService.fetchMachines()
            if (remoteMachines != null) {
                if (remoteMachines.isNotEmpty()) {
                    val remoteMachineIds = remoteMachines.map { it.id }.toSet()
                    val localMachines = dao.getAllMachinesDirect()
                    localMachines.forEach { local ->
                        if (!remoteMachineIds.contains(local.id)) {
                            dao.deleteMachine(local.id)
                        }
                    }
                    remoteMachines.forEach { dao.insertMachine(it) }
                    syncedCount += remoteMachines.size
                } else {
                    val localMachines = dao.getAllMachinesDirect()
                    localMachines.forEach { supabaseService.upsertMachine(it) }
                }
            }

            // 5. Sync Patrol Points
            val remotePoints = supabaseService.fetchPatrolPoints()
            if (remotePoints != null) {
                if (remotePoints.isNotEmpty()) {
                    val remotePointIds = remotePoints.map { it.id }.toSet()
                    val localPoints = dao.getAllPatrolPointsDirect()
                    localPoints.forEach { local ->
                        if (!remotePointIds.contains(local.id)) {
                            dao.deletePatrolPoint(local.id)
                        }
                    }
                    remotePoints.forEach { dao.insertPatrolPoint(it) }
                    syncedCount += remotePoints.size
                } else {
                    val localPoints = dao.getAllPatrolPointsDirect()
                    localPoints.forEach { supabaseService.upsertPatrolPoint(it) }
                }
            }

            // 6. Sync Patrol Point Revisions
            val remoteRevisions = supabaseService.fetchRevisions()
            if (remoteRevisions != null && remoteRevisions.isNotEmpty()) {
                val remoteRevIds = remoteRevisions.map { it.id }.toSet()
                val localRevs = dao.getAllRevisionsDirect()
                localRevs.forEach { local ->
                    if (!remoteRevIds.contains(local.id)) {
                        dao.deletePointRevision(local.id)
                    }
                }
                remoteRevisions.forEach { dao.insertPointRevision(it) }
                syncedCount += remoteRevisions.size
            }

            // 7. Sync Patrol Logs
            val remoteLogs = supabaseService.fetchPatrolLogs()
            if (remoteLogs != null) {
                val remoteLogIds = remoteLogs.map { it.id }.toSet()
                val localLogs = dao.getAllPatrolLogsDirect()
                localLogs.forEach { local ->
                    if (!remoteLogIds.contains(local.id)) {
                        dao.deletePatrolLog(local.id)
                    }
                }
                remoteLogs.forEach { dao.insertPatrolLog(it) }
                syncedCount += remoteLogs.size
            }

            // 8. Sync Patrol Point Results
            val remoteResults = supabaseService.fetchPatrolPointResults()
            if (remoteResults != null) {
                val remoteResultIds = remoteResults.map { it.id }.toSet()
                val localResults = dao.getAllResultsDirect()
                localResults.forEach { local ->
                    if (!remoteResultIds.contains(local.id)) {
                        dao.deletePatrolPointResult(local.id)
                    }
                }
                if (remoteResults.isNotEmpty()) {
                    dao.insertPatrolPointResults(remoteResults)
                }
                syncedCount += remoteResults.size
            }

            // 9. Sync Abnormalities
            val remoteAbnormalities = supabaseService.fetchAbnormalities()
            if (remoteAbnormalities != null) {
                val remoteAbIds = remoteAbnormalities.map { it.id }.toSet()
                val localAbs = dao.getAllAbnormalitiesDirect()
                localAbs.forEach { local ->
                    if (!remoteAbIds.contains(local.id)) {
                        dao.deleteAbnormality(local.id)
                    }
                }
                remoteAbnormalities.forEach { dao.insertAbnormality(it) }
                syncedCount += remoteAbnormalities.size
            }

            // 10. Sync Audit Logs
            val remoteAuditLogs = supabaseService.fetchAuditLogs()
            if (remoteAuditLogs != null && remoteAuditLogs.isNotEmpty()) {
                remoteAuditLogs.forEach { dao.insertAuditLog(it) }
                syncedCount += remoteAuditLogs.size
            }

            Result.success("Central database synchronized ($syncedCount records updated)")
        } catch (e: Exception) {
            Log.e("YamahaRepository", "Sync failed", e)
            Result.failure(e)
        }
    }

    // --- Authentication ---
    suspend fun authenticateUser(username: String, passwordAttempt: String): UserEntity? = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().lowercase().removeSuffix("@yamaha-motor-india.com")

        // Central-First: Sync user records from Supabase to ensure fresh credentials & role state
        if (supabaseService.isConfigured()) {
            try {
                val remoteUsers = supabaseService.fetchUsers()
                if (!remoteUsers.isNullOrEmpty()) {
                    remoteUsers.forEach { dao.insertUser(it) }
                }
            } catch (e: Exception) {
                Log.w("YamahaRepository", "Pre-auth user sync failed, using cached credentials", e)
            }
        }

        var user = dao.getUserByUsername(cleanUsername)

        // Ensure default Super Admin exists if central DB is new and empty
        if (user == null && cleanUsername == "admin") {
            val defaultAdmin = UserEntity(
                employeeId = "YMH-ADM-001",
                employeeName = "System Super Admin",
                username = "admin",
                email = "admin@yamaha-motor-india.com",
                department = "Maintenance Engineering",
                plant = "Sriperumbudur Plant 1",
                role = "ADMIN",
                passwordHash = "Admin@123",
                status = "Active",
                createdBy = "System Installer"
            )
            dao.insertUser(defaultAdmin)
            supabaseService.upsertUser(defaultAdmin)
            user = defaultAdmin
        }

        if (user != null) {
            val trimmedPass = passwordAttempt.trim()
            if (user.passwordHash == trimmedPass) {
                return@withContext user
            }
        }
        return@withContext null
    }

    suspend fun clearTransactionalData(): Result<Unit> = withContext(Dispatchers.IO) {
        dao.deleteAllPatrolPointResults()
        dao.deleteAllPatrolLogs()
        dao.deleteAllAbnormalities()
        dao.deleteAllAuditLogs()
        if (supabaseService.isConfigured()) {
            supabaseService.deleteAllPatrolPointResults()
            supabaseService.deleteAllPatrolLogs()
            supabaseService.deleteAllAbnormalities()
            supabaseService.deleteAllAuditLogs()
        }
        Result.success(Unit)
    }

    // --- User CRUD ---
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()

    suspend fun insertUser(user: UserEntity): Result<UserEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertUser(user)
        if (remote != null) {
            dao.insertUser(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Could not synchronize user to central Supabase cloud database."))
            } else {
                dao.insertUser(user)
                Result.success(user)
            }
        }
    }

    suspend fun updateUser(user: UserEntity): Result<UserEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertUser(user)
        if (remote != null) {
            dao.updateUser(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Could not synchronize user update to central Supabase cloud database."))
            } else {
                dao.updateUser(user)
                Result.success(user)
            }
        }
    }

    suspend fun deleteUser(employeeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val remoteSuccess = supabaseService.deleteUser(employeeId)
        if (remoteSuccess || !supabaseService.isConfigured()) {
            dao.deleteUser(employeeId)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Could not delete user from central Supabase cloud database."))
        }
    }

    // --- Shops ---
    val allShops: Flow<List<ShopEntity>> = dao.getAllShops()

    suspend fun insertShop(shop: ShopEntity): Result<ShopEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertShop(shop)
        if (remote != null) {
            dao.insertShop(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to persist Shop to central Supabase cloud database."))
            } else {
                val localId = dao.insertShop(shop).toInt()
                val saved = shop.copy(id = if (shop.id > 0) shop.id else localId)
                Result.success(saved)
            }
        }
    }

    suspend fun updateShop(shop: ShopEntity): Result<ShopEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertShop(shop)
        if (remote != null) {
            dao.updateShop(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to update Shop in central Supabase cloud database."))
            } else {
                dao.updateShop(shop)
                Result.success(shop)
            }
        }
    }

    suspend fun deleteShop(shopId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val remoteSuccess = supabaseService.deleteShop(shopId)
        if (remoteSuccess || !supabaseService.isConfigured()) {
            dao.deleteShop(shopId)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete Shop from central Supabase cloud database."))
        }
    }

    // --- Lines ---
    val allLines: Flow<List<LineEntity>> = dao.getAllLines()
    fun getLinesForShop(shopId: Int): Flow<List<LineEntity>> = dao.getLinesForShop(shopId)

    suspend fun insertLine(line: LineEntity): Result<LineEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertLine(line)
        if (remote != null) {
            dao.insertLine(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to persist Line to central Supabase cloud database."))
            } else {
                val localId = dao.insertLine(line).toInt()
                val saved = line.copy(id = if (line.id > 0) line.id else localId)
                Result.success(saved)
            }
        }
    }

    suspend fun updateLine(line: LineEntity): Result<LineEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertLine(line)
        if (remote != null) {
            dao.updateLine(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to update Line in central Supabase cloud database."))
            } else {
                dao.updateLine(line)
                Result.success(line)
            }
        }
    }

    suspend fun deleteLine(lineId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val remoteSuccess = supabaseService.deleteLine(lineId)
        if (remoteSuccess || !supabaseService.isConfigured()) {
            dao.deleteLine(lineId)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete Line from central Supabase cloud database."))
        }
    }

    // --- Machines ---
    val allMachines: Flow<List<MachineEntity>> = dao.getAllMachines()
    fun getMachinesForLine(lineId: Int): Flow<List<MachineEntity>> = dao.getMachinesForLine(lineId)

    suspend fun insertMachine(machine: MachineEntity): Result<MachineEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertMachine(machine)
        if (remote != null) {
            dao.insertMachine(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to persist Machine to central Supabase cloud database."))
            } else {
                val localId = dao.insertMachine(machine).toInt()
                val saved = machine.copy(id = if (machine.id > 0) machine.id else localId)
                Result.success(saved)
            }
        }
    }

    suspend fun updateMachine(machine: MachineEntity): Result<MachineEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertMachine(machine)
        if (remote != null) {
            dao.updateMachine(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to update Machine in central Supabase cloud database."))
            } else {
                dao.updateMachine(machine)
                Result.success(machine)
            }
        }
    }

    suspend fun deleteMachine(machineId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val remoteSuccess = supabaseService.deleteMachine(machineId)
        if (remoteSuccess || !supabaseService.isConfigured()) {
            dao.deleteMachine(machineId)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete Machine from central Supabase cloud database."))
        }
    }

    // --- Patrol Points ---
    val allPatrolPoints: Flow<List<PatrolPointEntity>> = dao.getAllPatrolPoints()
    fun getPointsForMachine(machineId: Int): Flow<List<PatrolPointEntity>> = dao.getPointsForMachine(machineId)

    suspend fun insertPatrolPoint(point: PatrolPointEntity): Result<PatrolPointEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertPatrolPoint(point)
        if (remote != null) {
            dao.insertPatrolPoint(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to persist Patrol Point to central Supabase cloud database."))
            } else {
                val localId = dao.insertPatrolPoint(point).toInt()
                val saved = point.copy(id = if (point.id > 0) point.id else localId)
                Result.success(saved)
            }
        }
    }

    suspend fun updatePatrolPoint(point: PatrolPointEntity): Result<PatrolPointEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertPatrolPoint(point)
        if (remote != null) {
            dao.updatePatrolPoint(remote)
            Result.success(remote)
        } else {
            if (supabaseService.isConfigured()) {
                Result.failure(Exception("Failed to update Patrol Point in central Supabase cloud database."))
            } else {
                dao.updatePatrolPoint(point)
                Result.success(point)
            }
        }
    }

    suspend fun deletePatrolPoint(pointId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val remoteSuccess = supabaseService.deletePatrolPoint(pointId)
        if (remoteSuccess || !supabaseService.isConfigured()) {
            dao.deletePatrolPoint(pointId)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to delete Patrol Point from central Supabase cloud database."))
        }
    }

    val allRevisions: Flow<List<PatrolPointRevisionEntity>> = dao.getAllRevisions()

    suspend fun revisePatrolPoint(
        point: PatrolPointEntity,
        newStandardValue: String,
        newCategory: String,
        newFrequency: String,
        reason: String,
        revisedBy: String
    ): Result<PatrolPointEntity> = withContext(Dispatchers.IO) {
        val oldSummary = "Category: ${point.category}, Standard: ${point.standardValue}, Freq: ${point.frequency}"
        val newSummary = "Category: $newCategory, Standard: $newStandardValue, Freq: $newFrequency"
        val nextRev = point.revisionNumber + 1

        val updatedPoint = point.copy(
            standardValue = newStandardValue,
            category = newCategory,
            frequency = newFrequency,
            revisionNumber = nextRev
        )

        val remotePoint = supabaseService.upsertPatrolPoint(updatedPoint) ?: updatedPoint
        dao.updatePatrolPoint(remotePoint)

        val revisionLog = PatrolPointRevisionEntity(
            pointId = point.id,
            revisionNumber = nextRev,
            revisionDate = System.currentTimeMillis(),
            revisedBy = revisedBy,
            reason = reason,
            oldValue = oldSummary,
            newValue = newSummary
        )
        val remoteRevision = supabaseService.upsertRevision(revisionLog) ?: revisionLog
        dao.insertPointRevision(remoteRevision)

        Result.success(remotePoint)
    }

    // --- Photo Evidence Upload ---
    suspend fun uploadEvidencePhoto(context: Context, localUri: Uri): String? {
        return supabaseService.uploadEvidencePhoto(context, localUri)
    }

    // --- Patrol Execution ---
    val allPatrolLogs: Flow<List<PatrolLogEntity>> = dao.getAllPatrolLogs()

    suspend fun submitPatrolLog(
        log: PatrolLogEntity,
        results: List<PatrolPointResultEntity>,
        abnormality: AbnormalityEntity?
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Central-First: Push Patrol Log
            val remoteLog = supabaseService.upsertPatrolLog(log)
            val logId = if (remoteLog != null && remoteLog.id > 0) {
                dao.insertPatrolLog(remoteLog)
                remoteLog.id
            } else {
                val localId = dao.insertPatrolLog(log).toInt()
                localId
            }

            // Push Results
            val mappedResults = results.map { it.copy(patrolLogId = logId) }
            val remoteResults = supabaseService.upsertPatrolPointResults(mappedResults)
            if (remoteResults != null && remoteResults.isNotEmpty()) {
                dao.insertPatrolPointResults(remoteResults)
            } else {
                dao.insertPatrolPointResults(mappedResults)
            }

            // Push Abnormality if present
            if (abnormality != null) {
                val createdAbnormality = abnormality.copy(patrolLogId = logId)
                val remoteAb = supabaseService.upsertAbnormality(createdAbnormality)
                if (remoteAb != null) {
                    dao.insertAbnormality(remoteAb)
                } else {
                    dao.insertAbnormality(createdAbnormality)
                }
            }

            // Push Audit
            val audit = AuditLogEntity(
                employeeId = log.employeeId,
                employeeName = log.employeeName,
                action = "PATROL_SUBMITTED",
                module = "Patrol Entry",
                details = "Submitted maintenance patrol #${log.patrolNumber} for ${log.machineName} (${log.overallStatus})"
            )
            val remoteAudit = supabaseService.insertAuditLog(audit)
            dao.insertAuditLog(remoteAudit ?: audit)

            Result.success(logId.toLong())
        } catch (e: Exception) {
            Log.e("YamahaRepository", "submitPatrolLog error", e)
            Result.failure(e)
        }
    }

    suspend fun getResultsForLog(logId: Int): List<PatrolPointResultEntity> {
        return dao.getResultsForLog(logId)
    }

    suspend fun getAllResultsDirect(): List<PatrolPointResultEntity> {
        return dao.getAllResultsDirect()
    }

    // --- Abnormality Management ---
    val allAbnormalities: Flow<List<AbnormalityEntity>> = dao.getAllAbnormalities()
    fun getAbnormalitiesByStatus(status: String) = dao.getAbnormalitiesByStatus(status)

    suspend fun updateAbnormality(abnormality: AbnormalityEntity, updatedBy: UserEntity): Result<AbnormalityEntity> = withContext(Dispatchers.IO) {
        val remote = supabaseService.upsertAbnormality(abnormality)
        val finalAb = remote ?: abnormality
        if (finalAb.id > 0) {
            dao.updateAbnormality(finalAb)
        } else {
            val localId = dao.insertAbnormality(finalAb).toInt()
            finalAb.copy(id = localId)
        }

        val audit = AuditLogEntity(
            employeeId = updatedBy.employeeId,
            employeeName = updatedBy.employeeName,
            action = if (abnormality.id > 0) "ABNORMALITY_UPDATED" else "ABNORMALITY_CREATED",
            module = "Abnormality Management",
            details = "Updated abnormality #${finalAb.id} status to ${finalAb.status}"
        )
        val remoteAudit = supabaseService.insertAuditLog(audit)
        dao.insertAuditLog(remoteAudit ?: audit)

        Result.success(finalAb)
    }

    // --- Audit Logs ---
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    suspend fun logAudit(user: UserEntity, action: String, module: String, details: String) = withContext(Dispatchers.IO) {
        val audit = AuditLogEntity(
            employeeId = user.employeeId,
            employeeName = user.employeeName,
            action = action,
            module = module,
            details = details
        )
        val remote = supabaseService.insertAuditLog(audit)
        dao.insertAuditLog(remote ?: audit)
    }

    // --- Dashboard Counts ---
    val totalPatrolsCount: Flow<Int> = dao.getTotalPatrolsCount()
    val pendingAbnormalitiesCount: Flow<Int> = dao.getPendingAbnormalitiesCount()
    val criticalIssuesCount: Flow<Int> = dao.getCriticalIssuesCount()
    val operationalMachinesCount: Flow<Int> = dao.getOperationalMachinesCount()
    val totalMachinesCount: Flow<Int> = dao.getTotalMachinesCount()
}
